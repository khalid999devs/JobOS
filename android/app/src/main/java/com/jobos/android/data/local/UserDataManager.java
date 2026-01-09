package com.jobos.android.data.local;

import android.content.Context;
import com.jobos.android.data.model.profile.ProfileResponse;

public class UserDataManager {
    
    private static UserDataManager instance;
    private ProfileResponse currentUser;
    
    private UserDataManager() {}
    
    public static synchronized UserDataManager getInstance() {
        if (instance == null) {
            instance = new UserDataManager();
        }
        return instance;
    }
    
    public void setCurrentUser(ProfileResponse user) {
        this.currentUser = user;
    }
    
    public ProfileResponse getCurrentUser() {
        return currentUser;
    }
    
    public String getFullName() {
        if (currentUser == null) return "";
        String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
        String lastName = currentUser.getLastName() != null ? currentUser.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }
    
    public String getFirstName() {
        return currentUser != null && currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
    }
    
    public String getLastName() {
        return currentUser != null && currentUser.getLastName() != null ? currentUser.getLastName() : "";
    }
    
    public String getEmail() {
        return currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : "";
    }
    
    public String getRole() {
        return currentUser != null && currentUser.getRole() != null ? currentUser.getRole() : "";
    }
    
    public String getPhone() {
        return currentUser != null && currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "";
    }
    
    public String getLocation() {
        return currentUser != null && currentUser.getLocation() != null ? currentUser.getLocation() : "";
    }
    
    public String getBio() {
        return currentUser != null && currentUser.getBio() != null ? currentUser.getBio() : "";
    }
    
    public String getCompanyName() {
        if (currentUser != null && currentUser.getPosterProfile() != null) {
            return currentUser.getPosterProfile().getCompanyName() != null ? 
                   currentUser.getPosterProfile().getCompanyName() : "";
        }
        return "";
    }
    
    public String getCompanySize() {
        if (currentUser != null && currentUser.getPosterProfile() != null) {
            return currentUser.getPosterProfile().getCompanySize() != null ? 
                   currentUser.getPosterProfile().getCompanySize() : "";
        }
        return "";
    }
    
    public String getIndustry() {
        if (currentUser != null && currentUser.getPosterProfile() != null) {
            return currentUser.getPosterProfile().getIndustry() != null ? 
                   currentUser.getPosterProfile().getIndustry() : "";
        }
        return "";
    }
    
    public String getWebsite() {
        if (currentUser != null && currentUser.getPosterProfile() != null) {
            return currentUser.getPosterProfile().getWebsite() != null ? 
                   currentUser.getPosterProfile().getWebsite() : "";
        }
        return "";
    }
    
    public boolean isPoster() {
        return "POSTER".equals(getRole());
    }
    
    public boolean isSeeker() {
        return "SEEKER".equals(getRole());
    }
    
    public void clear() {
        currentUser = null;
    }
}
