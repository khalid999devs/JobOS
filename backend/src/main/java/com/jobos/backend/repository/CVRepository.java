package com.jobos.backend.repository;

import com.jobos.backend.domain.cv.CV;
import com.jobos.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CVRepository extends JpaRepository<CV, UUID> {
    
    Page<CV> findByUser(User user, Pageable pageable);
    
    long countByUser(User user);
    
    Optional<CV> findByUserAndIsDefaultTrue(User user);
    
    @Modifying
    @Query("UPDATE CV c SET c.isDefault = false WHERE c.user = :user")
    void clearDefaultForUser(@Param("user") User user);
}
