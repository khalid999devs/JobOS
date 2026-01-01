package com.jobos.backend.repository;

import com.jobos.backend.domain.user.PosterProfile;
import com.jobos.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosterProfileRepository extends JpaRepository<PosterProfile, UUID> {
    Optional<PosterProfile> findByUser(User user);
}
