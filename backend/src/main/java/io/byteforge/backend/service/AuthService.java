package io.byteforge.backend.service;

import io.byteforge.backend.exceptions.AuthenticationException;
import io.byteforge.backend.exceptions.InvalidPasswordException;
import io.byteforge.backend.exceptions.UserAlreadyExistsException;
import io.byteforge.backend.exceptions.UserNotFoundException;
import io.byteforge.backend.model.dto.UserDto;
import io.byteforge.backend.model.entity.Token;
import io.byteforge.backend.model.entity.User;
import io.byteforge.backend.repository.TokenRepository;
import io.byteforge.backend.repository.UserRepository;
import io.byteforge.backend.service.tokens.CookieService;
import io.byteforge.backend.service.tokens.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final CookieService cookieService;

    public ResponseEntity<UserDto.Response> signUp(UserDto.Create requestUser, HttpServletResponse response) {
        Optional<User> existingUser = userRepository.findByEmail(requestUser.getEmail());

        if (existingUser.isPresent()) {
            throw new UserAlreadyExistsException("User with email " + requestUser.getEmail() + " already exists");
        }

        User user = new User();
        user.setUsername(requestUser.getUsername());
        user.setEmail(requestUser.getEmail());
        user.setPassword(passwordEncoder.encode(requestUser.getPassword()));

        UserDto.Response createdUser = UserDto.Response.toDto(userRepository.save(user));

        cookieService.setCookieInResponse(response, createdUser.getId());

        return ResponseEntity.status(HttpServletResponse.SC_CREATED).body(createdUser);
    }

    public ResponseEntity<UserDto.Response> signIn(UserDto.Login requestUser, HttpServletResponse response) {
        Optional<User> existingUser = userRepository.findByEmail(requestUser.getEmail());

        if (existingUser.isEmpty()) {
            throw new UserNotFoundException("User with email " + requestUser.getEmail() + " not found");
        }

        User user = existingUser.get();
        if (!passwordEncoder.matches(requestUser.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Invalid password");
        }

        UserDto.Response responseUser = UserDto.Response.toDto(user);
        cookieService.setCookieInResponse(response, responseUser.getId());

        return ResponseEntity.status(HttpServletResponse.SC_OK).body(responseUser);
    }

    public ResponseEntity<Void> signOut(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.resolveTokenFromCookie(request, "REFRESH_TOKEN");

        if (refreshToken == null) {
            throw new AuthenticationException("Refresh token not found");
        }

        Token storedToken = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));
        tokenRepository.delete(storedToken);

        cookieService.removeTokenCookie(response, "ACCESS_TOKEN");
        cookieService.removeTokenCookie(response, "REFRESH_TOKEN");

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = cookieService.resolveTokenFromCookie(request, "REFRESH_TOKEN");

        log.info("Start refresh() function");

        if (refreshToken == null) {
            throw new AuthenticationException("Refresh token not found");
        }

        Long userId = jwtService.getUserIdFromToken(refreshToken);

        Token storedToken = tokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(storedToken);
            throw new AuthenticationException("Refresh token expired");
        }

        if (!jwtService.validateToken(refreshToken)) {
            tokenRepository.delete(storedToken);
            throw new AuthenticationException("Invalid refresh token signature");
        }

        log.info("Start creating new refresh token");

        cookieService.setCookieInResponse(response, userId);
        tokenRepository.delete(storedToken);

        log.info("End of function refresh()");

        return ResponseEntity.ok().build();
    }
}