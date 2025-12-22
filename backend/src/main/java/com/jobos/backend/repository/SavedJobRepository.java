package com.jobos.backend.repository;

import com.jobos.backend.domain.job.JobPost;
import com.jobos.backend.domain.job.SavedJob;
import com.jobos.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {
    
    @EntityGraph(attributePaths = {"jobPost", "jobPost.poster"})
    Page<SavedJob> findByUser(User user, Pageable pageable);
    
    boolean existsByUserAndJobPost(User user, JobPost jobPost);
    
    Optional<SavedJob> findByUserAndJobPost(User user, JobPost jobPost);
    
    @Query("SELECT COUNT(s) FROM SavedJob s WHERE s.user = :user")
    long countByUser(User user);
}
