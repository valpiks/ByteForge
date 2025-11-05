package io.byteforge.backend.model.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExportTask {
    private String exportId;
    private Long projectId;
    private Long userId;
    private ProjectDto.ExportRequest request;
    private String status;
    private String message;
    private Integer progress;
    private String archivePath;
    private LocalDateTime createdAt;

    public static ExportTask create(String exportId, Long projectId, Long userId, ProjectDto.ExportRequest request) {
        return ExportTask.builder()
                .exportId(exportId)
                .projectId(projectId)
                .userId(userId)
                .request(request)
                .status("PROCESSING")
                .message("")
                .progress(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ExportTask updateProgress(int progress, String message) {
        this.progress = progress;
        this.message = message;
        return this;
    }
}