package com.practice.firstapp.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import com.practice.firstapp.vo.Users;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Secret key for signing
    public SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Generate JWT token
    public String generateAccessToken(Users user) {
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                // .claim("username", user.getUsername())
                // .claim("email", user.getEmail())
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 10)) // 10 min
                .signWith(getSecretKey())
                .compact();
    }

    // Validate token
    // public Boolean validateToken(String token, Users user) {
    // Long userId = getUserIdFromToken(token);
    // return userId.equals(user.getId()) && !isTokenExpired(token);
    // }

    // Extract identifier from token
    // public String getIdentifierFromToken(String token) {
    // Claims claims = Jwts.parserBuilder()
    // .setSigningKey(getSecretKey())
    // .build()
    // .parseClaimsJws(token)
    // .getBody();
    // return claims.getSubject();
    // }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // public String extractUsername(String token) {
    // return extractAllClaims(token).get("username", String.class);
    // }

    // public String extractEmail(String token) {
    // return extractAllClaims(token).get("email", String.class);
    // }

    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List<?>) {
            return ((List<?>) roles).stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    // Check token expiration
    public Boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
