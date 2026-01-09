package com.jobos.android.data.model.cv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

    // Parse JSON content from section and get a specific field
    private String getFieldFromSection(String sectionType, String fieldName) {
        String content = getSectionContent(sectionType);
        if (content == null || content.isEmpty()) return null;
        try {
            Map<String, Object> contentMap = objectMapper.readValue(content, 
                new TypeReference<Map<String, Object>>() {});
            Object value = contentMap.get(fieldName);
            if (value != null) {
                return value.toString();
            }
            // Also try "placeholder" field (used in templates)
            value = contentMap.get("placeholder");
            if (value != null) {
                return value.toString();
            }
            return null;
        } catch (Exception e) {
            // Content might be plain text, not JSON
            return content;
        }
    }

    // Parse JSON content from section and get a list field
    private List<String> getListFromSection(String sectionType, String fieldName) {
        String content = getSectionContent(sectionType);
        if (content == null || content.isEmpty()) return null;
        try {
            Map<String, Object> contentMap = objectMapper.readValue(content, 
                new TypeReference<Map<String, Object>>() {});
            Object value = contentMap.get(fieldName);
            if (value instanceof List) {
                List<String> result = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof String) {
                        result.add((String) item);
                    } else if (item instanceof Map) {
                        // Handle complex objects - extract display text
                        result.add(formatComplexItem((Map<?, ?>) item));
                    } else {
                        result.add(item.toString());
                    }
                }
                return result.isEmpty() ? null : result;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // Format a complex item (map) into a readable string
    private String formatComplexItem(Map<?, ?> item) {
        StringBuilder sb = new StringBuilder();
        // Try common field patterns for experience/education
        if (item.containsKey("title") || item.containsKey("position")) {
            Object titleVal = item.get("title");
            if (titleVal == null) titleVal = item.get("position");
            if (titleVal != null) sb.append(titleVal);
        }
        if (item.containsKey("company") || item.containsKey("organization")) {
            Object compVal = item.get("company");
            if (compVal == null) compVal = item.get("organization");
            if (compVal != null) {
                if (sb.length() > 0) sb.append(" at ");
                sb.append(compVal);
            }
        }
        if (item.containsKey("institution") || item.containsKey("school")) {
            Object instVal = item.get("institution");
            if (instVal == null) instVal = item.get("school");
            if (instVal != null) {
                if (sb.length() > 0) sb.append(" - ");
                sb.append(instVal);
            }
        }
        if (item.containsKey("degree")) {
            Object degreeVal = item.get("degree");
            if (degreeVal != null) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(degreeVal);
            }
        }
        if (item.containsKey("startDate") || item.containsKey("endDate")) {
            Object startVal = item.get("startDate");
            Object endVal = item.get("endDate");
            if (startVal != null || endVal != null) {
                if (sb.length() > 0) sb.append(" (");
                if (startVal != null) sb.append(startVal);
                if (startVal != null && endVal != null) sb.append(" - ");
                if (endVal != null) sb.append(endVal);
                if (startVal != null || endVal != null) sb.append(")");
            }
        }
        if (sb.length() == 0) {
            // Fallback: just concatenate all string values
            for (Object val : item.values()) {
                if (val instanceof String && !((String) val).isEmpty()) {
                    if (sb.length() > 0) sb.append(" | ");
                    sb.append(val);
                }
            }
        }
        return sb.toString();
    }

    public String getFullName() {
        if (fullName != null) return fullName;
        // Try to get from PERSONAL_INFO section
        String name = getFieldFromSection("PERSONAL_INFO", "fullName");
        if (name == null) name = getFieldFromSection("PERSONAL_INFO", "name");
        return name;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        if (email != null) return email;
        // Try to get from PERSONAL_INFO section first
        String emailVal = getFieldFromSection("PERSONAL_INFO", "email");
        return emailVal;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        if (phone != null) return phone;
        return getFieldFromSection("PERSONAL_INFO", "phone");
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        if (address != null) return address;
        return getFieldFromSection("PERSONAL_INFO", "address");
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSummary() {
        if (summary != null) return summary;
        // Try to get text content from SUMMARY section
        String summaryContent = getFieldFromSection("SUMMARY", "text");
        if (summaryContent == null) summaryContent = getFieldFromSection("SUMMARY", "summary");
        if (summaryContent == null) summaryContent = getFieldFromSection("SUMMARY", "placeholder");
        if (summaryContent == null) {
            // If all else fails, try to parse the raw content
            String rawContent = getSectionContent("SUMMARY");
            if (rawContent != null && rawContent.trim().startsWith("{")) {
                // Don't return raw JSON - try to extract any text value
                try {
                    Map<String, Object> contentMap = objectMapper.readValue(rawContent, 
                        new TypeReference<Map<String, Object>>() {});
                    // Try common text fields
                    for (String field : new String[]{"text", "summary", "placeholder", "content", "value"}) {
                        Object val = contentMap.get(field);
                        if (val != null && !val.toString().trim().isEmpty()) {
                            return val.toString();
                        }
                    }
                } catch (Exception e) {
                    // Ignore parsing error
                }
            } else {
                return rawContent; // It's plain text
            }
        }
        return summaryContent;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getSkills() {
        if (skills != null && !skills.isEmpty()) return skills;
        // Try to get from SKILLS section
        List<String> skillsList = getListFromSection("SKILLS", "skills");
        if (skillsList == null) skillsList = getListFromSection("SKILLS", "items");
        return skillsList;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getEducation() {
        if (education != null && !education.isEmpty()) return education;
        // Try to get from EDUCATION section
        List<String> eduList = getListFromSection("EDUCATION", "entries");
        if (eduList == null) eduList = getListFromSection("EDUCATION", "items");
        if (eduList == null) eduList = getListFromSection("EDUCATION", "education");
        return eduList;
    }

    public void setEducation(List<String> education) {
        this.education = education;
    }

    public List<String> getExperience() {
        if (experience != null && !experience.isEmpty()) return experience;
        // Try to get from EXPERIENCE section
        List<String> expList = getListFromSection("EXPERIENCE", "entries");
        if (expList == null) expList = getListFromSection("EXPERIENCE", "items");
        if (expList == null) expList = getListFromSection("EXPERIENCE", "experience");
        return expList;
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
