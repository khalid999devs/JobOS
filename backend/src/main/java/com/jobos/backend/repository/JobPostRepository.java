package com.jobos.backend.repository;

import com.jobos.backend.domain.job.JobPost;
import com.jobos.backend.domain.job.JobStatus;
import com.jobos.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {
    
    Page<JobPost> findByPoster(User poster, Pageable pageable);
    
    Page<JobPost> findByPosterAndStatus(User poster, JobStatus status, Pageable pageable);
    
    Optional<JobPost> findByIdAndPoster(UUID id, User poster);
}
