package com.jobos.backend.domain.credit;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private PlanType planType;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal yearlyPrice;

    @Column(nullable = false)
    private Integer monthlyCredits;

    @Column(nullable = false)
    private Integer maxCVs;

    @Column(nullable = false)
    private Integer maxJobApplications;

    @Column(nullable = false)
    private Boolean hasAIAssistance;

    @Column(nullable = false)
    private Boolean hasPrioritySupport;

    @Column(nullable = false)
    private Boolean hasPremiumTemplates;

    @Column(nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
