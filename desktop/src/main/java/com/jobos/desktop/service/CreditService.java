package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.shared.dto.common.ApiResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CreditService {
    
    private final ApiClient apiClient = ApiClient.getInstance();
    
    public CreditBalance getBalance() {
        try {
            ApiResponse<CreditBalance> response = apiClient.get(
                "/api/credits/balance",
                new TypeReference<ApiResponse<CreditBalance>>() {}
            ).get();
            
            if (response != null && response.isSuccess() && response.getResult() != null) {
                return response.getResult();
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }
        return new CreditBalance();
    }

    public CompletableFuture<Map<String, Object>> getTransactions(int page, int size) {
        return apiClient.get("/api/credits/transactions?page=" + page + "&size=" + size, 
            new TypeReference<Map<String, Object>>() {});
    }

    public CompletableFuture<ApiResponse<CreditBalance>> purchaseCredits(int amount, String paymentMethod) {
        Map<String, Object> request = Map.of("amount", amount, "paymentMethod", paymentMethod);
        return apiClient.post("/api/credits/purchase", request, new TypeReference<ApiResponse<CreditBalance>>() {});
    }

    public CompletableFuture<Map<String, Object>> getPlans() {
        return apiClient.get("/api/plans", new TypeReference<Map<String, Object>>() {});
    }

    public CompletableFuture<ApiResponse<Object>> subscribeToPlan(String planId) {
        Map<String, Object> request = Map.of(
            "planId", planId,
            "billingCycle", "MONTHLY",
            "paymentMethod", "SIMULATED"
        );
        return apiClient.post("/api/plans/subscribe", request, 
            new TypeReference<ApiResponse<Object>>() {});
    }
    
    public static class CreditBalance {
        private Integer credits = 0;
        private Integer usedCredits = 0;
        private String plan = "FREE";
        private Integer balance = 0;
        
        public Integer getCredits() {
            if (balance != null && balance > 0) return balance;
            return credits != null ? credits : 0;
        }
        
        public void setCredits(Integer credits) {
            this.credits = credits;
        }
        
        public Integer getUsedCredits() {
            return usedCredits != null ? usedCredits : 0;
        }
        
        public void setUsedCredits(Integer usedCredits) {
            this.usedCredits = usedCredits;
        }
        
        public String getPlan() {
            return plan != null && !plan.isEmpty() ? plan : "FREE";
        }
        
        public void setPlan(String plan) {
            this.plan = plan;
        }
        
        public Integer getBalance() {
            return balance;
        }
        
        public void setBalance(Integer balance) {
            this.balance = balance;
        }
    }
}
