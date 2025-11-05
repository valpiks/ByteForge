package io.byteforge.backend.model.dto;

import io.byteforge.backend.model.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;


public class UserDto {

    @Data
    public static class Create {
        @NotBlank
        private String username;

        @Email
        private String email;

        @Size(min = 8)
        private String password;
    }

    @Data
    public static class Login {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
        private String password;
    }

    @Data
    public static class Update {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String username;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String username;
        private String email;
        private String created_at;
        private String profileImageUrl;

        public static Response toDto(User user) {
            return Response.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .created_at(user.getCreatedAt().toString())
                    .profileImageUrl(user.getProfileImage() != null ?
                            "/api/users/" + user.getId() + "/profile-image" : null)
                    .build();
        }
    }

}

