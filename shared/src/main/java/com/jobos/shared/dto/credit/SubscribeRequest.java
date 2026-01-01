package com.jobos.shared.dto.credit;

import jakarta.validation.constraints.NotNull;

public class SubscribeRequest {
    @NotNull
    private String planId;

    @NotNull
    private String billingCycle; // MONTHLY or YEARLY

    private String paymentMethod;
    private String paymentToken;

    // Getters and Setters
    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(String billingCycle) {
        this.billingCycle = billingCycle;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentToken() {
        return paymentToken;
    }

    public void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }
}
