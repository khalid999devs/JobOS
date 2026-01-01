package com.jobos.android.data.local;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    
    private static final String PREF_NAME = "jobos_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_PROFILE_COMPLETE = "profile_complete";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;
    
    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }
    
    public void saveAuthTokens(String accessToken, String refreshToken) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.apply();
    }
    
    public void saveUserInfo(Long userId, String email, String name, String role) {
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_ROLE, role);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    public void setProfileComplete(boolean complete) {
        editor.putBoolean(KEY_PROFILE_COMPLETE, complete);
        editor.apply();
    }
    
    public void saveFcmToken(String token) {
        editor.putString(KEY_FCM_TOKEN, token);
        editor.apply();
    }
    
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    public Long getUserId() {
        long id = prefs.getLong(KEY_USER_ID, -1);
        return id == -1 ? null : id;
    }
    
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }
    
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }
    
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public boolean isProfileComplete() {
        return prefs.getBoolean(KEY_PROFILE_COMPLETE, false);
    }
    
    public String getFcmToken() {
        return prefs.getString(KEY_FCM_TOKEN, null);
    }
    
    public boolean isSeeker() {
        return "SEEKER".equals(getUserRole());
    }
    
    public boolean isPoster() {
        return "POSTER".equals(getUserRole());
    }
    
    public void clearSession() {
        String fcmToken = getFcmToken();
        editor.clear();
        if (fcmToken != null) {
            editor.putString(KEY_FCM_TOKEN, fcmToken);
        }
        editor.apply();
    }
    
    public void updateAccessToken(String newAccessToken) {
        editor.putString(KEY_ACCESS_TOKEN, newAccessToken);
        editor.apply();
    }
}
