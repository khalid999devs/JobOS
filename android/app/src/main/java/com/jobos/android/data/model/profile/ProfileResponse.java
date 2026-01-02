package com.jobos.android.data.model.profile;

import java.util.List;

public class ProfileResponse {

    private String id;
    private String email;
    private String role;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String avatarUrl;
    private String bio;
    private String location;
    private String timezone;
    private Boolean profileCompleted;
    private String createdAt;
    private SeekerPreferencesData seekerPreferences;
    private PosterProfileData posterProfile;

    public static class SeekerPreferencesData {
        private List<String> desiredRoles;
        private List<String> skills;
        private List<String> jobTypes;
        private String workingHours;
        private Integer salaryMin;
        private Integer salaryMax;
        private Boolean willingToRelocate;
        private String availableFrom;

        public List<String> getDesiredRoles() {
            return desiredRoles;
        }

        public void setDesiredRoles(List<String> desiredRoles) {
            this.desiredRoles = desiredRoles;
        }

        public List<String> getSkills() {
            return skills;
        }

        public void setSkills(List<String> skills) {
            this.skills = skills;
        }

        public List<String> getJobTypes() {
            return jobTypes;
        }

        public void setJobTypes(List<String> jobTypes) {
            this.jobTypes = jobTypes;
        }

        public String getWorkingHours() {
            return workingHours;
        }

        public void setWorkingHours(String workingHours) {
            this.workingHours = workingHours;
        }

        public Integer getSalaryMin() {
            return salaryMin;
        }

        public void setSalaryMin(Integer salaryMin) {
            this.salaryMin = salaryMin;
        }

        public Integer getSalaryMax() {
            return salaryMax;
        }

        public void setSalaryMax(Integer salaryMax) {
            this.salaryMax = salaryMax;
        }

        public Boolean getWillingToRelocate() {
            return willingToRelocate;
        }

        public void setWillingToRelocate(Boolean willingToRelocate) {
            this.willingToRelocate = willingToRelocate;
        }

        public String getAvailableFrom() {
            return availableFrom;
        }

        public void setAvailableFrom(String availableFrom) {
            this.availableFrom = availableFrom;
        }
    }

    public static class PosterProfileData {
        private String companyName;
        private String companySize;
        private String industry;
        private String website;
        private String verificationStatus;
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

        public String getVerificationStatus() {
            return verificationStatus;
        }

        public void setVerificationStatus(String verificationStatus) {
            this.verificationStatus = verificationStatus;
        }

        public List<String> getVerificationDocuments() {
            return verificationDocuments;
        }

        public void setVerificationDocuments(List<String> verificationDocuments) {
            this.verificationDocuments = verificationDocuments;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Boolean getProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(Boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public SeekerPreferencesData getSeekerPreferences() {
        return seekerPreferences;
    }

    public void setSeekerPreferences(SeekerPreferencesData seekerPreferences) {
        this.seekerPreferences = seekerPreferences;
    }

    public PosterProfileData getPosterProfile() {
        return posterProfile;
    }

    public void setPosterProfile(PosterProfileData posterProfile) {
        this.posterProfile = posterProfile;
    }

    public String getName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        }
        return "";
    }

    public String getPhone() {
        return phoneNumber;
    }

    public String getJobTitle() {
        if (seekerPreferences != null && seekerPreferences.getDesiredRoles() != null 
            && !seekerPreferences.getDesiredRoles().isEmpty()) {
            return seekerPreferences.getDesiredRoles().get(0);
        }
        return null;
    }

    public List<String> getSkills() {
        if (seekerPreferences != null) {
            return seekerPreferences.getSkills();
        }
        return null;
    }

    public String getCompanyName() {
        if (posterProfile != null) {
            return posterProfile.getCompanyName();
        }
        return null;
    }

    public String getCompanyWebsite() {
        if (posterProfile != null) {
            return posterProfile.getWebsite();
        }
        return null;
    }
}
