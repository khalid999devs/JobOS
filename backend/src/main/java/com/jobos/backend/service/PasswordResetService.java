package com.jobos.backend.service;

import com.jobos.backend.domain.user.PasswordResetToken;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.exception.*;
import com.jobos.backend.repository.PasswordResetTokenRepository;
import com.jobos.backend.repository.RefreshTokenRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.shared.dto.auth.ChangePasswordRequest;
import com.jobos.shared.dto.auth.ForgotPasswordRequest;
import com.jobos.shared.dto.auth.ResetPasswordRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int RATE_LIMIT_MINUTES = 2;

    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository resetTokenRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.resetTokenRepository = resetTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("No account exists with email: " + email));

        resetTokenRepository.findByEmailAndUsedAtIsNull(email).ifPresent(existing -> {
            Duration timeSinceCreation = Duration.between(existing.getCreatedAt(), LocalDateTime.now());
            if (timeSinceCreation.toMinutes() < RATE_LIMIT_MINUTES) {
                long remainingSeconds = RATE_LIMIT_MINUTES * 60 - timeSinceCreation.getSeconds();
                throw new RateLimitException(
                    "Please wait " + remainingSeconds + " seconds before requesting a new OTP"
                );
            }
        });

        resetTokenRepository.deleteByEmail(email);

        String otp = generateOtp();
        String otpHash = hashOtp(otp);

        PasswordResetToken token = new PasswordResetToken();
        token.setEmail(email);
        token.setOtpHash(otpHash);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        resetTokenRepository.save(token);

        emailService.sendOtpEmail(email, otp);
    }

    @Transactional(noRollbackFor = {InvalidTokenException.class, OtpAttemptsExceededException.class})
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        String otp = request.getOtp();

        PasswordResetToken token = resetTokenRepository.findByEmailAndUsedAtIsNull(email)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired OTP"));

        if (token.isUsed()) {
            throw new InvalidTokenException("OTP has already been used");
        }

        if (token.isExpired()) {
            throw new OtpExpiredException("OTP has expired. Please request a new one");
        }

        if (token.isMaxAttemptsReached()) {
            throw new OtpAttemptsExceededException("Maximum OTP attempts exceeded. Please request a new OTP");
        }

        String otpHash = hashOtp(otp);
        if (!token.getOtpHash().equals(otpHash)) {
            token.incrementAttempts();
            int remainingAttempts = token.getMaxAttempts() - token.getAttempts();
            resetTokenRepository.saveAndFlush(token);
            
            if (remainingAttempts > 0) {
                throw new InvalidTokenException("Invalid OTP. " + remainingAttempts + " attempts remaining");
            } else {
                throw new OtpAttemptsExceededException("Maximum OTP attempts exceeded. Please request a new OTP");
            }
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser_Id(user.getId());

        token.setUsedAt(LocalDateTime.now());
        resetTokenRepository.save(token);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteByUser_Id(userId);
    }

    private String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000); // Generate 6-digit number
        return String.valueOf(otp);
    }

    private String hashOtp(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
