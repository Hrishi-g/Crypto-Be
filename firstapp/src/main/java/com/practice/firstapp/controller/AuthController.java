package com.practice.firstapp.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.practice.firstapp.dto.LoginReqDto;
import com.practice.firstapp.dto.SignUpReqDto;
import com.practice.firstapp.service.AuthService;
import com.practice.firstapp.vo.Users;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<?> PreCheck() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Users) {
            return ResponseEntity.ok(Map.of("authenticated", true));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request, HttpServletResponse response) {
        return authService.LogOut(request, response);
    }
}