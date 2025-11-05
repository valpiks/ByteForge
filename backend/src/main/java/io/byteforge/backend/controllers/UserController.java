package io.byteforge.backend.controllers;

import io.byteforge.backend.model.custom.CustomUserDetails;
import io.byteforge.backend.model.dto.UserDto;
import io.byteforge.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/user")
    public ResponseEntity<UserDto.Response> getUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Get info of user with id: {}", userDetails.getId());
        return userService.getUser(userDetails.getId());
    }

    @PutMapping("/user")
    public ResponseEntity<UserDto.Response> updateUser(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody UserDto.Update userData) {
        log.info("Update info of user with id: {}", userDetails.getId());
        return userService.updateUser(userDetails.getId(), userData);
    }

    @PostMapping("/profile-image")
    public ResponseEntity<UserDto.Response> uploadProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("file") MultipartFile profileImage) throws IOException {
        log.info("Upload new profileImage for user with id: {}", userDetails.getId());
        return userService.uploadProfileImage(userDetails.getId(), profileImage);
    }

    @GetMapping("/profile-image/{id}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable("id") Long userId) throws IOException {
        log.info("Get profileImage for user with id: {}", userId);
        return userService.getProfileImage(userId);
    }
}