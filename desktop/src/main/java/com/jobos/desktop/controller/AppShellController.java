package com.jobos.desktop.controller;

import com.jobos.desktop.core.navigation.NavigationManager;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.model.UserRole;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Node;

public class AppShellController {
    
    @FXML
    private Label titleLabel;
    
    @FXML
    private Label creditsLabel;
    
    @FXML
    private Button notificationButton;
    
    @FXML
    private Label badgeLabel;
    
    @FXML
    private Label userNameLabel;
    
    @FXML
    private Label userRoleLabel;
    
    @FXML
    private Button logoutButton;
    
    @FXML
    private VBox sidebar;
    
    @FXML
    private StackPane contentArea;
    
    @FXML
    private void initialize() {
        buildSidebar();
        updateCredits();
        updateNotificationBadge();
        updateUserInfo();
        
        // Mark first sidebar item as active
        if (!sidebar.getChildren().isEmpty()) {
            sidebar.getChildren().get(0).getStyleClass().add("active");
        }
    }
    
    private void buildSidebar() {
        UserRole role = SessionManager.getInstance().getUserRole();
        
        if (role == UserRole.JOB_SEEKER) {
            addSidebarItem("Dashboard", Route.SEEKER_DASHBOARD);
            addSidebarItem("Browse Jobs", Route.SEEKER_JOBS);
            addSidebarItem("Applications", Route.SEEKER_APPLICATIONS);
            addSidebarItem("My CVs", Route.SEEKER_CVS);
        } else {
            addSidebarItem("Dashboard", Route.POSTER_DASHBOARD);
            addSidebarItem("Job Posts", Route.POSTER_JOB_POSTS);
            addSidebarItem("Applicants", Route.POSTER_APPLICANTS);
        }
    }
    
    private void addSidebarItem(String text, Route route) {
        Button item = new Button(text);
        item.getStyleClass().add("sidebar-item");
        item.setMaxWidth(Double.MAX_VALUE);
        
        item.setOnAction(e -> {
            NavigationManager.getInstance().navigate(route);
            updateActiveSidebarItem(item);
        });
        
        sidebar.getChildren().add(item);
    }
    
    private void updateActiveSidebarItem(Button activeItem) {
        sidebar.getChildren().forEach(node -> {
            node.getStyleClass().remove("active");
        });
        activeItem.getStyleClass().add("active");
    }
    
    public void setContent(Node content) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
    }
    
    public void setTitle(String title) {
        titleLabel.setText(title);
    }
    
    private void updateCredits() {
        // Mock data for prototype
        creditsLabel.setText("250");
    }
    
    private void updateNotificationBadge() {
        // Mock data for prototype
        int unreadCount = 5;
        if (unreadCount > 0) {
            badgeLabel.setText(String.valueOf(unreadCount));
            badgeLabel.setVisible(true);
        } else {
            badgeLabel.setVisible(false);
        }
    }
    
    private void updateUserInfo() {
        // Mock data for prototype
        String email = SessionManager.getInstance().getEmail();
        userNameLabel.setText(email != null ? email.split("@")[0] : "User");
        
        UserRole role = SessionManager.getInstance().getUserRole();
        userRoleLabel.setText(role == UserRole.JOB_SEEKER ? "Job Seeker" : "Job Poster");
    }
    
    @FXML
    private void handleNotifications() {
        NavigationManager.getInstance().navigate(Route.NOTIFICATIONS);
    }
    
    @FXML
    private void handleSettings() {
        NavigationManager.getInstance().navigate(Route.SETTINGS);
    }
    
    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        NavigationManager.getInstance().navigate(Route.WELCOME);
    }
}
