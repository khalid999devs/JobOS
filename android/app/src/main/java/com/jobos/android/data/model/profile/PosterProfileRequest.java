package com.jobos.android.data.model.profile;

import java.util.List;

public class PosterProfileRequest {
    
    private String companyName;
    private String companySize;
    private String industry;
    private String website;
    private List<String> verificationDocuments;

    public PosterProfileRequest() {}

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
