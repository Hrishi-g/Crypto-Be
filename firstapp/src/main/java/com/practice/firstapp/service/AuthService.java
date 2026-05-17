package com.practice.firstapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.practice.firstapp.config.Utility;
import com.practice.firstapp.dto.AuthDto;
import com.practice.firstapp.dto.LoginReqDto;
import com.practice.firstapp.dto.SignUpReqDto;
import com.practice.firstapp.dto.UserProfileDto;
import com.practice.firstapp.repo.ResetPassTokenRepo;
// import com.practice.firstapp.dto.UserUpdateReqDto;
import com.practice.firstapp.repo.UserRepo;
import com.practice.firstapp.security.JwtUtils;
import com.practice.firstapp.vo.Refresh_token;
import com.practice.firstapp.vo.ResetPassToken;
import com.practice.firstapp.vo.Users;
import com.practice.firstapp.vo.Wallet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Service
public class AuthService {

    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private Utility utility;
    private UserService userService;
    private ResetPassTokenRepo resetPassTokenRepo;
    private EmailService emailService;

    AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
            JwtUtils jwtUtils, Utility utility, UserService userService, ResetPassTokenRepo resetPassTokenRepo,
            EmailService emailService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.utility = utility;
        this.userService = userService;
        this.resetPassTokenRepo = resetPassTokenRepo;
        this.emailService = emailService;
    }

    private String generateUsername(String name) {
        String firstName = name.trim().split("\\s+")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        String uniquePart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return firstName + "_" + uniquePart;
    }

    @Transactional
    public ResponseEntity<?> SignUp(@Validated SignUpReqDto signUpReqDto) {
        Users existingUser = userRepo.findByEmail(signUpReqDto.getEmail()).orElse(null);

        if (existingUser != null) {
            throw new RuntimeException("User already exists, Use different email or signIn with your google account");
        }
        Users newUser = new Users();
        newUser.setFirstName(signUpReqDto.getFirstName());
        newUser.setLastName(signUpReqDto.getLastName());
        newUser.setPassword(passwordEncoder.encode(signUpReqDto.getPassword()));
        newUser.setEmail(signUpReqDto.getEmail());
        newUser.setUsername(generateUsername(signUpReqDto.getFirstName()));
        newUser.setDob(signUpReqDto.getDob());
        newUser.setRole("USER");

        String firstNameStr = signUpReqDto.getFirstName() != null ? signUpReqDto.getFirstName() : "user";
        String cleanFirstName = firstNameStr.trim().split("\\s+")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        String uniquePart = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        newUser.setUsername(cleanFirstName + "_" + uniquePart);

        Wallet wallet = new Wallet();
        wallet.setUser(newUser);
        wallet.setBalance(BigDecimal.ZERO);

        newUser.setWallet(wallet);

        userRepo.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Signup successful"));
    }

    public Users getUserById(Long id) {
        return userRepo.findById(id).orElse(null);
    }

    public ResponseEntity<List<Users>> getAllUsers() {
        return ResponseEntity.ok(userRepo.findAll());
    }

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

    public ResponseEntity<?> LogIn(LoginReqDto loginReq, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginReq.getIdentifier(), loginReq.getPassword()));

        Users user = (Users) auth.getPrincipal();

        utility.addJwtCookie(response, jwtUtils.generateAccessToken(user));

        Refresh_token refreshToken = utility.generateRefreshToken(user);

        utility.addRefreshCookie(response, refreshToken.getToken());

        return ResponseEntity.status(HttpStatus.OK)
                .body(Map.of("message", "Login successful",
                        "username", user.getUsername() != null ? user.getUsername() : "",
                        "email", user.getEmail() != null ? user.getEmail() : ""));
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

    public ResponseEntity<?> PreCheck(AuthDto authUser) {
        if (authUser != null) {
            UserProfileDto checkedUser = userService.getUser(authUser.getId());
            if (checkedUser != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("authenticated", true);
                response.put("id", authUser.getId());
                response.put("role", authUser.getRole() != null ? authUser.getRole() : "USER");
                response.put("username", checkedUser.getUsername());
                response.put("hasPassword", checkedUser.isHasPassword());
                response.put("totalAmount", checkedUser.getTotalAmount());
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "id", authUser.getId(),
                    "role", authUser.getRole() != null ? authUser.getRole() : "USER"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    public ResponseEntity<?> sendResetLink(String email) {
        Users user = userRepo.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "User not found"));
        }
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);
        ResetPassToken resetPassToken = new ResetPassToken();
        resetPassToken.setToken(token);
        resetPassToken.setUser(user);
        resetPassToken.setExpiryTime(expiryTime);
        resetPassToken.setIsUsed(false);
        resetPassToken.setCrtd_dt(LocalDateTime.now());
        resetPassTokenRepo.save(resetPassToken);

        emailService.sendResetMail(user.getEmail(), token);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Password reset link sent to email"));
    }

    public ResponseEntity<?> resetPassword(String token, String password) {
        ResetPassToken resetPassToken = resetPassTokenRepo.findByToken(token).orElse(null);
        if (resetPassToken == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Invalid token"));
        }
        if (Boolean.TRUE.equals(resetPassToken.getIsUsed())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Token already used"));
        }
        if (resetPassToken.getExpiryTime().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Token expired"));
        }

        Users user = resetPassToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepo.save(user);
        resetPassToken.setIsUsed(true);
        resetPassTokenRepo.save(resetPassToken);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "Password reset successful"));
    }
}
