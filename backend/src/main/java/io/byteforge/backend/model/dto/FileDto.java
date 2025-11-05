package io.byteforge.backend.model.dto;

import io.byteforge.backend.model.custom.FileType;
import io.byteforge.backend.model.entity.ProjectFile;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

public class FileDto {

    @Data
    public static class Create {
        @NotNull
        String name;

        @NotNull
        String path;

        @NotNull
        FileType type;

        Long parentId;
    }

    @Data
    public static class Update {
        String content;
    }

    @Data
    @Builder
    public static class Response {
        Long id;
        String name;
        String path;
        String content;
        Long parentId;
        FileType type;
        private LocalDateTime updatedAt;
        private LocalDateTime createdAt;

        public static Response toDto(ProjectFile projectFile) {
            if (projectFile == null) {
                throw new IllegalArgumentException("Project cannot be null");
            }

            return Response.builder()
                    .id(projectFile.getId())
                    .name(projectFile.getName())
                    .content(projectFile.getContent())
                    .createdAt(projectFile.getCreatedAt())
                    .updatedAt(projectFile.getUpdatedAt())
                    .path(projectFile.getPath())
                    .type(projectFile.getType())
                    .parentId(projectFile.getParent() != null ? projectFile.getParent().getId() : null)
                    .build();
        }
    }

}
