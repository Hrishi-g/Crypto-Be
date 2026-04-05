package com.practice.firstapp.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.practice.firstapp.dto.UserProfileDto;
import com.practice.firstapp.repo.UserRepo;
import com.practice.firstapp.vo.Users;

@Service
public class UserService {
    private UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public ResponseEntity<UserProfileDto> getUser(String username) {
        Users user = userRepo.findByUsername(username).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        UserProfileDto userProfileDto = new UserProfileDto();
        userProfileDto.setUsername(user.getUsername());
        userProfileDto.setTotalAmount(user.getWallet());
        userProfileDto.setFirstName(user.getFirstName());
        userProfileDto.setLastName(user.getLastName());
        userProfileDto.setEmail(user.getEmail());
        userProfileDto.setDob(user.getDob());
        return ResponseEntity.ok(userProfileDto);
    }
}
