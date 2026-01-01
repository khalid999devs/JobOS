package com.jobos.backend.repository;

import com.jobos.backend.domain.cv.CVTemplate;
import com.jobos.backend.domain.cv.UserTemplateUnlock;
import com.jobos.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTemplateUnlockRepository extends JpaRepository<UserTemplateUnlock, UUID> {
    
    Optional<UserTemplateUnlock> findByUserAndTemplate(User user, CVTemplate template);
    
    List<UserTemplateUnlock> findByUser(User user);
    
    boolean existsByUserAndTemplate(User user, CVTemplate template);
}
