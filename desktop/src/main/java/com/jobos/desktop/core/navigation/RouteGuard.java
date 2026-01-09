package com.jobos.desktop.core.navigation;

import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.model.UserRole;

public class RouteGuard {
    
    private static RouteGuard instance;
    private final SessionManager sessionManager;
    
    private RouteGuard() {
        this.sessionManager = SessionManager.getInstance();
    }
    
    public static RouteGuard getInstance() {
        if (instance == null) {
            instance = new RouteGuard();
        }
        return instance;
    }
    
    public RouteGuardResult canActivate(Route route) {
        if (!route.requiresAuth()) {
            return RouteGuardResult.allowed();
        }
        
        if (!sessionManager.isAuthenticated()) {
            return RouteGuardResult.redirectTo(Route.LOGIN);
        }
        
        UserRole requiredRole = route.getRequiredRole();
        if (requiredRole != null) {
            UserRole currentRole = sessionManager.getUserRole();
            if (currentRole != requiredRole) {
                return RouteGuardResult.redirectTo(getDefaultRouteForRole(currentRole));
            }
        }
        
        return RouteGuardResult.allowed();
    }
    
    public Route getDefaultRouteForRole(UserRole role) {
        if (role == null) {
            return Route.WELCOME;
        }
        return switch (role) {
            case SEEKER -> Route.SEEKER_DASHBOARD;
            case POSTER -> Route.POSTER_DASHBOARD;
        };
    }
    
    public static class RouteGuardResult {
        private final boolean allowed;
        private final Route redirectRoute;
        
        private RouteGuardResult(boolean allowed, Route redirectRoute) {
            this.allowed = allowed;
            this.redirectRoute = redirectRoute;
        }
        
        public static RouteGuardResult allowed() {
            return new RouteGuardResult(true, null);
        }
        
        public static RouteGuardResult redirectTo(Route route) {
            return new RouteGuardResult(false, route);
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public Route getRedirectRoute() {
            return redirectRoute;
        }
    }
}
