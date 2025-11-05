package io.byteforge.backend.model.dto;

import io.byteforge.backend.model.entity.Project;
import io.byteforge.backend.model.entity.SharedProject;
import io.byteforge.backend.model.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Optional;

public class ProjectDto {

    @Data
    public static class Create {
        @NotBlank
        private String title;
        private String description;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Edit extends Create {
        @NotBlank
        private Long projectId;
    }

    @Data
    @Builder
    public static class InviteToken {
        private String token;
        private String role;

        public static InviteToken toDto(String token) {
            return InviteToken.builder().token(token).build();
        }
    }

    @Data
    @Builder
    public static class JoinResponse {
        private Long projectId;

        public static JoinResponse toDto(Long projectId) {
            return JoinResponse.builder().projectId(projectId).build();
        }
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String description;
        private LocalDateTime updatedAt;
        private LocalDateTime createdAt;
        private Long ownerId;
        private Boolean isPublic;
        private Long fileCount;

        public static Response toDto(Project project, Long fileCount) {
            if (project == null) {
                throw new IllegalArgumentException("Project cannot be null");
            }

            return Response.builder()
                    .id(project.getId())
                    .title(project.getTitle())
                    .description(project.getDescription())
                    .createdAt(project.getCreatedAt())
                    .isPublic(project.getIsPublic())
                    .updatedAt(project.getUpdatedAt())
                    .ownerId(project.getOwner().getId())
                    .fileCount(fileCount)
                    .build();
        }
    }

    @Data
    public static class ChangeRole {
        private String role;
    }

    @Data
    @Builder
    public static class ProjectUsers {
        private Long id;
        private String username;
        private String email;
        private String role;

        public static ProjectUsers toOwner(Optional<User> user) {
            if (user.isEmpty()) {
                throw new IllegalArgumentException("Owner not exists");
            }

            return ProjectUsers.builder()
                    .id(user.get().getId())
                    .username(user.get().getUsername())
                    .email(user.get().getEmail())
                    .role("OWNER")
                    .build();
        }

        public static ProjectUsers toUser(SharedProject sharedProject) {
            return ProjectUsers.builder()
                    .id(sharedProject.getUser().getId())
                    .username(sharedProject.getUser().getUsername())
                    .email(sharedProject.getUser().getEmail())
                    .role(sharedProject.getAccessLevel())
                    .build();
        }
    }

    @Data
    @Builder
    public static class ExportRequest {
        private Boolean includeGit;
        private String format;
    }

    @Data
    @Builder
    public static class ExportResponse {
        private String exportId;
        private String status;
        private String downloadUrl;
        private String message;
        private Integer progress;
        private LocalDateTime createdAt;
    }

}
