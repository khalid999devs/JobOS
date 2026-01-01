package com.jobos.shared.dto.notification;

public class NotificationPreferenceResponse {
    private String userId;
    private Boolean emailEnabled;
    private Boolean pushEnabled;
    private Boolean applicationUpdates;
    private Boolean jobRecommendations;
    private Boolean marketingEmails;

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getPushEnabled() {
        return pushEnabled;
    }

    public void setPushEnabled(Boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }

    public Boolean getApplicationUpdates() {
        return applicationUpdates;
    }

    public void setApplicationUpdates(Boolean applicationUpdates) {
        this.applicationUpdates = applicationUpdates;
    }

    public Boolean getJobRecommendations() {
        return jobRecommendations;
    }

    public void setJobRecommendations(Boolean jobRecommendations) {
        this.jobRecommendations = jobRecommendations;
    }

    public Boolean getMarketingEmails() {
        return marketingEmails;
    }

    public void setMarketingEmails(Boolean marketingEmails) {
        this.marketingEmails = marketingEmails;
    }
}
