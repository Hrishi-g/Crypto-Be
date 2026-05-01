package com.practice.firstapp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.firstapp.dto.AuthDto;
import com.practice.firstapp.dto.LoginReqDto;
import com.practice.firstapp.dto.PasswordResetReqDto;
import com.practice.firstapp.dto.SignUpReqDto;
import com.practice.firstapp.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
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
        if (authUser != null) {
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "id", authUser.getId(),
                    "role", authUser.getRole() != null ? authUser.getRole() : "USER"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // @PostMapping("/set-password")
    // public ResponseEntity<?> setPassword(@AuthenticationPrincipal AuthDto
    // authUser,
    // @RequestBody PasswordResetReqDto passwordResetReqDto) {
    // return authService.setPassword(authUser, passwordResetReqDto);
    // }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@AuthenticationPrincipal AuthDto user, HttpServletRequest request,
            HttpServletResponse response) {
        Long userId = user.getId();
        return authService.LogOut(userId, request, response);
    }

}