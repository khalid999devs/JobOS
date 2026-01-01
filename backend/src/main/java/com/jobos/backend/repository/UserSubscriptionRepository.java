package com.jobos.backend.repository;

import com.jobos.backend.domain.credit.UserSubscription;
import com.jobos.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {
    Optional<UserSubscription> findByUserAndIsActiveTrueAndEndDateAfter(User user, Instant now);
}
