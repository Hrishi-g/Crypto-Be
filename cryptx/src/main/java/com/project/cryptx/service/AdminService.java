package com.project.cryptx.service;
// package com.practice.firstapp.service;

// import java.util.Map;

// import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Service;

// import com.practice.firstapp.dto.UserUpdateReqDto;
// import com.practice.firstapp.repo.UserRepo;
// import com.practice.firstapp.vo.Users;

// @Service
// public class AdminService {

// private UserRepo userRepo;

// AdminService(UserRepo userRepo) {
// this.userRepo = userRepo;
// }

// public ResponseEntity<?> updateUserAsAdmin(Long userId, UserUpdateReqDto
// updateReq) {
// Users existingUser = userRepo.findById(userId).orElse(null);
// if (existingUser == null) {
// throw new RuntimeException("User not found");
// }

// if (updateReq.getFirstName() != null) {
// existingUser.setFirstName(updateReq.getFirstName());
// }
// if (updateReq.getLastName() != null) {
// existingUser.setLastName(updateReq.getLastName());
// }
// if (updateReq.getEmail() != null) {
// existingUser.setEmail(updateReq.getEmail());
// }
// if (updateReq.getDob() != null) {
// existingUser.setDob(updateReq.getDob());
// }
// if (updateReq.getRole() != null) {
// existingUser.setRole(updateReq.getRole().toUpperCase());
// }

// userRepo.save(existingUser);

// return ResponseEntity.ok(Map.of("message", "User updated successfully",
// "user", existingUser));
// }
// }
