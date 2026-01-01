package com.jobos.shared.dto.credit;

import java.math.BigDecimal;

public class SubscriptionPlanResponse {
    private String id;
    private String planType;
    private String name;
    private String description;
    private BigDecimal monthlyPrice;
    private BigDecimal yearlyPrice;
    private Integer monthlyCredits;
    private Integer maxCVs;
    private Integer maxJobApplications;
    private Boolean hasAIAssistance;
    private Boolean hasPrioritySupport;
    private Boolean hasPremiumTemplates;
    private Boolean isCurrentPlan;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice;
    }

    public BigDecimal getYearlyPrice() {
        return yearlyPrice;
    }

    public void setYearlyPrice(BigDecimal yearlyPrice) {
        this.yearlyPrice = yearlyPrice;
    }

    public Integer getMonthlyCredits() {
        return monthlyCredits;
    }

    public void setMonthlyCredits(Integer monthlyCredits) {
        this.monthlyCredits = monthlyCredits;
    }

    public Integer getMaxCVs() {
        return maxCVs;
    }

    public void setMaxCVs(Integer maxCVs) {
        this.maxCVs = maxCVs;
    }

    public Integer getMaxJobApplications() {
        return maxJobApplications;
    }

    public void setMaxJobApplications(Integer maxJobApplications) {
        this.maxJobApplications = maxJobApplications;
    }

    public Boolean getHasAIAssistance() {
        return hasAIAssistance;
    }

    public void setHasAIAssistance(Boolean hasAIAssistance) {
        this.hasAIAssistance = hasAIAssistance;
    }

    public Boolean getHasPrioritySupport() {
        return hasPrioritySupport;
    }

    public void setHasPrioritySupport(Boolean hasPrioritySupport) {
        this.hasPrioritySupport = hasPrioritySupport;
    }

    public Boolean getHasPremiumTemplates() {
        return hasPremiumTemplates;
    }

    public void setHasPremiumTemplates(Boolean hasPremiumTemplates) {
        this.hasPremiumTemplates = hasPremiumTemplates;
    }

    public Boolean getIsCurrentPlan() {
        return isCurrentPlan;
    }

    public void setIsCurrentPlan(Boolean isCurrentPlan) {
        this.isCurrentPlan = isCurrentPlan;
    }
}
