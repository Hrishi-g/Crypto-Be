package com.practice.firstapp.config;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.practice.firstapp.repo.UserRepo;
import com.practice.firstapp.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.practice.firstapp.security.JwtUtils;
import com.practice.firstapp.vo.Refresh_token;
import com.practice.firstapp.vo.Users;
import com.practice.firstapp.vo.Wallet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    private final UserRepo userRepo;
    private final JwtUtils jwtUtils;
    private final Utility utility;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    public OAuth2SuccessHandler(UserRepo userRepo,
            JwtUtils jwtUtils,
            Utility utility,
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepository) {
        this.userRepo = userRepo;
        this.jwtUtils = jwtUtils;
        this.utility = utility;
        this.cookieRepository = cookieRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String provider = authToken.getAuthorizedClientRegistrationId();
        String providerId = oauthUser.getName();
        String email = oauthUser.getAttribute("email");
        String userName = oauthUser.getAttribute("name");

        // 🔥 Find or create user
        Users user = userRepo.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // 1. Check if user with this email already exists
                    Users existingUser = userRepo.findByEmail(email).orElse(null);
                    if (existingUser != null) {
                        // Link existing account to Google
                        existingUser.setProvider(provider);
                        existingUser.setProviderId(providerId);
                        return userRepo.save(existingUser);
                    }

                    // 2. Otherwise, create a brand new user
                    Users newUser = new Users();
                    newUser.setProvider(provider);
                    newUser.setProviderId(providerId);
                    newUser.setEmail(email);
                    newUser.setUsername(generateUsername(userName));
                    newUser.setRole("USER");

                    // ✅ Initialize Wallet for the new user
                    Wallet wallet = new Wallet();
                    wallet.setBalance(java.math.BigDecimal.ZERO);
                    wallet.setUser(newUser);
                    newUser.setWallet(wallet);

                    return userRepo.save(newUser);
                });

        // 🔥 SAME JWT FLOW (reuse your logic)
        String jwt = jwtUtils.generateAccessToken(user);
        utility.addJwtCookie(response, jwt);

        // optional: refresh token
        Refresh_token refreshToken = utility.generateRefreshToken(user);
        utility.addRefreshCookie(response, refreshToken.getToken());

        // 🔥 Clean up OAuth2 cookies
        cookieRepository.removeAuthorizationRequestCookies(request, response);

        // 🔥 Redirect to frontend
        response.sendRedirect(frontendUrl + "/");
    }

    private String generateUsername(String name) {
        String firstName = name.trim().split("\\s+")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        String uniquePart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return firstName + "_" + uniquePart;
    }

}
