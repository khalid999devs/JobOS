package com.jobos.desktop.core.navigation;

import com.jobos.desktop.controller.AppShellController;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.model.UserRole;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NavigationManager {
    private static NavigationManager instance;
    private final Map<Route, String> routeMap = new HashMap<>();
    private Route currentRoute;
    private Consumer<Parent> navigationListener;
    private AppShellController appShellController;
    private Parent appShellRoot;
    
    private NavigationManager() {
        // Map routes to FXML files
        routeMap.put(Route.WELCOME, "/fxml/welcome.fxml");
        routeMap.put(Route.LOGIN, "/fxml/login.fxml");
        routeMap.put(Route.SEEKER_DASHBOARD, "/fxml/seeker-dashboard.fxml");
        routeMap.put(Route.SEEKER_JOBS, "/fxml/seeker-jobs.fxml");
        routeMap.put(Route.SEEKER_APPLICATIONS, "/fxml/seeker-applications.fxml");
        routeMap.put(Route.SEEKER_CVS, "/fxml/seeker-cvs.fxml");
    }
    
    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }
    
    public void setNavigationListener(Consumer<Parent> listener) {
        this.navigationListener = listener;
    }
    
    public Parent navigate(Route route) {
        if (route.requiresAuth() && !SessionManager.getInstance().isAuthenticated()) {
            return navigate(Route.LOGIN);
        }
        
        if (isRoleRestricted(route)) {
            return navigate(getDefaultRouteForRole());
        }
        
        currentRoute = route;
        Parent content = loadFXML(route);
        
        // Wrap authenticated screens in AppShell
        if (route.requiresAuth() && content != null) {
            if (appShellRoot == null) {
                loadAppShell();
            }
            if (appShellController != null) {
                appShellController.setContent(content);
                appShellController.setTitle(getRouteTitle(route));
                if (navigationListener != null) {
                    navigationListener.accept(appShellRoot);
                }
                return appShellRoot;
            }
        }
        
        if (navigationListener != null && content != null) {
            navigationListener.accept(content);
        }
        
        return content;
    }
    
    private void loadAppShell() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/app-shell.fxml"));
            appShellRoot = loader.load();
            appShellController = loader.getController();
        } catch (IOException e) {
            System.err.println("Failed to load AppShell");
            e.printStackTrace();
        }
    }
    
    private String getRouteTitle(Route route) {
        return switch (route) {
            case SEEKER_DASHBOARD -> "Dashboard";
            case SEEKER_JOBS -> "Browse Jobs";
            case SEEKER_APPLICATIONS -> "My Applications";
            case SEEKER_CVS -> "My CVs";
            case POSTER_DASHBOARD -> "Dashboard";
            case POSTER_JOB_POSTS -> "Job Posts";
            case POSTER_APPLICANTS -> "Applicants";
            case SETTINGS -> "Settings";
            case NOTIFICATIONS -> "Notifications";
            default -> "JobOS";
        };
    }
    
    private Parent loadFXML(Route route) {
        String fxmlPath = routeMap.get(route);
        if (fxmlPath == null) {
            System.err.println("No FXML file mapped for route: " + route);
            return null;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            return loader.load();
        } catch (IOException e) {
            System.err.println("Failed to load FXML: " + fxmlPath);
            e.printStackTrace();
            return null;
        }
    }
    
    public Route getCurrentRoute() {
        return currentRoute;
    }
    
    private boolean isRoleRestricted(Route route) {
        SessionManager session = SessionManager.getInstance();
        if (!session.isAuthenticated()) return false;
        
        UserRole role = session.getUserRole();
        String path = route.getPath();
        
        if (path.startsWith("/seeker") && role != UserRole.JOB_SEEKER) {
            return true;
        }
        if (path.startsWith("/poster") && role != UserRole.JOB_POSTER) {
            return true;
        }
        
        return false;
    }
    
    private Route getDefaultRouteForRole() {
        UserRole role = SessionManager.getInstance().getUserRole();
        return role == UserRole.JOB_SEEKER ? Route.SEEKER_DASHBOARD : Route.POSTER_DASHBOARD;
    }
}
