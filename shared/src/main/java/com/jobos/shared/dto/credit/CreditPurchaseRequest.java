package com.jobos.shared.dto.credit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreditPurchaseRequest {
    @NotNull
    @Min(1)
    private Integer amount;

    private String paymentMethod;
    private String paymentToken;

    // Getters and Setters
    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
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
