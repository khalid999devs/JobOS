package com.jobos.backend.service;

import com.jobos.backend.domain.user.RefreshToken;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.domain.user.UserRole;
import com.jobos.backend.domain.user.UserStatus;
import com.jobos.backend.exception.DuplicateResourceException;
import com.jobos.backend.exception.InvalidCredentialsException;
import com.jobos.backend.exception.InvalidTokenException;
import com.jobos.backend.exception.ResourceNotFoundException;
import com.jobos.backend.repository.RefreshTokenRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.backend.security.jwt.JwtUtil;
import com.jobos.shared.dto.auth.AuthResponse;
import com.jobos.shared.dto.auth.LoginRequest;
import com.jobos.shared.dto.auth.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        logger.info("Registration attempt for email: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            logger.warn("Registration failed - email already exists: {}", request.getEmail());
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be SEEKER or POSTER");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        user = userRepository.save(user);
        logger.info("User registered successfully: {} with role: {}", user.getId(), role);

        UUID sessionId = UUID.randomUUID();
        return generateAuthResponse(user, sessionId);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        logger.info("Login attempt for email: {}", request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    logger.warn("Login failed - user not found: {}", request.getEmail());
                    return new InvalidCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Login failed - invalid password for user: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            logger.warn("Login failed - account not active: {} status: {}", request.getEmail(), user.getStatus());
            throw new InvalidCredentialsException("Account is " + user.getStatus().name().toLowerCase());
        }

        logger.info("User logged in successfully: {}", user.getId());
        UUID sessionId = UUID.randomUUID();
        return generateAuthResponse(user, sessionId);
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenString) {
        String tokenHash = hashToken(refreshTokenString);
        
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));

        if (!refreshToken.isValid()) {
            throw new InvalidTokenException("Refresh token has been revoked or expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(
            user.getId(), 
            user.getEmail(), 
            user.getRole().name(),
            refreshToken.getSessionId()
        );

        AuthResponse response = new AuthResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(refreshTokenString);
        response.setUserId(user.getId().toString());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setProfileCompleted(user.getProfileCompleted());

        return response;
    }

    @Transactional
    public void logout(UUID sessionId) {
        logger.info("Logout for session: {}", sessionId);
        refreshTokenRepository.deleteBySessionId(sessionId);
    }

    private AuthResponse generateAuthResponse(User user, UUID sessionId) {
        String accessToken = jwtUtil.generateAccessToken(
            user.getId(), 
            user.getEmail(), 
            user.getRole().name(),
            sessionId
        );
        String refreshTokenString = jwtUtil.generateRefreshToken(user.getId(), sessionId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setSessionId(sessionId);
        refreshToken.setTokenHash(hashToken(refreshTokenString));
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(30));
        refreshTokenRepository.save(refreshToken);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenString);
        response.setUserId(user.getId().toString());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setProfileCompleted(user.getProfileCompleted());

        return response;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
