package com.project.cryptx.config;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.project.cryptx.exception.InvalidTokenException;
import com.project.cryptx.repo.RefreshTokenRepo;
import com.project.cryptx.vo.Refresh_token;
import com.project.cryptx.vo.Users;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Component
public class Utility {

    private static final SingletonLogger log = SingletonLogger.log();

    private RefreshTokenRepo refreshTokenRepo;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

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
                .secure(cookieSecure) // Set dynamically based on environment
                .sameSite("None") // Set to None for cross-origin requests
                .maxAge(3600)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure) // Set dynamically based on environment
                .sameSite("None")
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
        
        // Also clear the XSRF-TOKEN
        ResponseCookie xsrfCookie = ResponseCookie.from("XSRF-TOKEN", "")
                .path("/")
                .secure(cookieSecure)
                .sameSite("None")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, xsrfCookie.toString());
    }

    public ResponseCookie createCleanResponseCookie(String name) {
        return ResponseCookie.from(name, "")
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure) // Must match the secure flag of the original cookie to delete it
                .sameSite("None")
                .maxAge(0)
                .build();
    }

    public Cookie createCleanCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // Setting Max-Age to 0 instructs the browser to delete the cookie immediately
        cookie.setMaxAge(0);
        // Set dynamically based on environment
        cookie.setSecure(cookieSecure);
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
