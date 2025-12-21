package com.jobos.backend.repository;

import com.jobos.backend.domain.user.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByEmailAndUsedAtIsNull(String email);
    
    void deleteByEmail(String email);
}
