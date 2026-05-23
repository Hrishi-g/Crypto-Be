package com.practice.firstapp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.practice.firstapp.dto.AuthDto;
import com.practice.firstapp.dto.LoginReqDto;
import com.practice.firstapp.dto.SignUpReqDto;
import com.practice.firstapp.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@AuthenticationPrincipal AuthDto user, HttpServletRequest request,
            HttpServletResponse response) {
        Long userId = user.getId();
        return authService.LogOut(userId, request, response);
    }

    @PostMapping("/send-reset-password-link")
    public ResponseEntity<?> sendResetLink(@RequestBody String email) {
        System.out.println("resetPassword" + email);
        return authService.sendResetLink(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestBody String password) {
        return authService.resetPassword(token, password);
    }

}