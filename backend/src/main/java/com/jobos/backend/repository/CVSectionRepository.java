package com.jobos.backend.repository;

import com.jobos.backend.domain.cv.CVSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CVSectionRepository extends JpaRepository<CVSection, UUID> {
}
