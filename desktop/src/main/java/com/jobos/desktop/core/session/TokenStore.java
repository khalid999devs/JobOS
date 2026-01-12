package com.jobos.desktop.core.session;

import com.jobos.desktop.core.config.Constants;

import java.util.Base64;
import java.util.prefs.Preferences;

public class TokenStore {
    
    private static TokenStore instance;
    private final Preferences prefs;
    
    private String accessToken;
    private String refreshToken;
    
    private TokenStore() {
        prefs = Preferences.userNodeForPackage(TokenStore.class);
        loadFromPrefs();
    }
    
    public static TokenStore getInstance() {
        if (instance == null) {
            instance = new TokenStore();
        }
        return instance;
    }
    
    private void loadFromPrefs() {
        String encodedAccess = prefs.get(Constants.Prefs.ACCESS_TOKEN, null);
        String encodedRefresh = prefs.get(Constants.Prefs.REFRESH_TOKEN, null);
        
        if (encodedAccess != null) {
            accessToken = decode(encodedAccess);
        }
        if (encodedRefresh != null) {
            refreshToken = decode(encodedRefresh);
        }
    }
    
    public void saveTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        
        if (accessToken != null && !accessToken.isEmpty()) {
            prefs.put(Constants.Prefs.ACCESS_TOKEN, encode(accessToken));
        } else {
            prefs.remove(Constants.Prefs.ACCESS_TOKEN);
        }
        
        if (refreshToken != null && !refreshToken.isEmpty()) {
            prefs.put(Constants.Prefs.REFRESH_TOKEN, encode(refreshToken));
        } else {
            prefs.remove(Constants.Prefs.REFRESH_TOKEN);
        }
        
        flushPrefs();
    }
    
    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
        if (accessToken != null && !accessToken.isEmpty()) {
            prefs.put(Constants.Prefs.ACCESS_TOKEN, encode(accessToken));
        } else {
            prefs.remove(Constants.Prefs.ACCESS_TOKEN);
        }
        flushPrefs();
    }
    
    public String getAccessToken() {
        if (accessToken == null) {
            loadFromPrefs();
        }
        return accessToken;
    }
    
    public String getRefreshToken() {
        if (refreshToken == null) {
            loadFromPrefs();
        }
        return refreshToken;
    }
    
    public boolean hasTokens() {
        if (refreshToken == null) {
            loadFromPrefs();
        }
        return refreshToken != null && !refreshToken.isEmpty();
    }
    
    public void clearTokens() {
        accessToken = null;
        refreshToken = null;
        prefs.remove(Constants.Prefs.ACCESS_TOKEN);
        prefs.remove(Constants.Prefs.REFRESH_TOKEN);
        flushPrefs();
    }
    
    private void flushPrefs() {
        try {
            prefs.flush();
        } catch (Exception ignored) {}
    }
    
    private String encode(String value) {
        if (value == null) return null;
        return Base64.getEncoder().encodeToString(value.getBytes());
    }
    
    private String decode(String encoded) {
        if (encoded == null) return null;
        try {
            return new String(Base64.getDecoder().decode(encoded));
        } catch (Exception e) {
            return null;
        }
    }
}
