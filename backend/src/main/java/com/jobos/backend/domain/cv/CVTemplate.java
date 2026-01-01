package com.jobos.backend.domain.cv;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cv_template")
public class CVTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "preview_image_url", length = 500)
    private String previewImageUrl;

    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium = false;

    @Column(name = "credit_cost", nullable = false)
    private Integer creditCost = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateCategory category;

    // JSON configuration defining sections structure for this template
    // Format: [{"sectionType": "PERSONAL_INFO", "title": "Personal Information", "orderIndex": 0, "defaultContent": {...}}, ...]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections_config", columnDefinition = "jsonb")
    private String sectionsConfig;

    // Styling configuration for the template (colors, fonts, layout)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "style_config", columnDefinition = "jsonb")
    private String styleConfig;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getPreviewImageUrl() {
        return previewImageUrl;
    }

    public void setPreviewImageUrl(String previewImageUrl) {
        this.previewImageUrl = previewImageUrl;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Boolean isPremium) {
        this.isPremium = isPremium;
    }

    public Integer getCreditCost() {
        return creditCost;
    }

    public void setCreditCost(Integer creditCost) {
        this.creditCost = creditCost;
    }

    public TemplateCategory getCategory() {
        return category;
    }

    public void setCategory(TemplateCategory category) {
        this.category = category;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getSectionsConfig() {
        return sectionsConfig;
    }

    public void setSectionsConfig(String sectionsConfig) {
        this.sectionsConfig = sectionsConfig;
    }

    public String getStyleConfig() {
        return styleConfig;
    }

    public void setStyleConfig(String styleConfig) {
        this.styleConfig = styleConfig;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
