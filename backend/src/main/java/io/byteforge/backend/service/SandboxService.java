package io.byteforge.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.byteforge.backend.model.dto.ExecutionDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class SandboxService {

    @Value("${sandbox.socket.host:localhost}")
    private String socketHost;

    @Value("${sandbox.socket.port:8884}")
    private int socketPort;

    @Value("${sandbox.timeout.seconds:30}")
    private int timeoutSeconds;

    @Value("${sandbox.memory.limit.mb:256}")
    private int memoryLimitMb;

    private final ObjectMapper objectMapper;
    private final Map<String, Socket> activeSockets = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> socketWriters = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public SandboxService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        log.info("🚀 SandboxService initialized - Socket: {}:{}", socketHost, socketPort);
    }

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        activeSockets.values().forEach(this::closeSocket);
        socketWriters.values().forEach(writer -> {
            try {
                writer.close();
            } catch (Exception e) {
                // ignore
            }
        });
        activeSockets.clear();
        socketWriters.clear();
    }

    public void registerSession(String sessionId, WebSocketSession session) {
        webSocketSessions.put(sessionId, session);
        log.info("✅ WebSocket session registered: {}", sessionId);
    }

    public void unregisterSession(String sessionId) {
        webSocketSessions.remove(sessionId);
        closeSocketConnection(sessionId);
        log.info("🔌 WebSocket session unregistered: {}", sessionId);
    }

    public void executeCodeInteractive(String sessionId, String code, String connectionId) {
        executeWithSocket(sessionId, connectionId, () -> sendCodeToSandbox(sessionId, code));
    }

    public void executeMultiFileInteractive(String sessionId, Map<String, String> files, String connectionId) {
        executeWithSocket(sessionId, connectionId, () -> sendMultiFileToSandbox(sessionId, files));
    }

    private void executeWithSocket(String sessionId, String connectionId, SocketOperation operation) {
        log.info("=== 🚀 STARTING SOCKET EXECUTION ===");
        log.info("📋 Session: {}, Connection: {}", sessionId, connectionId);

        executorService.submit(() -> {
            Socket socket = null;
            try {
                socket = getSocket(sessionId);
                operation.execute();
                waitForCompletion(sessionId, socket);

            } catch (java.net.ConnectException e) {
                handleConnectionError(sessionId, "CONNECTION FAILED: Cannot connect to C++ server at {}:{}", e);
            } catch (SocketTimeoutException e) {
                handleConnectionError(sessionId, "CONNECTION TIMEOUT: Connection to {}:{} timed out", e);
            } catch (Exception e) {
                log.error("❌ EXECUTION FAILED for session {}: {}", sessionId, e.getMessage(), e);
                sendWebSocketMessage(sessionId, createMessage("ERROR", "Execution failed: " + e.getMessage(), null));
            } finally {
                closeSocketConnection(sessionId);
                log.info("=== 🏁 EXECUTION FINISHED ===");
            }
        });
    }

    private void handleConnectionError(String sessionId, String logMessage, Exception e) {
        log.error("❌ " + logMessage, socketHost, socketPort);
        log.error("💡 Make sure cpp_sandbox.exe is running with: cpp_sandbox.exe --socket");
        sendWebSocketMessage(sessionId, createMessage("ERROR",
                "C++ execution server is not running. Please start the server first.", null));
    }

    private Socket getSocket(String sessionId) throws IOException, InterruptedException {
        Socket socket;
        log.info("🔌 Attempting to connect to C++ server at {}:{}", socketHost, socketPort);

        socket = new Socket();
        socket.connect(new InetSocketAddress(socketHost, socketPort), 5000);
        socket.setSoTimeout(30000);

        activeSockets.put(sessionId, socket);
        log.info("✅ SUCCESS: Connected to C++ sandbox server");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        socketWriters.put(sessionId, writer);

        sendWebSocketMessage(sessionId, createMessage("EXECUTION_STARTED", "Connected to execution engine", null));

        startOutputReader(sessionId, socket);

        Thread.sleep(50);
        return socket;
    }

    private void startOutputReader(String sessionId, Socket socket) {
        executorService.submit(() -> {
            log.info("📖 Starting OUTPUT READER for session: {}", sessionId);

            try (InputStream input = socket.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {

                char[] buffer = new char[8192];
                int totalBytesRead = 0;
                StringBuilder outputBuffer = new StringBuilder();

                socket.setSoTimeout(0);

                while (!socket.isClosed()) {
                    int bytesRead = reader.read(buffer);
                    if (bytesRead == -1) {
                        log.info("📖 End of stream reached for session: {}", sessionId);
                        break;
                    }

                    totalBytesRead += bytesRead;
                    String chunk = new String(buffer, 0, bytesRead);
                    outputBuffer.append(chunk);

                    log.info("📥 RAW CHUNK from C++ ({} bytes): [{}]", bytesRead,
                            escapeNonPrintable(chunk.length() > 200 ? chunk.substring(0, 200) + "..." : chunk));

                    processOutputChunk(sessionId, outputBuffer);
                }

                if (outputBuffer.length() > 0) {
                    String remaining = outputBuffer.toString();
                    log.info("📤 Processing remaining output ({} chars): {}", remaining.length(), remaining);
                    handleOutputLine(sessionId, remaining);
                }

                log.info("📖 OUTPUT READER COMPLETED for session: {} (total bytes: {})", sessionId, totalBytesRead);

            } catch (IOException e) {
                if (!e.getMessage().toLowerCase().contains("socket closed") &&
                        !e.getMessage().toLowerCase().contains("connection reset")) {
                    log.error("❌ ERROR in output reader for session {}: {}", sessionId, e.getMessage());
                } else {
                    log.info("🔌 Socket closed normally for session: {}", sessionId);
                }
            } catch (Exception e) {
                log.error("❌ UNEXPECTED ERROR in output reader for session {}: {}", sessionId, e.getMessage(), e);
            }
        });
    }

    private String escapeNonPrintable(String text) {
        return text.replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\0", "\\0");
    }

    private void processOutputChunk(String sessionId, StringBuilder outputBuffer) {
        String content = outputBuffer.toString();

        log.info("🔍 PROCESSING CHUNK: [{}]", escapeNonPrintable(content));

        if (content.contains("{\"type\":\"") && content.contains("\"message\":")) {
            int jsonStart = content.indexOf("{\"type\":");
            if (jsonStart != -1) {
                int jsonEnd = findJsonEnd(content, jsonStart);
                if (jsonEnd != -1) {
                    String jsonLine = content.substring(jsonStart, jsonEnd + 1);
                    log.info("🎯 FOUND JSON: [{}]", jsonLine);

                    handleOutputLine(sessionId, jsonLine);

                    outputBuffer.delete(jsonStart, jsonEnd + 1);
                    log.info("🔍 BUFFER AFTER JSON REMOVAL: [{}]", outputBuffer.toString());
                    return;
                } else {
                    log.info("⏳ JSON not complete yet, waiting for more data...");
                }
            }
        }

        int lastNewline = content.lastIndexOf('\n');
        if (lastNewline != -1) {
            String completeLines = content.substring(0, lastNewline + 1);
            String remaining = content.substring(lastNewline + 1);

            log.info("🔍 SPLITTING BY NEWLINE - complete: [{}], remaining: [{}]",
                    escapeNonPrintable(completeLines), escapeNonPrintable(remaining));

            String[] lines = completeLines.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (i < lines.length - 1 || line.isEmpty()) {
                    line += "\n";
                }
                if (!line.trim().isEmpty() || line.contains("\n")) {
                    log.info("🔍 PROCESSING LINE: [{}]", escapeNonPrintable(line));
                    handleOutputLine(sessionId, line);
                }
            }

            outputBuffer.setLength(0);
            outputBuffer.append(remaining);
            log.info("🔍 BUFFER AFTER NEWLINE PROCESSING: [{}]", outputBuffer.toString());
        } else {
            log.info("⏳ No newline found, keeping in buffer: [{}]", escapeNonPrintable(content));
        }
    }

    private int findJsonEnd(String content, int start) {
        int braceCount = 0;
        boolean inString = false;

        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;

                if (braceCount == 0) {
                    return i;
                }
            }
        }

        return -1;
    }

    private void handleOutputLine(String sessionId, String line) {
        log.info("🎯 HANDLING OUTPUT LINE: [{}]", escapeNonPrintable(line));

        if (line.startsWith("{\"type\":\"INPUT_REQUIRED\"")) {
            handleJsonMessage(sessionId, line, "INPUT_REQUIRED", "❓ PROGRAM REQUESTING INPUT: {}");
        } else if (line.startsWith("{\"type\":\"EXECUTION_RESULT\"")) {
            handleExecutionResult(sessionId, line);
        } else if (line.startsWith("{\"type\":\"OUTPUT\"")) {
            handleJsonMessage(sessionId, line, "OUTPUT", "📤 PROGRAM OUTPUT: {}");
        } else if (line.startsWith("{\"type\":\"ERROR\"")) {
            handleJsonMessage(sessionId, line, "ERROR", "❌ ERROR: {}");
        } else if (line.startsWith("{\"type\":\"COMPILE_SUCCESS\"")) {
            handleJsonMessage(sessionId, line, "COMPILE_SUCCESS", "✅ COMPILATION SUCCESSFUL: {}");
        } else {
            handlePlainTextOutput(sessionId, line);
        }
    }

    private void handleJsonMessage(String sessionId, String line, String type, String logMessage) {
        log.info("✅ DETECTED {} JSON", type);
        try {
            Map<String, Object> data = objectMapper.readValue(line, Map.class);
            String message = (String) data.get("message");
            log.info(logMessage, message);

            if ("EXECUTION_RESULT".equals(type)) {
                sendWebSocketMessage(sessionId, data);
                closeSocketConnection(sessionId);
            } else {
                sendWebSocketMessage(sessionId, createMessage(type, message, getExitCode(data)));
            }
        } catch (Exception e) {
            log.error("❌ Failed to parse {} JSON: {}", type, e.getMessage());
            log.error("📋 Raw JSON that failed: {}", line);
        }
    }

    private void handleExecutionResult(String sessionId, String line) {
        log.info("✅ DETECTED EXECUTION_RESULT JSON");
        try {
            Map<String, Object> result = objectMapper.readValue(line, Map.class);
            log.info("🏁 EXECUTION RESULT: {}", result.get("status"));
            sendWebSocketMessage(sessionId, result);
            closeSocketConnection(sessionId);
        } catch (Exception e) {
            log.error("❌ Failed to parse EXECUTION_RESULT JSON: {}", e.getMessage());
            log.error("📋 Raw JSON that failed: {}", line);
        }
    }

    private void handlePlainTextOutput(String sessionId, String line) {
        String cleanLine = line.trim();
        if (cleanLine.startsWith("COMPILE_ERROR:")) {
            String error = cleanLine.substring("COMPILE_ERROR:".length()).trim();
            log.error("❌ COMPILATION ERROR: {}", error);
            sendWebSocketMessage(sessionId, createMessage("COMPILE_ERROR", error, null));
        } else if (cleanLine.equals("COMPILE_SUCCESS")) {
            log.info("✅ COMPILATION SUCCESSFUL");
            sendWebSocketMessage(sessionId, createMessage("COMPILE_SUCCESS", "Code compiled successfully", null));
        } else if (!cleanLine.isEmpty()) {
            log.info("📤 PROGRAM OUTPUT: {}", cleanLine);
            sendWebSocketMessage(sessionId, createMessage("OUTPUT", cleanLine, null));
        }
    }

    private Integer getExitCode(Map<String, Object> data) {
        Object exitCode = data.get("exit_code");
        return exitCode != null ? (Integer) exitCode : null;
    }

    private void sendCodeToSandbox(String sessionId, String code) {
        sendToSandbox(sessionId, "CODE", () -> {
            String cleanedCode = cleanCode(code);
            log.info("📨 PREPARING TO SEND CODE:");
            log.info("   Original length: {} chars", code.length());
            log.info("   Cleaned length: {} chars", cleanedCode.length());
            log.info("   First 100 chars: {}", cleanedCode.substring(0, Math.min(100, cleanedCode.length())));

            ExecutionDto.CodeExecutionRequest request = ExecutionDto.CodeExecutionRequest.toDto(cleanedCode, timeoutSeconds, memoryLimitMb);
            return createGson().toJson(request);
        });
    }

    private void sendMultiFileToSandbox(String sessionId, Map<String, String> files) {
        sendToSandbox(sessionId, "MULTI-FILE PROJECT", () -> {
            Map<String, String> cleanedFiles = new HashMap<>();
            for (Map.Entry<String, String> entry : files.entrySet()) {
                cleanedFiles.put(entry.getKey(), cleanCode(entry.getValue()));
            }

            log.info("📨 PREPARING TO SEND MULTI-FILE PROJECT:");
            log.info("   Files count: {}", cleanedFiles.size());
            log.info("   Files: {}", cleanedFiles.keySet());

            Map<String, Object> request = new HashMap<>();
            request.put("files", cleanedFiles);
            request.put("timeLimitSec", timeoutSeconds);
            request.put("memoryLimitMb", memoryLimitMb);

            return createGson().toJson(request);
        });
    }

    private void sendToSandbox(String sessionId, String type, JsonSupplier jsonSupplier) {
        PrintWriter writer = socketWriters.get(sessionId);
        if (writer == null) {
            log.error("❌ No writer found for session: {}", sessionId);
            sendWebSocketMessage(sessionId, createMessage("ERROR", "No connection to execution server", null));
            return;
        }

        try {
            String jsonRequest = jsonSupplier.get();
            writer.print(jsonRequest);
            writer.flush();
            log.info("✅ {} SENT SUCCESSFULLY", type);

        } catch (Exception e) {
            log.error("❌ FAILED TO SEND {}: {}", type, e.getMessage(), e);
            sendWebSocketMessage(sessionId, createMessage("ERROR", "Failed to send " + type.toLowerCase() + " to execution server", null));
            throw new RuntimeException("Failed to send " + type.toLowerCase() + " to sandbox", e);
        }
    }

    public void sendInputToExecution(String sessionId, String input, String connectionId) {
        log.info("⌨️ SENDING INPUT to session {}: '{}'", sessionId, input);

        PrintWriter writer = socketWriters.get(sessionId);
        if (writer != null) {
            try {
                writer.print(input);
                writer.flush();
                log.info("✅ INPUT SENT SUCCESSFULLY: '{}'", input);
                sendWebSocketMessage(sessionId, createMessage("INPUT_SENT", "Input sent: " + input, null));
            } catch (Exception e) {
                log.error("❌ FAILED TO SEND INPUT: {}", e.getMessage());
                sendWebSocketMessage(sessionId, createMessage("ERROR", "Failed to send input: " + e.getMessage(), null));
            }
        } else {
            log.warn("⚠️ NO ACTIVE WRITER for session: {}", sessionId);
            sendWebSocketMessage(sessionId, createMessage("ERROR", "No active execution session", null));
        }
    }

    public void stopExecution(String sessionId, String connectionId) {
        log.info("🛑 STOPPING EXECUTION for session: {}", sessionId);
        closeSocketConnection(sessionId);
        sendWebSocketMessage(sessionId, createMessage("EXECUTION_STOPPED", "Execution stopped by user", null));
    }

    private void waitForCompletion(String sessionId, Socket socket) {
        try {
            log.info("⏳ WAITING FOR COMPLETION");

            long startTime = System.currentTimeMillis();
            long maxWaitTime = Math.max(timeoutSeconds * 1000L * 2, 10 * 60 * 1000L);

            while (!socket.isClosed()) {
                Thread.sleep(1000);

                if (socket.isClosed()) {
                    log.info("🔌 Socket closed by sandbox");
                    break;
                }

                if ((System.currentTimeMillis() - startTime) > maxWaitTime) {
                    log.warn("⏰ VERY LONG EXECUTION - forcing timeout");
                    sendWebSocketMessage(sessionId, createMessage("ERROR",
                            "Execution exceeded maximum wait time", null));
                    break;
                }
            }

            log.info("✅ Execution completed");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("🚫 Execution waiting interrupted for session: {}", sessionId);
        }
    }

    private void closeSocketConnection(String sessionId) {
        log.info("🔌 CLOSING SOCKET CONNECTION for session: {}", sessionId);

        PrintWriter writer = socketWriters.remove(sessionId);
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception e) {
                // ignore
            }
        }

        Socket socket = activeSockets.remove(sessionId);
        if (socket != null) {
            closeSocket(socket);
        }
    }

    private void closeSocket(Socket socket) {
        try {
            if (!socket.isClosed()) {
                socket.close();
                log.info("🔌 Socket closed successfully");
            }
        } catch (IOException e) {
            log.warn("⚠️ Error closing socket: {}", e.getMessage());
        }
    }

    private void sendWebSocketMessage(String sessionId, Map<String, Object> message) {
        try {
            WebSocketSession session = webSocketSessions.get(sessionId);
            if (session != null && session.isOpen()) {
                String jsonMessage = objectMapper.writeValueAsString(message);
                synchronized (session) {
                    session.sendMessage(new TextMessage(jsonMessage));
                }
                log.debug("📤 WEB SOCKET SENT to {}: {} - {}", sessionId, message.get("type"), message.get("message"));
            } else {
                log.warn("⚠️ WebSocket session not available for: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("❌ FAILED TO SEND WEB SOCKET MESSAGE to {}: {}", sessionId, e.getMessage());
        }
    }

    private Map<String, Object> createMessage(String type, String message, Integer exitCode) {
        return Map.of(
                "type", type,
                "message", message != null ? message : "",
                "exitCode", exitCode != null ? exitCode : 0,
                "timestamp", System.currentTimeMillis(),
                "sessionId", "sandbox"
        );
    }

    private String cleanCode(String code) {
        if (code == null) return "// Empty code";

        return code.replace("\uFEFF", "")
                .replace("\uFFFD", "")
                .replace("===END_CODE===", "")
                .trim();
    }

    private Gson createGson() {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .create();
    }

    @FunctionalInterface
    private interface SocketOperation {
        void execute() throws Exception;
    }

    @FunctionalInterface
    private interface JsonSupplier {
        String get() throws Exception;
    }
}