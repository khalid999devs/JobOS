package com.jobos.desktop.core.config;

public final class Constants {
    
    private Constants() {}
    
    public static final String WINDOW_STATE_FILE = "window-state.json";
    
    public static final class Api {
        public static final String AUTH_LOGIN = "/api/auth/login";
        public static final String AUTH_REGISTER = "/api/auth/register";
        public static final String AUTH_REFRESH = "/api/auth/refresh";
        public static final String AUTH_LOGOUT = "/api/auth/logout";
        public static final String AUTH_FORGOT_PASSWORD = "/api/auth/forgot-password";
        public static final String AUTH_RESET_PASSWORD = "/api/auth/reset-password";
        
        public static final String USER_ME = "/api/users/me";
        public static final String USER_PREFERENCES = "/api/users/me/preferences";
        
        public static final String JOBS = "/api/jobs";
        public static final String JOB_POSTS = "/api/job-posts";
        public static final String APPLICATIONS = "/api/applications";
        public static final String CVS = "/api/cvs";
        public static final String CV_TEMPLATES = "/api/cv-templates";
        
        public static final String CREDITS_BALANCE = "/api/credits/balance";
        public static final String CREDITS_TRANSACTIONS = "/api/credits/transactions";
        public static final String PLANS = "/api/plans";
        
        public static final String NOTIFICATIONS = "/api/notifications";
        public static final String NOTIFICATIONS_UNREAD_COUNT = "/api/notifications/unread-count";
    }
    
    public static final class Prefs {
        public static final String ACCESS_TOKEN = "access_token";
        public static final String REFRESH_TOKEN = "refresh_token";
        public static final String USER_ID = "user_id";
        public static final String USER_EMAIL = "user_email";
        public static final String USER_ROLE = "user_role";
    }
}
