package io.byteforge.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.byteforge.backend.model.dto.ExportTask;
import io.byteforge.backend.model.dto.ProjectDto;
import io.byteforge.backend.model.entity.Project;
import io.byteforge.backend.model.entity.ProjectFile;
import io.byteforge.backend.repository.ProjectFileRepository;
import io.byteforge.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectExportService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;

    private final Map<String, ExportTask> exportTasks = new ConcurrentHashMap<>();

    public ProjectDto.ExportResponse startExport(Long projectId, ProjectDto.ExportRequest request, Long userId) {
        String exportId = "export_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

        ExportTask task = ExportTask.create(exportId, projectId, userId, request);
        exportTasks.put(exportId, task);

        CompletableFuture.runAsync(() -> {
            try {
                processExport(task);
            } catch (Exception e) {
                log.error("Export failed for task: {}", task.getExportId(), e);
                task.setStatus("FAILED");
                task.setMessage("Export failed: " + e.getMessage());
            }
        });

        return ProjectDto.ExportResponse.builder()
                .exportId(exportId)
                .status("PROCESSING")
                .progress(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ProjectDto.ExportResponse getExportStatus(String exportId) {
        ExportTask task = exportTasks.get(exportId);
        if (task == null) {
            throw new RuntimeException("Export task not found");
        }

        return ProjectDto.ExportResponse.builder()
                .exportId(exportId)
                .status(task.getStatus())
                .progress(task.getProgress())
                .message(task.getMessage())
                .downloadUrl(task.getArchivePath() != null ?
                        "/api/projects/export/download/" + exportId : null)
                .createdAt(task.getCreatedAt())
                .build();
    }

    public ResponseEntity<Resource> downloadExport(String exportId) {
        try {
            ExportTask task = exportTasks.get(exportId);
            if (task == null || !"COMPLETED".equals(task.getStatus())) {
                throw new RuntimeException("Export not available");
            }

            Resource resource = new FileSystemResource(task.getArchivePath());
            String filename = "project_export_" + task.getProjectId() + "." + task.getRequest().getFormat().toLowerCase();

            exportTasks.remove(exportId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            throw new RuntimeException("Download failed: " + e.getMessage());
        }
    }

    private void processExport(ExportTask task) {
        try {
            Project project = projectRepository.findById(task.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));

            task.updateProgress(25, "Collecting files...");
            List<ProjectFile> allFiles = getAllProjectFilesWithChildren(task.getProjectId());

            task.updateProgress(50, "Filtering files...");
            List<ProjectFile> filesToExport = filterFiles(allFiles, task.getRequest());

            task.updateProgress(75, "Creating archive...");
            Path archiveFile = createArchive(project, filesToExport, task.getRequest().getFormat());

            task.updateProgress(100, "Export completed");
            task.setArchivePath(archiveFile.toString());
            task.setStatus("COMPLETED");

        } catch (Exception e) {
            log.error("Export failed for task: {}", task.getExportId(), e);
            task.setStatus("FAILED");
            task.setMessage("Export failed: " + e.getMessage());
        }
    }

    @Transactional
    public List<ProjectFile> getAllProjectFilesWithChildren(Long projectId) {
        List<ProjectFile> allFiles = projectFileRepository.findAllByProjectIdWithChildren(projectId);

        for (ProjectFile file : allFiles) {
            if (file.getChildren() != null) {
                file.getChildren().size();
            }
        }

        return allFiles;
    }

    private List<ProjectFile> filterFiles(List<ProjectFile> allFiles, ProjectDto.ExportRequest request) {
        return allFiles.stream()
                .filter(file -> {
                    if (file.getType() == io.byteforge.backend.model.custom.FileType.FOLDER) {
                        return true;
                    }

                    String path = file.getPath().toLowerCase();

                    return !Boolean.FALSE.equals(request.getIncludeGit()) || !path.contains(".git/");
                })
                .collect(Collectors.toList());
    }

    private Path createArchive(Project project, List<ProjectFile> files, String format) throws IOException {
        Path tempDir = Files.createTempDirectory("project-export-" + project.getId());

        try {
            createProjectMetadata(project, files, tempDir);

            for (ProjectFile file : files) {
                if (file.getType() == io.byteforge.backend.model.custom.FileType.FOLDER) {
                    Path dirPath = tempDir.resolve(file.getPath());
                    Files.createDirectories(dirPath);
                } else {
                    Path filePath = tempDir.resolve(file.getPath());
                    Files.createDirectories(filePath.getParent());
                    String content = file.getContent() != null ? file.getContent() : "";
                    Files.write(filePath, content.getBytes());
                }
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            String fileName = sanitizeFileName(project.getTitle()) + "_" + timestamp + "." + format.toLowerCase();
            Path archiveFile = Files.createTempFile("export-", "." + format.toLowerCase());

            if ("RAR".equalsIgnoreCase(format)) {
                createRarArchive(tempDir, archiveFile);
            } else {
                createZipArchive(tempDir, archiveFile);
            }

            return archiveFile;

        } finally {
            deleteDirectoryRecursive(tempDir);
        }
    }

    private void createProjectMetadata(Project project, List<ProjectFile> files, Path tempDir) throws IOException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("projectName", project.getTitle());
        metadata.put("projectId", project.getId());
        metadata.put("exportDate", LocalDateTime.now().toString());
        metadata.put("totalFiles", files.stream().filter(f -> f.getType() != io.byteforge.backend.model.custom.FileType.FOLDER).count());
        metadata.put("totalFolders", files.stream().filter(f -> f.getType() == io.byteforge.backend.model.custom.FileType.FOLDER).count());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        Path metadataFile = tempDir.resolve("PROJECT_INFO.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(metadataFile.toFile(), metadata);
    }

    private void createZipArchive(Path sourceDir, Path zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            String relativePath = sourceDir.relativize(path).toString().replace("\\", "/");
                            zos.putNextEntry(new ZipEntry(relativePath));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    private void createRarArchive(Path sourceDir, Path rarFile) throws IOException {
        if (!isRarAvailable()) {
            createZipArchive(sourceDir, rarFile);
            return;
        }

        ProcessBuilder pb = new ProcessBuilder("rar", "a", "-ep1", rarFile.toString(), sourceDir.toString());
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("RAR compression failed");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("RAR compression interrupted", e);
        }
    }

    private boolean isRarAvailable() {
        try {
            Process process = new ProcessBuilder("rar").start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void deleteDirectoryRecursive(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("Failed to delete temporary file: {}", p);
                        }
                    });
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
}