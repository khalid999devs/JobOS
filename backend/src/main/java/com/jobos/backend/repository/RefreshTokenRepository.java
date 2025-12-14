package com.jobos.backend.repository;

import com.jobos.backend.domain.user.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    
    Optional<RefreshToken> findBySessionId(UUID sessionId);
    
    void deleteBySessionId(UUID sessionId);
    
    void deleteByUser_Id(UUID userId);
    
    long countByUser_Id(UUID userId);
}
