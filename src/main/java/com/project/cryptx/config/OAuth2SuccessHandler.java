package com.project.cryptx.config;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.project.cryptx.repo.UserRepo;
import com.project.cryptx.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.project.cryptx.vo.Users;
import com.project.cryptx.vo.Wallet;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    private final UserRepo userRepo;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    private final com.project.cryptx.service.OAuth2CodeService codeService;

    public OAuth2SuccessHandler(UserRepo userRepo,
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepository,
            com.project.cryptx.service.OAuth2CodeService codeService) {
        this.userRepo = userRepo;
        this.cookieRepository = cookieRepository;
        this.codeService = codeService;
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
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email not provided by OAuth provider");
        }
        String firstName = oauthUser.getAttribute("given_name");
        String lastName = oauthUser.getAttribute("family_name");

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
                    newUser.setFirstName(firstName == null ? "user" : firstName);
                    newUser.setLastName(lastName == null ? "" : lastName);
                    newUser.setUsername(generateUsername(email));
                    newUser.setRole("USER");

                    // ✅ Initialize Wallet for the new user
                    Wallet wallet = new Wallet();
                    wallet.setBalance(java.math.BigDecimal.ZERO);
                    wallet.setUser(newUser);
                    newUser.setWallet(wallet);

                    return userRepo.save(newUser);
                });

        // 🔥 Authorization Code Generation
        String code = UUID.randomUUID().toString();
        codeService.storeCode(code, user.getId());

        // 🔥 Clean up OAuth2 cookies
        cookieRepository.removeAuthorizationRequestCookies(request, response);

        // 🔥 Token Handoff Redirect (Authorization Code Flow)
        String redirectUrl = frontendUrl + "/oauth2-redirect?code=" + code;
        response.sendRedirect(redirectUrl);
    }

    private String generateUsername(String email) {
        String base = email.substring(0, email.indexOf("@"))
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
        String uniquePart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
        return base + "_" + uniquePart;
    }

}
