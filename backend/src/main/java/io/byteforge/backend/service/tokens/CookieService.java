package io.byteforge.backend.service.tokens;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CookieService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public String resolveTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void setCookieInResponse(HttpServletResponse response, long id) {
        String accessToken = jwtService.generateAccessToken(id);
        String refreshToken = jwtService.generateRefreshToken(id);

        refreshTokenService.createRefreshToken(refreshToken, id);

        setTokenCookie(response, "ACCESS_TOKEN", accessToken, (int) (jwtService.getAccessTokenExpiration() / 1000));
        setTokenCookie(response, "REFRESH_TOKEN", refreshToken, (int) (jwtService.getRefreshTokenExpiration() / 1000));
    }

    public void setTokenCookie(HttpServletResponse response, String name, String token, int maxAge) {
        Cookie cookie = new Cookie(name, token);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public void removeTokenCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}