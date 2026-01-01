package com.jobos.backend.repository;

import com.jobos.backend.domain.credit.CreditBalance;
import com.jobos.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditBalanceRepository extends JpaRepository<CreditBalance, UUID> {
    Optional<CreditBalance> findByUser(User user);
}
