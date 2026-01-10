package com.jobos.desktop.core.navigation;

import com.jobos.desktop.model.UserRole;

public enum Route {
    WELCOME("/auth/welcome", false, false, null),
    LOGIN("/auth/login", false, false, null),
    REGISTER("/auth/register", false, false, null),
    FORGOT_PASSWORD("/auth/forgot-password", false, false, null),
    RESET_PASSWORD("/auth/reset-password", false, false, null),
    PROFILE_SETUP("/auth/profile-setup", true, false, null),
    
    SEEKER_DASHBOARD("/seeker/dashboard", true, true, UserRole.SEEKER),
    SEEKER_JOBS("/seeker/jobs", true, true, UserRole.SEEKER),
    SEEKER_JOB_DETAIL("/seeker/job-detail", true, true, UserRole.SEEKER),
    SEEKER_APPLY("/seeker/apply", true, true, UserRole.SEEKER),
    SEEKER_APPLICATIONS("/seeker/applications", true, true, UserRole.SEEKER),
    SEEKER_APPLICATION_DETAIL("/seeker/application-detail", true, true, UserRole.SEEKER),
    SEEKER_CVS("/seeker/cvs", true, true, UserRole.SEEKER),
    SEEKER_CV_EDITOR("/seeker/cv-editor", true, false, UserRole.SEEKER),
    
    POSTER_DASHBOARD("/poster/dashboard", true, true, UserRole.POSTER),
    POSTER_JOB_POSTS("/poster/job-posts", true, true, UserRole.POSTER),
    POSTER_JOB_FORM("/poster/job-form", true, true, UserRole.POSTER),
    POSTER_JOB_DETAIL("/poster/job-detail", true, true, UserRole.POSTER),
    POSTER_APPLICANTS("/poster/applicants", true, true, UserRole.POSTER),
    POSTER_APPLICATION_DETAIL("/poster/application-detail", true, true, UserRole.POSTER),
    
    NOTIFICATIONS("/notifications", true, true, null),
    SETTINGS("/settings", true, true, null),
    EDIT_PROFILE("/edit-profile", true, true, null),
    CREDITS("/credits", true, true, null);
    
    private final String path;
    private final boolean requiresAuth;
    private final boolean showShell;
    private final UserRole requiredRole;
    
    Route(String path, boolean requiresAuth, boolean showShell, UserRole requiredRole) {
        this.path = path;
        this.requiresAuth = requiresAuth;
        this.showShell = showShell;
        this.requiredRole = requiredRole;
    }
    
    public String getPath() {
        return path;
    }
    
    public boolean requiresAuth() {
        return requiresAuth;
    }
    
    public boolean showShell() {
        return showShell;
    }
    
    public UserRole getRequiredRole() {
        return requiredRole;
    }
    
    public String getFxmlPath() {
        return "/fxml" + path + ".fxml";
    }
    
    public String getTitle() {
        return switch (this) {
            case WELCOME -> "Welcome to JobOS";
            case LOGIN -> "Login";
            case REGISTER -> "Create Account";
            case FORGOT_PASSWORD -> "Forgot Password";
            case RESET_PASSWORD -> "Reset Password";
            case PROFILE_SETUP -> "Complete Your Profile";
            case SEEKER_DASHBOARD, POSTER_DASHBOARD -> "Dashboard";
            case SEEKER_JOBS -> "Browse Jobs";
            case SEEKER_JOB_DETAIL -> "Job Details";
            case SEEKER_APPLY -> "Apply for Job";
            case SEEKER_APPLICATIONS -> "My Applications";
            case SEEKER_APPLICATION_DETAIL -> "Application Details";
            case SEEKER_CVS -> "My CVs";
            case SEEKER_CV_EDITOR -> "CV Editor";
            case POSTER_JOB_POSTS -> "Job Posts";
            case POSTER_JOB_FORM -> "Job Form";
            case POSTER_JOB_DETAIL -> "Job Details";
            case POSTER_APPLICANTS -> "Applicants";
            case POSTER_APPLICATION_DETAIL -> "Application Details";
            case NOTIFICATIONS -> "Notifications";
            case SETTINGS -> "Settings";
            case EDIT_PROFILE -> "Edit Profile";
            case CREDITS -> "Credits & Plans";
        };
    }
}
