package com.practice.firstapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.practice.firstapp.dto.UserUpdateReqDto;
import com.practice.firstapp.service.AuthService;

@RestController
@RequestMapping("/auth/admin")
public class AdminController {

    private AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateReqDto updateReq) {

        return authService.updateUserAsAdmin(userId, updateReq);
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return authService.getAllUsers();
    }
}
