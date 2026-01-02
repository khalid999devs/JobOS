package com.jobos.android.data.model.cv;

import java.util.List;

/**
 * DTO for CV data that supports both:
 * - Backend CVResponse structure (sections-based)
 * - Simplified fields used by Android UI
 * 
 * The backend stores CV data in sections, but the Android UI displays
 * flattened fields like fullName, email, etc. The getter methods provide
 * fallbacks to extract data from sections when direct fields are not set.
 */
public class CVDTO {

    private String id;
    private String title;
    private String templateId;
    private String templateName;
    private Boolean isDefault;
    private String visibility;
    private Integer sectionCount;
    private String createdAt;
    private String updatedAt;
    
    // Sections from backend CVResponse
    private List<CVSection> sections;
    
    // Simplified fields used by Android UI (populated from sections or directly)
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String summary;
    private List<String> skills;
    private List<String> education;
    private List<String> experience;
    private String linkedinUrl;
    private String portfolioUrl;

    public static class CVSection {
        private String id;
        private String sectionType;
        private String title;
        private String content;
        private Integer orderIndex;
        private Boolean isVisible;
        private String createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSectionType() { return sectionType; }
        public void setSectionType(String sectionType) { this.sectionType = sectionType; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Integer getOrderIndex() { return orderIndex; }
        public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
        public Boolean getIsVisible() { return isVisible; }
        public void setIsVisible(Boolean isVisible) { this.isVisible = isVisible; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return title;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
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

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CVSection> getSections() {
        return sections;
    }

    public void setSections(List<CVSection> sections) {
        this.sections = sections;
    }

    // Helper to get section content by type
    private String getSectionContent(String sectionType) {
        if (sections == null) return null;
        for (CVSection section : sections) {
            if (sectionType.equalsIgnoreCase(section.getSectionType()) 
                    && Boolean.TRUE.equals(section.getIsVisible())) {
                return section.getContent();
            }
        }
        return null;
    }

    public String getFullName() {
        if (fullName != null) return fullName;
        return getSectionContent("PERSONAL_INFO");
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        if (email != null) return email;
        return getSectionContent("CONTACT");
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSummary() {
        if (summary != null) return summary;
        return getSectionContent("SUMMARY");
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getEducation() {
        return education;
    }

    public void setEducation(List<String> education) {
        this.education = education;
    }

    public List<String> getExperience() {
        return experience;
    }

    public void setExperience(List<String> experience) {
        this.experience = experience;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }
}
