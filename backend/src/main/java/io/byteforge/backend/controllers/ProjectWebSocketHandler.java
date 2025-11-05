package io.byteforge.backend.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.byteforge.backend.model.dto.FileDto;
import io.byteforge.backend.service.FileService;
import io.byteforge.backend.service.ProjectService;
import io.byteforge.backend.service.SandboxService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> projectSessions = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArraySet<String>> projectToSessions = new ConcurrentHashMap<>();
    private final Map<String, UserInfo> sessionToUserInfo = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArraySet<UserInfo>> projectToUsers = new ConcurrentHashMap<>();
    private final Object sendLock = new Object();

    private final SandboxService sandboxService;
    private final ProjectService projectService;
    private final FileService fileService;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    private static class UserInfo {
        private String sessionId;
        private Long userId;
        private String username;
        private String email;
        private Long projectId;
        private Long connectedAt;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String projectId = extractProjectId(session);

        projectSessions.put(sessionId, session);
        projectToSessions
                .computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>())
                .add(sessionId);

        UserInfo tempUser = UserInfo.builder()
                .sessionId(sessionId)
                .userId(null)
                .username("Anonymous")
                .projectId(Long.valueOf(projectId))
                .connectedAt(System.currentTimeMillis())
                .build();

        sessionToUserInfo.put(sessionId, tempUser);
        projectToUsers
                .computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>())
                .add(tempUser);

        sandboxService.registerSession(sessionId, session);

        log.info("✅ WebSocket connected - Session: {}, Project: {}", sessionId, projectId);

        sendMessageSafely(session, Map.of(
                "type", "SESSION_INFO",
                "sessionId", sessionId,
                "message", "Connected successfully",
                "timestamp", System.currentTimeMillis()
        ));

        sendOnlineUsers(projectId);

        CompletableFuture.runAsync(() -> {
            try {
                sendProjectState(session, projectId);
            } catch (Exception e) {
                log.error("Failed to send project state", e);
            }
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String projectId = extractProjectId(session);
        String payload = message.getPayload();

        log.info("📨 Received from {} (project: {}): {}", sessionId, projectId, payload);

        try {
            JsonNode data = objectMapper.readTree(payload);
            String type = data.get("type").asText();

            switch (type) {
                case "AUTH":
                    handleAuth(session, data);
                    break;
                case "GET_ONLINE_USERS":
                    handleGetOnlineUsers(session, projectId, data);
                    break;
                case "FILE_SAVE":
                    handleFileSave(session, projectId, data);
                    break;
                case "FILE_CREATE":
                    handleFileCreate(session, projectId, data);
                    break;
                case "FILE_DELETE":
                    handleFileDelete(session, projectId, data);
                    break;
                case "FILE_RENAME":
                    handleFileRename(session, projectId, data);
                    break;
                case "EXECUTE_CODE":
                    handleCodeExecute(session, projectId, data);
                    break;
                case "SEND_INPUT":
                    handleCodeInput(session, projectId, data);
                    break;
                case "STOP_EXECUTION":
                    handleStopExecution(session, projectId, data);
                    break;
                case "CURSOR_MOVE":
                    handleCursorMove(session, projectId, data);
                    break;
                case "KICK_USER":
                    handleKickUser(session, projectId, data);
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
                    sendError(session, "Unknown message type: " + type);
            }

        } catch (Exception e) {
            log.error("Error handling message from {}: {}", sessionId, e.getMessage(), e);
            sendError(session, "Message processing error: " + e.getMessage());
        }
    }

    private void handleKickUser(WebSocketSession session, String projectId, JsonNode data) throws IOException, InterruptedException {
        Long targetUserId = data.get("userId").asLong();
        String sessionId = session.getId();

        UserInfo kickerInfo = sessionToUserInfo.get(sessionId);
        if (kickerInfo == null) {
            sendError(session, "Authentication required");
            return;
        }

        log.info("👢 User {} is kicking user {} from project {}", kickerInfo.getUserId(), targetUserId, projectId);

        String targetSessionId = findSessionIdByUserId(projectId, targetUserId);
        if (targetSessionId == null) {
            sendError(session, "User not found or not connected");
            return;
        }

        WebSocketSession targetSession = projectSessions.get(targetSessionId);
        if (targetSession != null && targetSession.isOpen()) {
            sendMessageSafely(targetSession, Map.of(
                    "type", "USER_KICKED",
                    "message", "You have been removed from the project",
                    "kickedBy", kickerInfo.getUsername(),
                    "timestamp", System.currentTimeMillis()
            ));

            Thread.sleep(100);

            targetSession.close();

            broadcastToProject(projectId, Map.of(
                    "type", "USER_KICKED_BROADCAST",
                    "userId", targetUserId,
                    "kickedBy", kickerInfo.getUserId(),
                    "kickedByUsername", kickerInfo.getUsername(),
                    "timestamp", System.currentTimeMillis()
            ));

            log.info("✅ User {} successfully kicked from project {}", targetUserId, projectId);
        } else {
            sendError(session, "User session not found");
        }
    }

    private String findSessionIdByUserId(String projectId, Long userId) {
        CopyOnWriteArraySet<UserInfo> users = projectToUsers.get(projectId);
        if (users == null) return null;

        return users.stream()
                .filter(user -> userId.equals(user.getUserId()))
                .map(UserInfo::getSessionId)
                .findFirst()
                .orElse(null);
    }

    private void handleAuth(WebSocketSession session, JsonNode data) throws Exception {
        String sessionId = session.getId();
        String projectId = extractProjectId(session);

        Long userId = data.has("userId") ? data.get("userId").asLong() : null;
        String username = data.has("username") ? data.get("username").asText() : "Unknown";
        String email = data.has("email") ? data.get("email").asText() : "";

        log.info("🔐 Authentication - User: {} ({}), Project: {}", username, userId, projectId);

        UserInfo userInfo = UserInfo.builder()
                .sessionId(sessionId)
                .userId(userId)
                .username(username)
                .email(email)
                .projectId(Long.valueOf(projectId))
                .connectedAt(System.currentTimeMillis())
                .build();

        UserInfo oldUser = sessionToUserInfo.put(sessionId, userInfo);

        CopyOnWriteArraySet<UserInfo> projectUsers = projectToUsers.get(projectId);
        if (projectUsers != null) {
            if (oldUser != null) {
                projectUsers.remove(oldUser);
            }
            projectUsers.add(userInfo);
        }

        sendMessageSafely(session, Map.of(
                "type", "AUTH_SUCCESS",
                "message", "Authenticated successfully",
                "user", Map.of(
                        "id", userId,
                        "username", username,
                        "email", email
                ),
                "timestamp", System.currentTimeMillis()
        ));

        broadcastToProject(projectId, Map.of(
                "type", "USER_JOINED",
                "user", Map.of(
                        "id", userId,
                        "username", username,
                        "email", email,
                        "sessionId", sessionId
                ),
                "timestamp", System.currentTimeMillis()
        ));

        sendOnlineUsers(projectId);
    }

    private void handleGetOnlineUsers(WebSocketSession session, String projectId, JsonNode data) {
        log.info("📋 Online users requested for project: {}", projectId);
        sendOnlineUsersToSession(session, projectId);
    }

    private void handleFileSave(WebSocketSession session, String projectId, JsonNode data) {
        String fileId = data.get("fileId").asText();
        String content = data.get("content").asText();

        try {
            fileService.updateFile(Long.valueOf(fileId), content);

            broadcastToProject(projectId, Map.of(
                    "type", "FILE_SAVED",
                    "fileId", fileId,
                    "content", content,
                    "userId", session.getId(),
                    "timestamp", System.currentTimeMillis()
            ));

            sendMessageSafely(session, Map.of(
                    "type", "FILE_SAVED",
                    "message", "File saved successfully",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("Failed to save file", e);
            sendError(session, "Failed to save file: " + e.getMessage());
        }
    }

    private void handleFileCreate(WebSocketSession session, String projectId, JsonNode data) throws IOException {
        log.info("File create requested for project: {}", projectId);
        String fileName = data.get("fileName").asText();
        String filePath = data.get("path").asText();
        String type = data.get("fileType").asText();
        Long parentId = data.get("parentId").asLong();

        FileDto.Response newFile = fileService.createFile(Long.valueOf(projectId), fileName, filePath, type, parentId);

        broadcastToProject(projectId, Map.of(
                "type", "FILE_CREATED",
                "file", newFile,
                "userId", session.getId(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    private void handleFileDelete(WebSocketSession session, String projectId, JsonNode data) throws IOException {
        log.info("File delete requested for project: {}", projectId);
        Long fileId = data.get("fileId").asLong();

        fileService.deleteFile(fileId);

        broadcastToProject(projectId, Map.of(
                "type", "FILE_DELETED",
                "fileId", fileId,
                "userId", session.getId(),
                "timestamp", System.currentTimeMillis()
        ));

        sendMessageSafely(session, Map.of(
                "type", "FILE_DELETED",
                "message", "File deleted successfully",
                "timestamp", System.currentTimeMillis()
        ));
    }

    private void handleFileRename(WebSocketSession session, String projectId, JsonNode data) throws BadRequestException {
        log.info("File rename requested for project: {}", projectId);
        String name = data.get("newFileName").asText();
        Long fileId = data.get("fileId").asLong();

        fileService.renameFile(fileId, name);

        broadcastToProject(projectId, Map.of(
                "type", "FILE_RENAMED",
                "fileId", fileId,
                "name", name,
                "userId", session.getId(),
                "timestamp", System.currentTimeMillis()
        ));

        sendMessageSafely(session, Map.of(
                "type", "FILE_RENAMED",
                "message", "File renamed successfully",
                "timestamp", System.currentTimeMillis()
        ));
    }

    private void handleCursorMove(WebSocketSession session, String projectId, JsonNode data) {
        log.info("Cursor move for project: {}", projectId);
    }

    private void handleCodeExecute(WebSocketSession session, String projectId, JsonNode data) {
        String sessionId = session.getId();
        String connectionId = data.has("connectionId") ? data.get("connectionId").asText() : "unknown";

        log.info("🚀 EXECUTE CODE - Session: {}, Project: {}", sessionId, projectId);

        sendMessageSafely(session, Map.of(
                "type", "EXECUTION_STARTED",
                "message", "Starting code execution...",
                "timestamp", System.currentTimeMillis()
        ));

        CompletableFuture.runAsync(() -> {
            try {
                if (data.has("files")) {
                    log.info("📁 MULTI-FILE EXECUTION DETECTED");
                    Map<String, String> files = new HashMap<>();
                    JsonNode filesNode = data.get("files");
                    filesNode.fields().forEachRemaining(entry -> {
                        files.put(entry.getKey(), entry.getValue().asText());
                    });
                    sandboxService.executeMultiFileInteractive(sessionId, files, connectionId);
                } else if (data.has("code")) {
                    log.info("📄 SINGLE-FILE EXECUTION DETECTED");
                    String code = data.get("code").asText();
                    String filePath = data.has("filePath") ? data.get("filePath").asText() : "main.cpp";
                    log.info("Code length: {}, File: {}", code.length(), filePath);
                    sandboxService.executeCodeInteractive(sessionId, code, connectionId);
                } else {
                    throw new IllegalArgumentException("No code or files provided");
                }
            } catch (Exception e) {
                log.error("❌ Execution failed for session {}: {}", sessionId, e.getMessage(), e);
                sendError(session, "Execution failed: " + e.getMessage());
            }
        });
    }

    private void handleCodeInput(WebSocketSession session, String projectId, JsonNode data) {
        String input = data.get("input").asText();
        String connectionId = data.has("connectionId") ? data.get("connectionId").asText() : "unknown";
        String sessionId = session.getId();

        log.info("⌨️ Sending input - Session: {}, Input: {}", sessionId, input);

        sandboxService.sendInputToExecution(sessionId, input, connectionId);
    }

    private void handleStopExecution(WebSocketSession session, String projectId, JsonNode data) {
        String connectionId = data.has("connectionId") ? data.get("connectionId").asText() : "unknown";
        String sessionId = session.getId();

        log.info("🛑 Stopping execution - Session: {}", sessionId);

        sandboxService.stopExecution(sessionId, connectionId);
    }

    private String extractProjectId(WebSocketSession session) {
        String path = Objects.requireNonNull(session.getUri()).getPath();
        String[] segments = path.split("/");
        return segments.length > 3 ? segments[3] : "unknown";
    }

    private void sendProjectState(WebSocketSession session, String projectId) {
        try {
            sendMessageSafely(session, Map.of(
                    "type", "PROJECT_STATE",
                    "projectId", projectId,
                    "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("Failed to send project state", e);
        }
    }

    private void sendOnlineUsers(String projectId) {
        CopyOnWriteArraySet<UserInfo> users = projectToUsers.get(projectId);
        if (users == null) return;

        List<Map<String, Object>> onlineUsers = users.stream()
                .filter(user -> user.getUserId() != null)
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getUserId());
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("sessionId", user.getSessionId());
                    userMap.put("connectedAt", user.getConnectedAt());
                    return userMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "ONLINE_USERS");
        message.put("users", onlineUsers);
        message.put("count", onlineUsers.size());
        message.put("timestamp", System.currentTimeMillis());

        broadcastToProject(projectId, message);

        log.info("👥 Online users for project {}: {}", projectId, onlineUsers.size());
    }


    private void sendOnlineUsersToSession(WebSocketSession session, String projectId) {
        CopyOnWriteArraySet<UserInfo> users = projectToUsers.get(projectId);
        if (users == null) {
            sendMessageSafely(session, Map.of(
                    "type", "ONLINE_USERS",
                    "users", List.of(),
                    "count", 0,
                    "timestamp", System.currentTimeMillis()
            ));
            return;
        }

        List<Map<String, Object>> onlineUsers = users.stream()
                .filter(user -> user.getUserId() != null)
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getUserId());
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("sessionId", user.getSessionId());
                    userMap.put("connectedAt", user.getConnectedAt());
                    return userMap;
                })
                .collect(Collectors.toList());

        sendMessageSafely(session, Map.of(
                "type", "ONLINE_USERS",
                "users", onlineUsers,
                "count", onlineUsers.size(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    private void sendMessageSafely(WebSocketSession session, Map<String, Object> message) {
        synchronized (sendLock) {
            try {
                if (session.isOpen()) {
                    String jsonMessage = objectMapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(jsonMessage));
                    log.debug("📤 Sent to {}: {}", session.getId(), message.get("type"));
                }
            } catch (Exception e) {
                log.error("Failed to send message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    private void broadcastToProject(String projectId, Map<String, Object> message) {
        CopyOnWriteArraySet<String> sessionIds = projectToSessions.get(projectId);
        if (sessionIds == null || sessionIds.isEmpty()) return;

        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("Failed to serialize broadcast message", e);
            return;
        }

        for (String sessionId : sessionIds) {
            WebSocketSession session = projectSessions.get(sessionId);
            if (session != null && session.isOpen()) {
                synchronized (sendLock) {
                    try {
                        session.sendMessage(new TextMessage(jsonMessage));
                    } catch (IOException e) {
                        log.error("Failed to broadcast to session {}: {}", sessionId, e.getMessage());
                        sessionIds.remove(sessionId);
                        projectSessions.remove(sessionId);
                    }
                }
            } else {
                sessionIds.remove(sessionId);
                projectSessions.remove(sessionId);
            }
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        sendMessageSafely(session, Map.of(
                "type", "ERROR",
                "message", errorMessage,
                "timestamp", System.currentTimeMillis()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String projectId = extractProjectId(session);

        UserInfo userInfo = sessionToUserInfo.get(sessionId);

        projectSessions.remove(sessionId);
        sessionToUserInfo.remove(sessionId);

        CopyOnWriteArraySet<String> projectSessions = projectToSessions.get(projectId);
        if (projectSessions != null) {
            projectSessions.remove(sessionId);
            if (projectSessions.isEmpty()) {
                projectToSessions.remove(projectId);
            }
        }

        CopyOnWriteArraySet<UserInfo> projectUsers = projectToUsers.get(projectId);
        if (projectUsers != null) {
            projectUsers.removeIf(user -> user.getSessionId().equals(sessionId));
            if (projectUsers.isEmpty()) {
                projectToUsers.remove(projectId);
            }
        }

        sandboxService.unregisterSession(sessionId);

        log.info("🔌 WebSocket disconnected - Session: {}, Project: {}, User: {}, Reason: {}",
                sessionId, projectId,
                userInfo != null ? userInfo.getUsername() : "Unknown",
                status.getReason());

        if (userInfo != null && userInfo.getUserId() != null) {
            broadcastToProject(projectId, Map.of(
                    "type", "USER_LEFT",
                    "user", Map.of(
                            "id", userInfo.getUserId(),
                            "username", userInfo.getUsername(),
                            "sessionId", sessionId
                    ),
                    "timestamp", System.currentTimeMillis()
            ));

            sendOnlineUsers(projectId);
        }
    }
}