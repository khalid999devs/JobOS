package com.jobos.backend.security;

import com.jobos.backend.domain.user.UserRole;

import java.util.UUID;

public class AuthenticatedUser {
    
    private final UUID userId;
    private final String email;
    private final UserRole role;

    public AuthenticatedUser(UUID userId, String email, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return userId.toString();
    }
}
