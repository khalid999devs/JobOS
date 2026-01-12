package com.jobos.desktop.controller.shell;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.ui.Modal;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.model.UserRole;
import com.jobos.desktop.service.ApiClient;
import com.jobos.desktop.service.CreditService;
import com.jobos.desktop.service.NotificationService;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class AppShellController implements Initializable, Router.AppShellAware {
    
    @FXML private VBox sidebar;
    @FXML private VBox navItems;
    @FXML private Label avatarLabel;
    @FXML private Label creditsLabel;
    @FXML private Label planLabel;
    @FXML private StackPane contentArea;
    @FXML private StackPane notificationBadgeContainer;
    @FXML private Label notificationCountLabel;
    @FXML private HBox headerBar;
    
    private Router router;
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final CreditService creditService = new CreditService();
    private final NotificationService notificationService = NotificationService.getInstance();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        buildNavigation();
        loadUserProfile();
        loadCreditsInfo();
        setupNotificationPolling();
    }
    
    @Override
    public void setRouter(Router router) {
        this.router = router;
    }
    
    @Override
    public StackPane getContentArea() {
        return contentArea;
    }
    
    @Override
    public void setTitle(String title) {
    }
    
    private void buildNavigation() {
        navItems.getChildren().clear();
        
        UserRole role = sessionManager.getUserRole();
        
        if (role == UserRole.SEEKER) {
            addNavItem("fas-chart-bar", "Dashboard", Route.SEEKER_DASHBOARD);
            addNavItem("fas-search", "Browse Jobs", Route.SEEKER_JOBS);
            addNavItem("fas-clipboard-list", "Applications", Route.SEEKER_APPLICATIONS);
            addNavItem("fas-file-alt", "My CVs", Route.SEEKER_CVS);
        } else if (role == UserRole.POSTER) {
            addNavItem("fas-chart-bar", "Dashboard", Route.POSTER_DASHBOARD);
            addNavItem("fas-edit", "Job Posts", Route.POSTER_JOB_POSTS);
            addNavItem("fas-users", "Applicants", Route.POSTER_APPLICANTS);
        }
    }
    
    private void addNavItem(String iconLiteral, String text, Route route) {
        Button navButton = new Button();
        navButton.getStyleClass().add("sidebar-item");
        navButton.setMaxWidth(Double.MAX_VALUE);
        navButton.setAlignment(Pos.CENTER_LEFT);
        
        HBox content = new HBox(10);
        content.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(14);
        icon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
        
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        content.getChildren().addAll(icon, label);
        navButton.setGraphic(content);
        
        navButton.setOnAction(e -> {
            if (router != null) {
                router.navigate(route);
                updateActiveNav(navButton);
            }
        });
        navItems.getChildren().add(navButton);
    }
    
    private void updateActiveNav(Button active) {
        navItems.getChildren().forEach(node -> {
            if (node instanceof Button btn) {
                btn.getStyleClass().remove("sidebar-item-active");
            }
        });
        active.getStyleClass().add("sidebar-item-active");
    }
    
    private void updateUserInfo() {
        ProfileResponse profile = sessionManager.getProfile();
        String avatarText = "U";
        
        if (profile != null && profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
            avatarText = profile.getFirstName().substring(0, 1).toUpperCase();
        } else {
            String email = sessionManager.getEmail();
            if (email != null && !email.isEmpty()) {
                avatarText = email.substring(0, 1).toUpperCase();
            }
        }
        
        avatarLabel.setText(avatarText);
    }
    
    private void loadUserProfile() {
  
        updateUserInfo();

        CompletableFuture.runAsync(() -> {
            ApiClient apiClient = ApiClient.getInstance();
            apiClient.get("/api/users/me", new TypeReference<ProfileResponse>() {})
                .thenAccept(profile -> {
                    if (profile != null) {
                        Platform.runLater(() -> {
                            sessionManager.setProfile(profile);
                            updateUserInfo();
                        });
                    }
                })
                .exceptionally(e -> null);
        });
    }
    
    private void loadCreditsInfo() {
        CompletableFuture.runAsync(() -> {
            try {
                var balance = creditService.getBalance();
                Platform.runLater(() -> {
                    if (balance != null) {
                        creditsLabel.setText(String.valueOf(balance.getCredits()));
                        String plan = balance.getPlan();
                        if (plan != null && !plan.isEmpty()) {
                            planLabel.setText(plan.toUpperCase());
                        } else {
                            planLabel.setText("FREE");
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    creditsLabel.setText("0");
                    planLabel.setText("FREE");
                });
            }
        });
    }
    
    private void setupNotificationPolling() {
        notificationService.setUnreadCountListener(count -> {
            Platform.runLater(() -> setUnreadNotificationCount(count.intValue()));
        });
        notificationService.startPolling(30);
    }
    
    @FXML
    private void onSettings() {
        if (router != null) {
            router.navigate(Route.SETTINGS);
        }
    }
    
    @FXML
    private void onCredits() {
        if (router != null) {
            router.navigate(Route.CREDITS);
        }
    }
    
    @FXML
    private void onNotifications() {
        if (router != null) {
            router.navigate(Route.NOTIFICATIONS);
        }
    }
    
    @FXML
    private void onProfile() {
        if (router != null) {
            router.navigate(Route.EDIT_PROFILE);
        }
    }
    
    @FXML
    private void onLogout() {
        Modal.confirm("Logout", "Are you sure you want to logout?", () -> {
            notificationService.stopPolling();
            sessionManager.logout();
            if (router != null) {
                router.resetShell();
                router.navigate(Route.WELCOME);
            }
            Toast.info("You have been logged out");
        });
    }
    
    public void setUnreadNotificationCount(int count) {
        if (notificationBadgeContainer != null) {
            notificationBadgeContainer.setVisible(count > 0);
            notificationBadgeContainer.setManaged(count > 0);
        }
        if (notificationCountLabel != null) {
            notificationCountLabel.setText(count > 99 ? "99+" : String.valueOf(count));
        }
    }
    
    /**
     * Refresh credits info from server - call this after plan changes
     */
    public void refreshCreditsInfo() {
        loadCreditsInfo();
    }
}
