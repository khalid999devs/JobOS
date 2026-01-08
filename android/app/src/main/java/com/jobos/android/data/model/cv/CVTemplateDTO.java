package com.jobos.android.data.model.cv;

/**
 * DTO for CV template data from the backend.
 */
public class CVTemplateDTO {

    private String id;
    private String name;
    private String description;
    private String previewImageUrl;
    private Boolean isPremium;
    private Integer creditCost;
    private String category;
    private Boolean isUnlocked;
    private String sectionsConfig;
    private String styleConfig;
    private Integer sectionCount;
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getIsUnlocked() {
        return isUnlocked;
    }

    public void setIsUnlocked(Boolean isUnlocked) {
        this.isUnlocked = isUnlocked;
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

    public Integer getSectionCount() {
        return sectionCount;
    }

    public void setSectionCount(Integer sectionCount) {
        this.sectionCount = sectionCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
