package com.project.cryptx.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.cryptx.dto.AuthDto;
import com.project.cryptx.dto.LoginReqDto;
import com.project.cryptx.dto.SignUpReqDto;
import com.project.cryptx.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;
    private com.project.cryptx.service.OAuth2CodeService codeService;

    public AuthController(AuthService authService, com.project.cryptx.service.OAuth2CodeService codeService) {
        this.authService = authService;
        this.codeService = codeService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> SignUp(@RequestBody SignUpReqDto signUpReqDto) {
        return authService.SignUp(signUpReqDto);
    }

    @PostMapping("/login")
    public ResponseEntity<?> LogIn(@RequestBody LoginReqDto loginReq, HttpServletResponse response) {
        return authService.LogIn(loginReq, response);
    }

    @GetMapping("/check")
    public ResponseEntity<?> PreCheck(@AuthenticationPrincipal AuthDto authUser) {
        return authService.preCheck(authUser);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        return authService.refreshToken(request, response);
    }

    @PostMapping("/send-reset-password-link")
    public ResponseEntity<?> sendResetLink(@RequestBody String email) {
        return authService.sendResetLink(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestBody String password) {
        return authService.resetPassword(token, password);
    }

    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            return ResponseEntity.ok(java.util.Map.of("token", csrfToken.getToken()));
        }
        return ResponseEntity.ok(java.util.Map.of());
    }

    @PostMapping("/oauth2/exchange")
    public ResponseEntity<?> exchangeOAuth2Code(@RequestBody java.util.Map<String, String> requestBody, HttpServletResponse response) {
        return authService.exchangeOAuth2Code(requestBody, response, codeService);
    }
}