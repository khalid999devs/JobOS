package com.jobos.backend.repository;

import com.jobos.backend.domain.application.Application;
import com.jobos.backend.domain.application.ApplicationStatus;
import com.jobos.backend.domain.job.JobPost;
import com.jobos.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    
    @EntityGraph(attributePaths = {"jobPost", "seeker"})
    Page<Application> findBySeeker(User seeker, Pageable pageable);
    
    @EntityGraph(attributePaths = {"seeker"})
    Page<Application> findByJobPost(JobPost jobPost, Pageable pageable);
    
    @EntityGraph(attributePaths = {"seeker"})
    Page<Application> findByJobPostAndStatus(JobPost jobPost, ApplicationStatus status, Pageable pageable);
    
    boolean existsBySeekerAndJobPost(User seeker, JobPost jobPost);
    
    long countByJobPost(JobPost jobPost);
    
    @EntityGraph(attributePaths = {"jobPost", "jobPost.poster", "seeker"})
    Optional<Application> findById(UUID id);
}
