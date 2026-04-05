package com.practice.firstapp.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.practice.firstapp.config.Utility;
import com.practice.firstapp.dto.LoginReqDto;
import com.practice.firstapp.dto.SignUpReqDto;
import com.practice.firstapp.dto.UserUpdateReqDto;
import com.practice.firstapp.repo.UserRepo;
import com.practice.firstapp.security.JwtUtils;
import com.practice.firstapp.vo.Refresh_token;
import com.practice.firstapp.vo.Users;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {

    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private Utility utility;

    AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
            JwtUtils jwtUtils, Utility utility) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.utility = utility;
    }

    public ResponseEntity<?> SignUp(SignUpReqDto signUpReqDto) {
        Users existingUser = userRepo.findByUsername(signUpReqDto.getUsername()).orElse(null);

        if (existingUser != null) {
            throw new RuntimeException("UserName already exists");
        }
        Users newUser = new Users();
        newUser.setFirstName(signUpReqDto.getFirstName());
        newUser.setLastName(signUpReqDto.getLastName());
        newUser.setUsername(signUpReqDto.getUsername());
        newUser.setPassword(passwordEncoder.encode(signUpReqDto.getPassword()));
        newUser.setEmail(signUpReqDto.getEmail());
        newUser.setDob(signUpReqDto.getDob());
        newUser.setRole("USER");
        userRepo.save(newUser);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Signup successful"));
    }

    public Users getUserById(Long id) {
        return userRepo.findById(id).orElse(null);
    }

    public ResponseEntity<List<Users>> getAllUsers() {
        return ResponseEntity.ok(userRepo.findAll());
    }

    public ResponseEntity<?> updateUserAsAdmin(Long userId, UserUpdateReqDto updateReq) {
        Users existingUser = userRepo.findById(userId).orElse(null);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }

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
        if (updateReq.getRole() != null) {
            existingUser.setRole(updateReq.getRole().toUpperCase());
        }

        userRepo.save(existingUser);

        return ResponseEntity.ok(Map.of("message", "User updated successfully", "user", existingUser));
    }

    public ResponseEntity<?> LogIn(LoginReqDto loginReq, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginReq.getUsername(), loginReq.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        Users user = (Users) auth.getPrincipal();
        utility.addJwtCookie(response, jwtUtils.generateAccessToken(user));
        Refresh_token refreshToken = utility.generateRefreshToken(loginReq.getUsername());
        utility.addRefreshCookie(response, refreshToken.getToken());
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Login successful"));
    }

    public ResponseEntity<?> LogOut(HttpServletRequest request, HttpServletResponse response) {
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
