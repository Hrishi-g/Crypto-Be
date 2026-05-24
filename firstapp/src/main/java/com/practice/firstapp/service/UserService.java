package com.practice.firstapp.service;

import java.util.Arrays;
import java.util.Map;

import com.practice.firstapp.config.SingletonLogger;
import com.practice.firstapp.exception.ResourceNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.practice.firstapp.config.Utility;
import com.practice.firstapp.dto.PasswordResetReqDto;
import com.practice.firstapp.dto.UpdateUserProfile;
import com.practice.firstapp.dto.UserProfileDto;
import com.practice.firstapp.repo.UserRepo;
import com.practice.firstapp.vo.Users;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserService {

    private static final SingletonLogger log = SingletonLogger.log();

    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private Utility utility;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder, Utility utility) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.utility = utility;
    }

    @Cacheable(key = "#userId", cacheNames = "user", unless = "#result == null", cacheManager = "cacheManager")
    public UserProfileDto getUser(Long userId) {
        log.info("Cache being set for User Profile");
        Users user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        UserProfileDto userProfileDto = new UserProfileDto();
        userProfileDto.setUsername(user.getUsername());
        if (user.getWallet() != null) {
            userProfileDto.setTotalAmount(user.getWallet().getBalance());
        } else {
            userProfileDto.setTotalAmount(java.math.BigDecimal.ZERO);
        }
        userProfileDto.setHasPassword(user.getPassword() != null);
        userProfileDto.setFirstName(user.getFirstName());
        userProfileDto.setLastName(user.getLastName());
        userProfileDto.setEmail(user.getEmail());
        userProfileDto.setDob(user.getDob());
        return userProfileDto;
    }

    @CachePut(key = "#userId", cacheNames = "user", cacheManager = "cacheManager")
    public UserProfileDto updateProfile(Long userId, UpdateUserProfile updateReq) {
        Users existingUser = userRepo.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (updateReq.getFirstName() != null) {
            existingUser.setFirstName(updateReq.getFirstName());
        }
        if (updateReq.getLastName() != null) {
            existingUser.setLastName(updateReq.getLastName());
        }
        if (updateReq.getDob() != null) {
            existingUser.setDob(updateReq.getDob());
        }
        userRepo.save(existingUser);
        UserProfileDto userProfileDto = new UserProfileDto();
        userProfileDto.setUsername(existingUser.getUsername());
        if (existingUser.getWallet() != null) {
            userProfileDto.setTotalAmount(existingUser.getWallet().getBalance());
        } else {
            userProfileDto.setTotalAmount(java.math.BigDecimal.ZERO);
        }
        userProfileDto.setHasPassword(existingUser.getPassword() != null);
        userProfileDto.setFirstName(existingUser.getFirstName());
        userProfileDto.setLastName(existingUser.getLastName());
        userProfileDto.setEmail(existingUser.getEmail());
        userProfileDto.setDob(existingUser.getDob());
        return userProfileDto;
    }

    @CacheEvict(key = "#userId", cacheNames = "user", cacheManager = "cacheManager")
    public ResponseEntity<?> setPassword(Long userId, PasswordResetReqDto passwordResetReqDto) {
        Users user = userRepo.findById(userId).orElse(null);
        if (!passwordResetReqDto.getPassword().equals(passwordResetReqDto.getConfirmPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Passwords do not match"));
        }
        user.setPassword(passwordEncoder.encode(passwordResetReqDto.getPassword()));
        userRepo.save(user);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Password set successfully"));

    }

    @CacheEvict(key = "#userId", cacheNames = "user", cacheManager = "cacheManager")
    public ResponseEntity<?> LogOut(Long userId, HttpServletRequest request,
            HttpServletResponse response) {
        String refreshTokenStr = Arrays.stream(request.getCookies() == null ? new Cookie[0] : request.getCookies())
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        if (refreshTokenStr != null) {
            utility.deleteRefreshTokenFromDb(refreshTokenStr);
        }
        utility.clearCookies(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Logged out successfully"));
    }

}
