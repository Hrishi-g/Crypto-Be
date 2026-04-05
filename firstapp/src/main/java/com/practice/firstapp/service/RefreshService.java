package com.practice.firstapp.service;

import java.util.Arrays;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.practice.firstapp.config.Utility;
import com.practice.firstapp.repo.RefreshTokenRepo;
import com.practice.firstapp.security.JwtUtils;
import com.practice.firstapp.vo.Refresh_token;
import com.practice.firstapp.vo.Users;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RefreshService {

    private final Utility utility; // Renamed for clarity
    private final RefreshTokenRepo refreshTokenRepo;
    private final JwtUtils jwtUtils;

    public RefreshService(Utility utility, RefreshTokenRepo refreshTokenRepo, JwtUtils jwtUtils) {
        this.utility = utility;
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtUtils = jwtUtils;
    }

    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. Extract the refresh token from the cookies
        String refreshTokenStr = Arrays.stream(request.getCookies() == null ? new Cookie[0] : request.getCookies())
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Refresh token missing"));

        // 2. Find and validate the token in the database
        Refresh_token refreshToken = refreshTokenRepo.findByToken(refreshTokenStr)
                .map(utility::validateRefreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // 3. Generate a new Access Token
        Users user = refreshToken.getUser();
        String newAccessToken = jwtUtils.generateAccessToken(user);

        // 4. Update the jwt_token cookie using the new Utility method
        // This method now adds the SameSite=Lax header directly
        utility.addJwtCookie(response, newAccessToken);

        return ResponseEntity.ok(java.util.Map.of("message", "Token refreshed successfully"));
    }
}
