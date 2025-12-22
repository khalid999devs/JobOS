package com.jobos.shared.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public class PosterProfileRequest {
    
    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name cannot exceed 200 characters")
    private String companyName;
    
    @Size(max = 50, message = "Company size cannot exceed 50 characters")
    private String companySize;
    
    @Size(max = 100, message = "Industry cannot exceed 100 characters")
    private String industry;
    
    @Size(max = 500, message = "Website cannot exceed 500 characters")
    private String website;
    
    private List<String> verificationDocuments;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanySize() {
        return companySize;
    }

    public void setCompanySize(String companySize) {
        this.companySize = companySize;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public List<String> getVerificationDocuments() {
        return verificationDocuments;
    }

    public void setVerificationDocuments(List<String> verificationDocuments) {
        this.verificationDocuments = verificationDocuments;
    }
}
