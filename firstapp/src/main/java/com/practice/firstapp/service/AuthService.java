package com.practice.firstapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import com.practice.firstapp.config.SingletonLogger;
import com.practice.firstapp.exception.UserAlreadyExistsException;
import com.practice.firstapp.exception.InvalidTokenException;

import com.practice.firstapp.config.Utility;
import com.practice.firstapp.dto.AuthDto;
import com.practice.firstapp.dto.LoginReqDto;
import com.practice.firstapp.dto.SignUpReqDto;
import com.practice.firstapp.dto.UserProfileDto;
import com.practice.firstapp.repo.RefreshTokenRepo;
import com.practice.firstapp.repo.ResetPassTokenRepo;
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
    private static final SingletonLogger log = SingletonLogger.log();
    private UserRepo userRepo;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtUtils jwtUtils;
    private Utility utility;
    private UserService userService;
    private ResetPassTokenRepo resetPassTokenRepo;
    private EmailService emailService;
    private final RefreshTokenRepo refreshTokenRepo;

    AuthService(UserRepo userRepo, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
            JwtUtils jwtUtils, Utility utility, UserService userService, ResetPassTokenRepo resetPassTokenRepo,
            EmailService emailService, RefreshTokenRepo refreshTokenRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.utility = utility;
        this.userService = userService;
        this.resetPassTokenRepo = resetPassTokenRepo;
        this.emailService = emailService;
        this.refreshTokenRepo = refreshTokenRepo;
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
            throw new UserAlreadyExistsException(
                    "User already exists, Use different email or signIn with your google account");
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

    public ResponseEntity<?> preCheck(AuthDto authUser) {
        if (authUser != null) {
            log.info("preCheck user id: {}", authUser.getId());
            UserProfileDto checkedUser = userService.getUser(authUser.getId());
            log.info("preCheck checkedUser: {}", checkedUser);
            if (checkedUser != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("authenticated", true);
                response.put("navAvatar",
                        getFirstLetterUppercase(checkedUser.getUsername()));
                response.put("hasPassword",
                        checkedUser.isHasPassword());
                response.put("totalAmount", checkedUser.getTotalAmountPlain());
                return ResponseEntity.ok(response);
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "authenticated", false,
                        "message", "Unauthorized"));
    }

    public String getFirstLetterUppercase(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "U";
        }
        return String.valueOf(username.trim().charAt(0)).toUpperCase();
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

    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenStr = Arrays.stream(request.getCookies() == null ? new Cookie[0] : request.getCookies())
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new InvalidTokenException("Refresh token missing"));

        Refresh_token refreshToken = refreshTokenRepo.findByToken(refreshTokenStr)
                .map(utility::validateRefreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        Users user = refreshToken.getUser();
        String newAccessToken = jwtUtils.generateAccessToken(user);

        utility.addJwtCookie(response, newAccessToken);

        return ResponseEntity.ok(java.util.Map.of("message", "Token refreshed successfully"));
    }
}
