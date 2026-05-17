package com.practice.firstapp.service;

import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.practice.firstapp.dto.PasswordResetReqDto;
import com.practice.firstapp.dto.UpdateUserProfile;
import com.practice.firstapp.dto.UserProfileDto;
import com.practice.firstapp.repo.UserRepo;
import com.practice.firstapp.vo.Users;

@Service
public class UserService {

    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Cacheable(key = "#userId", cacheNames = "user", unless = "#result == null", cacheManager = "cacheManager")
    public UserProfileDto getUser(Long userId) {
        System.out.println("Cache being set for User Profile");
        Users user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
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
        Users existingUser = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (updateReq.getFirstName() != null) {
            existingUser.setFirstName(updateReq.getFirstName());
        }
        if (updateReq.getLastName() != null) {
            existingUser.setLastName(updateReq.getLastName());
        }
        if (updateReq.getEmail() != null) {
            existingUser.setEmail(updateReq.getEmail());
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

}
