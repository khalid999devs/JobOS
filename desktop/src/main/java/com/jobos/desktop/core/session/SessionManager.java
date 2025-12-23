package com.jobos.desktop.core.session;

import com.jobos.desktop.model.UserRole;

public class SessionManager {
    private static SessionManager instance;
    
    private String accessToken;
    private String refreshToken;
    private String userId;
    private String email;
    private UserRole userRole;
    
    private SessionManager() {}
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    public void login(String accessToken, String refreshToken, String userId, String email, UserRole role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
        this.userRole = role;
    }
    
    public void logout() {
        this.accessToken = null;
        this.refreshToken = null;
        this.userId = null;
        this.email = null;
        this.userRole = null;
    }
    
    public boolean isAuthenticated() {
        return accessToken != null && userId != null;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public UserRole getUserRole() {
        return userRole;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
