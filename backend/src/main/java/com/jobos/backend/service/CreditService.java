package com.jobos.backend.service;

import com.jobos.backend.domain.credit.*;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.repository.*;
import com.jobos.shared.dto.credit.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CreditService {

    private final CreditBalanceRepository creditBalanceRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;

    public CreditService(CreditBalanceRepository creditBalanceRepository,
                        CreditTransactionRepository creditTransactionRepository,
                        SubscriptionPlanRepository subscriptionPlanRepository,
                        UserSubscriptionRepository userSubscriptionRepository,
                        UserRepository userRepository) {
        this.creditBalanceRepository = creditBalanceRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CreditBalanceResponse getBalance(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CreditBalance balance = creditBalanceRepository.findByUser(user)
                .orElseGet(() -> initializeBalance(user));

        return mapToBalanceResponse(balance);
    }

    @Transactional
    public CreditBalanceResponse purchaseCredits(UUID userId, CreditPurchaseRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        CreditBalance balance = creditBalanceRepository.findByUser(user)
                .orElseGet(() -> initializeBalance(user));

        balance.setBalance(balance.getBalance() + request.getAmount());
        balance = creditBalanceRepository.save(balance);

        // Record transaction
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setTransactionType(TransactionType.PURCHASE);
        transaction.setAmount(request.getAmount());
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setDescription("Credit purchase");
        creditTransactionRepository.save(transaction);

        return mapToBalanceResponse(balance);
    }

    @Transactional
    public boolean deductCredits(User user, Integer amount, String description) {
        CreditBalance balance = creditBalanceRepository.findByUser(user)
                .orElseGet(() -> initializeBalance(user));

        if (balance.getBalance() < amount) {
            return false;
        }

        balance.setBalance(balance.getBalance() - amount);
        balance = creditBalanceRepository.save(balance);

        // Record transaction
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setTransactionType(TransactionType.DEDUCTION);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setDescription(description);
        creditTransactionRepository.save(transaction);

        return true;
    }

    @Transactional(readOnly = true)
    public Page<CreditTransactionResponse> getTransactions(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Page<CreditTransaction> transactions = creditTransactionRepository.findByUser(user, pageable);
        return transactions.map(this::mapToTransactionResponse);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAllPlans(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<SubscriptionPlan> plans = subscriptionPlanRepository.findByIsActiveTrue();
        UserSubscription currentSub = userSubscriptionRepository
                .findByUserAndIsActiveTrueAndEndDateAfter(user, Instant.now())
                .orElse(null);

        return plans.stream()
                .map(plan -> mapToPlanResponse(plan, currentSub != null && 
                        currentSub.getPlan().getId().equals(plan.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public SubscriptionPlanResponse subscribe(UUID userId, SubscribeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UUID planId = UUID.fromString(request.getPlanId());
        SubscriptionPlan plan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));

        // Deactivate current subscription
        userSubscriptionRepository.findByUserAndIsActiveTrueAndEndDateAfter(user, Instant.now())
                .ifPresent(sub -> {
                    sub.setIsActive(false);
                    userSubscriptionRepository.save(sub);
                });

        // Create new subscription
        UserSubscription subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setStartDate(Instant.now());
        
        if ("YEARLY".equalsIgnoreCase(request.getBillingCycle())) {
            subscription.setEndDate(Instant.now().plus(365, ChronoUnit.DAYS));
        } else {
            subscription.setEndDate(Instant.now().plus(30, ChronoUnit.DAYS));
        }
        
        subscription.setIsActive(true);
        subscription.setAutoRenew(true);
        userSubscriptionRepository.save(subscription);

        // Add monthly credits
        CreditBalance balance = creditBalanceRepository.findByUser(user)
                .orElseGet(() -> initializeBalance(user));
        balance.setBalance(balance.getBalance() + plan.getMonthlyCredits());
        creditBalanceRepository.save(balance);

        // Record credit bonus transaction
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUser(user);
        transaction.setTransactionType(TransactionType.BONUS);
        transaction.setAmount(plan.getMonthlyCredits());
        transaction.setBalanceAfter(balance.getBalance());
        transaction.setDescription("Monthly credits from " + plan.getName() + " subscription");
        creditTransactionRepository.save(transaction);

        return mapToPlanResponse(plan, true);
    }

    private CreditBalance initializeBalance(User user) {
        CreditBalance balance = new CreditBalance();
        balance.setUser(user);
        balance.setBalance(0);
        return creditBalanceRepository.save(balance);
    }

    private CreditBalanceResponse mapToBalanceResponse(CreditBalance balance) {
        CreditBalanceResponse response = new CreditBalanceResponse();
        response.setUserId(balance.getUser().getId().toString());
        response.setBalance(balance.getBalance());
        response.setCreatedAt(balance.getCreatedAt().toString());
        response.setUpdatedAt(balance.getUpdatedAt().toString());
        return response;
    }

    private CreditTransactionResponse mapToTransactionResponse(CreditTransaction transaction) {
        CreditTransactionResponse response = new CreditTransactionResponse();
        response.setId(transaction.getId().toString());
        response.setTransactionType(transaction.getTransactionType().name());
        response.setAmount(transaction.getAmount());
        response.setBalanceAfter(transaction.getBalanceAfter());
        response.setDescription(transaction.getDescription());
        response.setCreatedAt(transaction.getCreatedAt().toString());
        return response;
    }

    private SubscriptionPlanResponse mapToPlanResponse(SubscriptionPlan plan, boolean isCurrentPlan) {
        SubscriptionPlanResponse response = new SubscriptionPlanResponse();
        response.setId(plan.getId().toString());
        response.setPlanType(plan.getPlanType().name());
        response.setName(plan.getName());
        response.setDescription(plan.getDescription());
        response.setMonthlyPrice(plan.getMonthlyPrice());
        response.setYearlyPrice(plan.getYearlyPrice());
        response.setMonthlyCredits(plan.getMonthlyCredits());
        response.setMaxCVs(plan.getMaxCVs());
        response.setMaxJobApplications(plan.getMaxJobApplications());
        response.setHasAIAssistance(plan.getHasAIAssistance());
        response.setHasPrioritySupport(plan.getHasPrioritySupport());
        response.setHasPremiumTemplates(plan.getHasPremiumTemplates());
        response.setIsCurrentPlan(isCurrentPlan);
        return response;
    }
}
