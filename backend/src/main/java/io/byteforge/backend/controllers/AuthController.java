package io.byteforge.backend.controllers;

import io.byteforge.backend.model.custom.CustomUserDetails;
import io.byteforge.backend.model.dto.UserDto;
import io.byteforge.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService auth;

    @PostMapping("/signIn")
    public ResponseEntity<UserDto.Response> singIn(
            @Valid @RequestBody UserDto.Login request,
            HttpServletResponse response) {
        log.info("SignIn user with email: {}", request.getEmail());
        return auth.signIn(request, response);
    }

    @PostMapping("/signUp")
    public ResponseEntity<UserDto.Response> singUp(
            @Valid @RequestBody UserDto.Create request,
            HttpServletResponse response) {
        log.info("SignUp user with email: {}", request.getEmail());
        return auth.signUp(request, response);
    }

    @PostMapping("/signOut")
    public ResponseEntity<Void> signOut(
            HttpServletResponse response, HttpServletRequest request) {
        log.info("signOut user");
        return auth.signOut(request, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("Refresh user token");
        return auth.refresh(request, response);
    }
}