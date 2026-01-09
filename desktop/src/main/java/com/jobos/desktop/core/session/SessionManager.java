package com.jobos.desktop.core.session;

import com.jobos.desktop.core.config.Constants;
import com.jobos.desktop.model.UserRole;
import com.jobos.shared.dto.profile.ProfileResponse;

import java.util.prefs.Preferences;

public class SessionManager {
    
    private static SessionManager instance;
    private final Preferences prefs;
    private final TokenStore tokenStore;
    
    private String userId;
    private String email;
    private UserRole userRole;
    private ProfileResponse profile;
    private boolean profileCompleted;
    
    private SessionManager() {
        prefs = Preferences.userNodeForPackage(SessionManager.class);
        tokenStore = TokenStore.getInstance();
        loadFromPrefs();
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    private void loadFromPrefs() {
        userId = prefs.get(Constants.Prefs.USER_ID, null);
        email = prefs.get(Constants.Prefs.USER_EMAIL, null);
        String roleStr = prefs.get(Constants.Prefs.USER_ROLE, null);
        userRole = UserRole.fromString(roleStr);
        profileCompleted = prefs.getBoolean("profileCompleted", false);
    }
    
    public void login(String userId, String email, String role, String accessToken, String refreshToken, boolean profileCompleted) {
        this.userId = userId;
        this.email = email;
        this.userRole = UserRole.fromString(role);
        this.profileCompleted = profileCompleted;
        
        if (userId != null) prefs.put(Constants.Prefs.USER_ID, userId);
        if (email != null) prefs.put(Constants.Prefs.USER_EMAIL, email);
        if (role != null) prefs.put(Constants.Prefs.USER_ROLE, role);
        prefs.putBoolean("profileCompleted", profileCompleted);
        
        tokenStore.saveTokens(accessToken, refreshToken);
        
        flushPrefs();
    }
    
    public void updateProfile(ProfileResponse profile) {
        this.profile = profile;
        if (profile != null) {
            if (profile.getId() != null && !profile.getId().isEmpty()) {
                this.userId = profile.getId();
                prefs.put(Constants.Prefs.USER_ID, userId);
            }
            if (profile.getEmail() != null) {
                this.email = profile.getEmail();
                prefs.put(Constants.Prefs.USER_EMAIL, email);
            }
            if (profile.getRole() != null) {
                this.userRole = UserRole.fromString(profile.getRole());
                prefs.put(Constants.Prefs.USER_ROLE, profile.getRole());
            }
            if (profile.getProfileCompleted() != null) {
                this.profileCompleted = profile.getProfileCompleted();
                prefs.putBoolean("profileCompleted", profileCompleted);
            }
            flushPrefs();
        }
    }
    
    private void flushPrefs() {
        try {
            prefs.flush();
        } catch (Exception ignored) {}
    }
    
    public void setProfileCompleted(boolean completed) {
        this.profileCompleted = completed;
        prefs.putBoolean("profileCompleted", completed);
        flushPrefs();
    }
    
    public boolean isProfileCompleted() {
        return profileCompleted;
    }
    
    public void logout() {
        userId = null;
        email = null;
        userRole = null;
        profile = null;
        profileCompleted = false;
        
        prefs.remove(Constants.Prefs.USER_ID);
        prefs.remove(Constants.Prefs.USER_EMAIL);
        prefs.remove(Constants.Prefs.USER_ROLE);
        prefs.remove("profileCompleted");
        
        tokenStore.clearTokens();
        flushPrefs();
    }
    
    public boolean isAuthenticated() {
        if (!tokenStore.hasTokens()) {
            return false;
        }
        if (userId == null || userRole == null) {
            loadFromPrefs();
        }
        return userId != null && userRole != null;
    }
    
    public boolean hasStoredSession() {
        if (!tokenStore.hasTokens()) {
            return false;
        }
        if (userId == null || userRole == null) {
            loadFromPrefs();
        }
        return tokenStore.hasTokens();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public UserRole getUserRole() {
        if (userRole == null) {
            loadFromPrefs();
        }
        return userRole;
    }
    
    public ProfileResponse getProfile() {
        return profile;
    }
    
    public void setProfile(ProfileResponse profile) {
        updateProfile(profile);
    }
    
    public TokenStore getTokenStore() {
        return tokenStore;
    }
}
