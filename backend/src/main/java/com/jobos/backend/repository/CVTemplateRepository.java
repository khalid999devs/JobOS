package com.jobos.backend.repository;

import com.jobos.backend.domain.cv.CVTemplate;
import com.jobos.backend.domain.cv.TemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CVTemplateRepository extends JpaRepository<CVTemplate, UUID> {
    
    List<CVTemplate> findByIsActiveTrue();
    
    List<CVTemplate> findByIsActiveTrueAndCategory(TemplateCategory category);
    
    List<CVTemplate> findByIsActiveTrueAndIsPremiumFalse();
}
