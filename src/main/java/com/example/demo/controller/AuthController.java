package com.example.demo.controller;

import com.example.demo.dto.TokenPairResponse;
import com.example.demo.entity.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService,
                          JwtTokenProvider jwtTokenProvider,
                          PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            String role = (request.getRole() == null || request.getRole().trim().isEmpty())
                    ? "USER"
                    : request.getRole().trim().toUpperCase();

            if ("ADMIN".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Ошибка регистрации", "ADMIN может создавать только ADMIN"));
            }

            if (!"USER".equals(role) && !"TEACHER".equals(role)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Ошибка регистрации", "role должен быть USER или TEACHER"));
            }

            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());
            user.setRole(role);

            User createdUser = userService.createUser(user);
            createdUser.setPassword(null);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Ошибка валидации пароля", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Ошибка регистрации", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка сервера", e.getMessage()));
        }
    }

    /**
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.findByUsername(request.getUsername());

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Ошибка входа", "Неверные учетные данные"));
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Ошибка входа", "Неверные учетные данные"));
            }

            String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

            TokenPairResponse response = new TokenPairResponse(
                    accessToken,
                    refreshToken,
                    jwtTokenProvider.getAccessTokenExpiration()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка сервера", e.getMessage()));
        }
    }

    /**
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            if (request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Ошибка", "Refresh token is required"));
            }

            if (!jwtTokenProvider.validateRefreshToken(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Ошибка", "Invalid or expired refresh token"));
            }

            String username = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());
            User user = userService.findByUsername(username);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("Ошибка", "User not found"));
            }

            String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUsername(), user.getEmail());
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

            TokenPairResponse response = new TokenPairResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtTokenProvider.getAccessTokenExpiration()
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Ошибка", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Ошибка сервера", e.getMessage()));
        }
    }

    // ===== Inner DTOs =====

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class ErrorResponse {
        private String error;
        private String message;

        public ErrorResponse() {}
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
