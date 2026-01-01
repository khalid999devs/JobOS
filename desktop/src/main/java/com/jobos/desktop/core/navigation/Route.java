package com.jobos.desktop.core.navigation;

public enum Route {
    WELCOME("/welcome"),
    LOGIN("/login"),
    REGISTER("/register"),
    FORGOT_PASSWORD("/forgot-password"),
    RESET_PASSWORD("/reset-password"),
    
    SEEKER_DASHBOARD("/seeker/dashboard"),
    SEEKER_CVS("/seeker/cvs"),
    SEEKER_CV_EDITOR("/seeker/cv-editor"),
    SEEKER_JOBS("/seeker/jobs"),
    SEEKER_JOB_DETAIL("/seeker/job-detail"),
    SEEKER_APPLICATIONS("/seeker/applications"),
    
    POSTER_DASHBOARD("/poster/dashboard"),
    POSTER_JOB_POSTS("/poster/job-posts"),
    POSTER_JOB_FORM("/poster/job-form"),
    POSTER_APPLICANTS("/poster/applicants"),
    
    NOTIFICATIONS("/notifications"),
    SETTINGS("/settings");
    
    private final String path;
    
    Route(String path) {
        this.path = path;
    }
    
    public String getPath() {
        return path;
    }
    
    public boolean requiresAuth() {
        return this != WELCOME && this != LOGIN && this != REGISTER 
            && this != FORGOT_PASSWORD && this != RESET_PASSWORD;
    }
}
