package com.jobos.desktop.model;

public enum UserRole {
    SEEKER,
    POSTER;
    
    public static UserRole fromString(String role) {
        if (role == null) return null;
        return switch (role.toUpperCase()) {
            case "JOB_SEEKER", "SEEKER" -> SEEKER;
            case "JOB_POSTER", "POSTER" -> POSTER;
            default -> null;
        };
    }
}
