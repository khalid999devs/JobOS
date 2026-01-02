package com.jobos.android.data.model.profile;

import java.util.List;

public class UpdateProfileRequest {

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String avatarUrl;
    private String bio;
    private String location;
    private String timezone;
    
    // Additional fields used by Android UI
    private String name;
    private String phone;
    private String jobTitle;
    private List<String> skills;
    private String companyName;
    private String companyWebsite;
    private String website;
    private String companyDescription;
    private Integer experienceYears;
    private String role;

    public UpdateProfileRequest() {
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

    // Convenience methods for Android UI
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        // Also split into firstName/lastName if contains space
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : null;
        } else {
            this.firstName = name;
        }
    }

    public String getPhone() {
        return phone != null ? phone : phoneNumber;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.phoneNumber = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyWebsite() {
        return companyWebsite;
    }

    public void setCompanyWebsite(String companyWebsite) {
        this.companyWebsite = companyWebsite;
    }

    public String getWebsite() {
        return website != null ? website : companyWebsite;
    }

    public void setWebsite(String website) {
        this.website = website;
        this.companyWebsite = website;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
