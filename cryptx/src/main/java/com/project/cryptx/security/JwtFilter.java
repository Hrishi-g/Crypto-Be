package com.project.cryptx.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.project.cryptx.dto.AuthDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    private JwtUtils jwtUtils;

    public JwtFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        log.debug("Filter triggered for URL: {}", request.getRequestURI());

        String[] tokens = null;
        if (request.getCookies() != null) {
            tokens = Arrays.stream(request.getCookies())
                    .filter(cookie -> "jwt_token".equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .toArray(String[]::new);
        }

        if (tokens != null && tokens.length > 0) {
            log.info("Found {} jwt_token cookies for URL: {}", tokens.length, request.getRequestURI());
            for (String token : tokens) {
                try {
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        Long userId = jwtUtils.getUserIdFromToken(token);
                        Claims claims = jwtUtils.extractAllClaims(token);
                        List<String> roles = claims.get("roles", List.class);

                        if (roles == null)
                            roles = List.of();

                        if (userId != null && !jwtUtils.isTokenExpired(token)) {
                            log.info("Successfully validated JWT for userId: {}", userId);
                            List<SimpleGrantedAuthority> authorities = roles.stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());

                            AuthDto authUser = new AuthDto(userId, roles);

                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    authUser, null, authorities);

                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);

                            // Successfully authenticated with this token, stop checking others
                            break;
                        } else {
                            log.warn("JWT is invalid or expired for token starting with: {}",
                                    token.substring(0, Math.min(10, token.length())));
                        }
                    }
                } catch (ExpiredJwtException e) {
                    log.warn("One of the JWT Tokens has expired: {}", e.getMessage());
                } catch (JwtException | IllegalArgumentException e) {
                    log.error("One of the JWT validations failed: {}", e.getMessage(), e);
                }
            }
        } else {
            log.info("No jwt_token cookie found in request to URL: {}", request.getRequestURI());
        }

        // Crucial: This must be outside the catch block so the request isn't dropped
        filterChain.doFilter(request, response);
    }
}
