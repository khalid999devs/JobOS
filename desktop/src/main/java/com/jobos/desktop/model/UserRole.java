package com.jobos.desktop.model;

public enum UserRole {
    JOB_SEEKER,
    JOB_POSTER;
    
    public static UserRole fromString(String role) {
        if (role == null) return null;
        return switch (role.toUpperCase()) {
            case "JOB_SEEKER", "SEEKER" -> JOB_SEEKER;
            case "JOB_POSTER", "POSTER" -> JOB_POSTER;
            default -> null;
        };
    }
}
