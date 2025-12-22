package com.jobos.backend.repository;

import com.jobos.backend.domain.user.SeekerPreferences;
import com.jobos.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeekerPreferencesRepository extends JpaRepository<SeekerPreferences, UUID> {
    Optional<SeekerPreferences> findByUser(User user);
}
