package com.jobos.backend.repository;

import com.jobos.backend.domain.user.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    Optional<RefreshToken> findBySessionId(UUID sessionId);
    
    @Modifying
    @Transactional
    void deleteBySessionId(UUID sessionId);
    
    @Modifying
    @Transactional
    void deleteByUser_Id(UUID userId);
    
    long countByUser_Id(UUID userId);
}
