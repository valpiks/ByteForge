package io.byteforge.backend.service;

import io.byteforge.backend.exceptions.UserNotFoundException;
import io.byteforge.backend.model.dto.UserDto;
import io.byteforge.backend.model.entity.User;
import io.byteforge.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    @Value("${file.storage.path:/uploads}")
    private String storagePath;

    private final UserRepository userRepository;

    public ResponseEntity<UserDto.Response> getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        return ResponseEntity.status(HttpServletResponse.SC_OK).body(UserDto.Response.toDto(user));
    }

    public ResponseEntity<UserDto.Response> updateUser(Long userId, UserDto.Update userData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        user.setEmail(userData.getEmail());
        user.setUsername(userData.getUsername());

        return ResponseEntity.status(HttpServletResponse.SC_OK)
                .body(UserDto.Response.toDto(userRepository.save(user)));
    }

    public ResponseEntity<UserDto.Response> uploadProfileImage(Long userId, MultipartFile profileImage) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        Path storageDir = Paths.get(storagePath);

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Path filePath = storageDir.resolve(user.getProfileImage());
            Files.deleteIfExists(filePath);
        }

        String uuid = UUID.randomUUID().toString();

        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }

        String fileExtension = getFileExtension(profileImage.getOriginalFilename());
        String fileName = uuid + (fileExtension != null ? "." + fileExtension : "");
        Path filePath = storageDir.resolve(fileName);

        Files.copy(profileImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        user.setProfileImage(fileName);
        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpServletResponse.SC_OK).body(UserDto.Response.toDto(savedUser));
    }

    public ResponseEntity<Resource> getProfileImage(Long userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id: " + userId + " not found"));

        if (user.getProfileImage() == null || user.getProfileImage().isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = Paths.get(storagePath).resolve(user.getProfileImage());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = Files.probeContentType(filePath);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + user.getProfileImage() + "\"")
                .body(resource);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
