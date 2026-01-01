package com.jobos.backend.controller;

import com.jobos.backend.security.jwt.JwtUtil;
import com.jobos.backend.service.AuthService;
import com.jobos.backend.service.PasswordResetService;
import com.jobos.shared.dto.auth.*;
import com.jobos.shared.dto.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        // Extract token from "Bearer " prefix
        String token = authHeader.substring(7);
        
        // Validate token and extract sessionId
        jwtUtil.validateToken(token);
        UUID sessionId = jwtUtil.getSessionIdFromToken(token);
        
        // Revoke only this specific session
        authService.logout(sessionId);
        
        return ResponseEntity.ok(
            ApiResponse.success(null, "Logged out successfully from this device")
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request);
        return ResponseEntity.ok(
            ApiResponse.success(null, "A 6-digit OTP has been sent to your email. Please check your inbox and use it to reset your password. The OTP will expire in 10 minutes.")
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(
            ApiResponse.success(null, "Password reset successfully")
        );
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        // Extract token and get userId
        String token = authHeader.substring(7);
        jwtUtil.validateToken(token);
        UUID userId = jwtUtil.getUserIdFromToken(token);
        
        passwordResetService.changePassword(userId, request);
        return ResponseEntity.ok(
            ApiResponse.success(null, "Password changed successfully. All active sessions have been logged out for security. Please login again with your new password.")
        );
    }
}
