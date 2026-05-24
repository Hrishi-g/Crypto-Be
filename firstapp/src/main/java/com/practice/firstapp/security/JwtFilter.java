package com.practice.firstapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import com.practice.firstapp.config.SingletonLogger;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.practice.firstapp.dto.AuthDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private static final SingletonLogger log = SingletonLogger.log();

    private JwtUtils jwtUtils;

    public JwtFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.debug("Filter triggered for URL: {}", request.getRequestURI());

        String token = null;
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(cookie -> "jwt_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        // Wrap in try-catch to handle ExpiredJwtException
        try {
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                Long userId = jwtUtils.getUserIdFromToken(token);
                Claims claims = jwtUtils.extractAllClaims(token);
                // String username = claims.get("username", String.class);
                // String email = claims.get("email", String.class);
                List<String> roles = claims.get("roles", List.class);

                if (roles == null)
                    roles = List.of();

                if (userId != null && !jwtUtils.isTokenExpired(token)) {
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Stateless: Treat the JWT as the source of truth for the user's roles
                    AuthDto authUser = new AuthDto(userId, roles);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            authUser, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            // Log the expiration and continue the filter chain
            log.warn("JWT Token has expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            // Handle other JWT-related errors (invalid signature, malformed token)
            log.error("JWT validation failed: {}", e.getMessage(), e);
        }

        // Crucial: This must be outside the catch block so the request isn't dropped
        filterChain.doFilter(request, response);
    }
}
