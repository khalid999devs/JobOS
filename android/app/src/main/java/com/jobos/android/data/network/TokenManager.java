package com.jobos.android.data.network;

import android.content.Context;
import com.jobos.android.data.local.SessionManager;
import com.jobos.android.data.model.auth.AuthResponse;

/**
 * Manages authentication tokens and automatic refresh
 */
public class TokenManager {
    
    private final SessionManager sessionManager;
    private final ApiService apiService;
    private boolean isRefreshing = false;
    
    public interface TokenCallback {
        void onTokenReady(String accessToken);
        void onTokenRefreshFailed(String error);
    }
    
    public TokenManager(Context context) {
        this.sessionManager = new SessionManager(context);
        this.apiService = new ApiService();
    }
    
    public TokenManager(SessionManager sessionManager, ApiService apiService) {
        this.sessionManager = sessionManager;
        this.apiService = apiService;
    }
    
    /**
     * Get a valid access token. If the current token might be expired,
     * this will attempt to refresh it first.
     */
    public void getValidToken(TokenCallback callback) {
        String accessToken = sessionManager.getAccessToken();
        String refreshToken = sessionManager.getRefreshToken();
        
        if (accessToken == null || accessToken.isEmpty()) {
            if (refreshToken != null && !refreshToken.isEmpty()) {
                refreshAccessToken(callback);
            } else {
                callback.onTokenRefreshFailed("No tokens available. Please login again.");
            }
            return;
        }
        
        // Return current token - API will handle if it's expired
        callback.onTokenReady(accessToken);
    }
    
    /**
     * Force refresh the access token using the refresh token
     */
    public synchronized void refreshAccessToken(TokenCallback callback) {
        if (isRefreshing) {
            // Wait a bit and try with current token
            callback.onTokenReady(sessionManager.getAccessToken());
            return;
        }
        
        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            callback.onTokenRefreshFailed("No refresh token available. Please login again.");
            return;
        }
        
        isRefreshing = true;
        
        apiService.refreshToken(refreshToken, new ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse response) {
                isRefreshing = false;
                
                // Save new tokens
                sessionManager.saveAuthTokens(response.getAccessToken(), response.getRefreshToken());
                
                // Also update user info if available
                if (response.getEmail() != null) {
                    sessionManager.saveUserInfo(
                        response.getUserId(),
                        response.getEmail(),
                        response.getName(),
                        response.getRole()
                    );
                }
                
                callback.onTokenReady(response.getAccessToken());
            }

            @Override
            public void onError(String error) {
                isRefreshing = false;
                callback.onTokenRefreshFailed(error);
            }
        });
    }
    
    /**
     * Check if user is logged in with valid tokens
     */
    public boolean hasValidSession() {
        return sessionManager.isLoggedIn() && 
               sessionManager.getAccessToken() != null && 
               !sessionManager.getAccessToken().isEmpty();
    }
    
    /**
     * Clear all tokens and session
     */
    public void clearTokens() {
        sessionManager.clearSession();
    }
}
