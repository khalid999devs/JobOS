package com.jobos.backend.controller;

import com.jobos.backend.security.jwt.JwtUtil;
import com.jobos.backend.service.AuthService;
import com.jobos.shared.dto.auth.AuthResponse;
import com.jobos.shared.dto.auth.LoginRequest;
import com.jobos.shared.dto.auth.RefreshRequest;
import com.jobos.shared.dto.auth.RegisterRequest;
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

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
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
}
