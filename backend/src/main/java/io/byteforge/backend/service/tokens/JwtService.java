package io.byteforge.backend.service.tokens;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token.expiration}")
    private @Getter long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private @Getter long refreshTokenExpiration;


    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException("JWT secret must be at least 512 bits (64 characters) for HS512");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long id) {
        return generateToken(id, accessTokenExpiration);
    }

    public String generateRefreshToken(Long id) {
        return generateToken(id, refreshTokenExpiration);
    }

    private String generateToken(Long id, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(id.toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}