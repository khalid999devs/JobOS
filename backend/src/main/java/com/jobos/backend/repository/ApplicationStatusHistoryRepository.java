package com.jobos.backend.repository;

import com.jobos.backend.domain.application.Application;
import com.jobos.backend.domain.application.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {
    
    List<ApplicationStatusHistory> findByApplicationOrderByChangedAtDesc(Application application);
}
