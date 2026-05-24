package com.practice.firstapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.practice.firstapp.dto.AuthDto;
import com.practice.firstapp.dto.PasswordResetReqDto;
import com.practice.firstapp.dto.UpdateUserProfile;
import com.practice.firstapp.dto.UserProfileDto;
import com.practice.firstapp.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/user")
public class UserController {

    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile") // Authentication authentication -> @AuthenticationPrincipal Users user
    public ResponseEntity<UserProfileDto> getUser(@AuthenticationPrincipal AuthDto user) {
        Long userId = user.getId();
        return ResponseEntity.ok(userService.getUser(userId));
    }

    @PutMapping("/update/profile")
    public ResponseEntity<UserProfileDto> updateProfile(@AuthenticationPrincipal AuthDto user,
            @RequestBody UpdateUserProfile updateReq) {
        Long userId = user.getId();
        return ResponseEntity.ok(userService.updateProfile(userId, updateReq));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@AuthenticationPrincipal AuthDto user, HttpServletRequest request,
            HttpServletResponse response) {
        Long userId = user.getId();
        return userService.LogOut(userId, request, response);
    }

    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@AuthenticationPrincipal AuthDto authUser,
            @RequestBody PasswordResetReqDto passwordResetReqDto) {
        return userService.setPassword(authUser.getId(), passwordResetReqDto);
    }

}
