package com.practice.firstapp.config;

import java.time.Instant;
import com.practice.firstapp.exception.InvalidTokenException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import com.practice.firstapp.repo.RefreshTokenRepo;
import com.practice.firstapp.vo.Refresh_token;
import com.practice.firstapp.vo.Users;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Component
public class Utility {

    private static final SingletonLogger log = SingletonLogger.log();

    private RefreshTokenRepo refreshTokenRepo;

    public Utility(RefreshTokenRepo refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    @Transactional
    public Refresh_token generateRefreshToken(Users user) {
        // Check if token already exists for this user
        Refresh_token refreshToken = refreshTokenRepo.findByUser(user).orElse(new Refresh_token());

        // Update its value and expiry date
        refreshToken.setToken(java.util.UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60)); // 7 days

        refreshTokenRepo.save(refreshToken);
        return refreshToken;
    }

    public void addJwtCookie(HttpServletResponse response, String jwtToken) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", jwtToken)
                .path("/")
                .httpOnly(true)
                .secure(false) // Set to true only for HTTPS/AWS
                .sameSite("Lax") // This fixes the cross-site block you saw in the UI
                .maxAge(3600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60) // 7 days
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Refresh_token validateRefreshToken(Refresh_token token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepo.delete(token);
            throw new InvalidTokenException("Refresh token expired. Please log in again.");
        }
        return token;
    }

    public void clearCookies(HttpServletResponse response) {
        // We reuse createCleanCookie to avoid repeating code
        response.addHeader(HttpHeaders.SET_COOKIE, createCleanResponseCookie("jwt_token").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createCleanResponseCookie("refresh_token").toString());
    }

    public ResponseCookie createCleanResponseCookie(String name) {
        return ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .secure(false) // Must match secure(false) of original cookies to delete them successfully
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }

    public Cookie createCleanCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // Setting Max-Age to 0 instructs the browser to delete the cookie immediately
        cookie.setMaxAge(0);
        // Secure=true is required if you deploy to AWS or Render with HTTPS
        cookie.setSecure(false);
        return cookie;
    }

    // Add your DB deletion call here or in a dedicated TokenService
    public void deleteRefreshTokenFromDb(String token) {
        try {
            // This removes the specific session record from PostgreSQL
            refreshTokenRepo.deleteByToken(token);
            log.info("Refresh token successfully removed from DB");
        } catch (Exception e) {
            // Log the error but allow the logout process to continue
            log.error("Error deleting token from DB: {}", e.getMessage(), e);
        }
    }
}
