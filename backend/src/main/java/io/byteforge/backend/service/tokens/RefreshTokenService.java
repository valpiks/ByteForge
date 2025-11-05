package io.byteforge.backend.service.tokens;

import io.byteforge.backend.exceptions.UserNotFoundException;
import io.byteforge.backend.model.entity.Token;
import io.byteforge.backend.model.entity.User;
import io.byteforge.backend.repository.TokenRepository;
import io.byteforge.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    public Token createRefreshToken(String rawToken, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

        Token newToken = new Token(
                rawToken,
                Instant.now().plus(refreshTokenExpiration, ChronoUnit.MILLIS),
                user
        );

        return tokenRepository.save(newToken);
    }
}