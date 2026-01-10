package com.jobos.desktop.controller.common;

import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.ui.Modal;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApiClient;
import com.jobos.desktop.service.NotificationService;
import com.jobos.shared.dto.common.ApiResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class SettingsController implements Initializable {
    
    // Appearance
    @FXML private ComboBox<String> themeCombo;
    
    // Notifications
    @FXML private CheckBox desktopNotificationsCheck;
    @FXML private CheckBox emailNotificationsCheck;
    @FXML private CheckBox jobAlertsCheck;
    
    // Security
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label passwordErrorLabel;
    @FXML private Button changePasswordButton;
    
    // About
    @FXML private Label versionLabel;
    
    // Danger Zone
    @FXML private VBox dangerZone;
    
    private final ApiClient apiClient = ApiClient.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final NotificationService notificationService = NotificationService.getInstance();
    private final Preferences prefs = Preferences.userNodeForPackage(SettingsController.class);
    
    private static final String PREF_THEME = "theme";
    private static final String PREF_DESKTOP_NOTIFICATIONS = "desktop_notifications";
    private static final String PREF_EMAIL_NOTIFICATIONS = "email_notifications";
    private static final String PREF_JOB_ALERTS = "job_alerts";
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupThemeOptions();
        loadPreferences();
        setupListeners();
        versionLabel.setText("1.0.0");
        hideErrors();
    }
    
    private void setupThemeOptions() {
        themeCombo.setItems(FXCollections.observableArrayList(
            "Light", "Dark", "System Default"
        ));
    }
    
    private void loadPreferences() {
        // Theme
        String theme = prefs.get(PREF_THEME, "System Default");
        themeCombo.setValue(theme);
        
        // Notifications
        desktopNotificationsCheck.setSelected(prefs.getBoolean(PREF_DESKTOP_NOTIFICATIONS, true));
        emailNotificationsCheck.setSelected(prefs.getBoolean(PREF_EMAIL_NOTIFICATIONS, true));
        jobAlertsCheck.setSelected(prefs.getBoolean(PREF_JOB_ALERTS, true));
    }
    
    private void setupListeners() {
        themeCombo.setOnAction(e -> {
            String theme = themeCombo.getValue();
            prefs.put(PREF_THEME, theme);
            Toast.success("Theme changed to " + theme);
            // Note: Theme switching is visual only in current implementation
        });
        
        desktopNotificationsCheck.setOnAction(e -> {
            boolean enabled = desktopNotificationsCheck.isSelected();
            prefs.putBoolean(PREF_DESKTOP_NOTIFICATIONS, enabled);
            
            // Control notification polling based on setting
            if (enabled) {
                notificationService.startPolling(30);
                Toast.success("Desktop notifications enabled");
            } else {
                notificationService.stopPolling();
                Toast.info("Desktop notifications disabled");
            }
            updateNotificationPreferences();
        });
        
        emailNotificationsCheck.setOnAction(e -> {
            boolean enabled = emailNotificationsCheck.isSelected();
            prefs.putBoolean(PREF_EMAIL_NOTIFICATIONS, enabled);
            Toast.success(enabled ? "Email notifications enabled" : "Email notifications disabled");
            updateNotificationPreferences();
        });
        
        jobAlertsCheck.setOnAction(e -> {
            boolean enabled = jobAlertsCheck.isSelected();
            prefs.putBoolean(PREF_JOB_ALERTS, enabled);
            Toast.success(enabled ? "Job alerts enabled" : "Job alerts disabled");
            updateNotificationPreferences();
        });
    }
    
    private void updateNotificationPreferences() {
        Map<String, Object> preferences = Map.of(
            "desktopNotifications", desktopNotificationsCheck.isSelected(),
            "emailNotifications", emailNotificationsCheck.isSelected(),
            "jobAlerts", jobAlertsCheck.isSelected()
        );
        
        apiClient.put("/api/users/me/preferences", preferences, ApiResponse.class)
            .thenAccept(response -> Platform.runLater(() -> 
                Toast.success("Preferences saved")))
            .exceptionally(e -> {
                Platform.runLater(() -> Toast.error("Failed to save preferences"));
                return null;
            });
    }
    
    @FXML
    private void onChangePassword() {
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (currentPassword.isEmpty()) {
            showPasswordError("Current password is required");
            return;
        }
        
        if (newPassword.isEmpty()) {
            showPasswordError("New password is required");
            return;
        }
        
        if (newPassword.length() < 8) {
            showPasswordError("New password must be at least 8 characters");
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            showPasswordError("Passwords do not match");
            return;
        }
        
        hideErrors();
        changePasswordButton.setDisable(true);
        Toast.info("Changing password...");
        
        Map<String, String> request = Map.of(
            "currentPassword", currentPassword,
            "newPassword", newPassword
        );
        
        apiClient.post("/api/auth/change-password", request, ApiResponse.class)
            .thenAccept(response -> Platform.runLater(() -> {
                changePasswordButton.setDisable(false);
                Toast.success("Password changed successfully");
                currentPasswordField.clear();
                newPasswordField.clear();
                confirmPasswordField.clear();
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    changePasswordButton.setDisable(false);
                    showPasswordError("Failed to change password. Please check your current password.");
                });
                return null;
            });
    }
    
    @FXML
    private void onPrivacyPolicy() {
        openUrl("https://jobos.com/privacy");
    }
    
    @FXML
    private void onTermsOfService() {
        openUrl("https://jobos.com/terms");
    }
    
    @FXML
    private void onHelpCenter() {
        openUrl("https://jobos.com/help");
    }
    
    private void openUrl(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            Toast.error("Failed to open link");
        }
    }
    
    @FXML
    private void onDeleteAccount() {
        Modal.confirm("Delete Account", 
            "Are you sure you want to delete your account? This action cannot be undone.",
            () -> {
                Toast.info("Deleting account...");
                
                apiClient.delete("/api/users/me")
                    .thenAccept(response -> Platform.runLater(() -> {
                        Toast.success("Account deleted");
                        sessionManager.logout();
                    }))
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            Toast.error("Failed to delete account");
                        });
                        return null;
                    });
            });
    }
    
    private void showPasswordError(String message) {
        passwordErrorLabel.setText(message);
        passwordErrorLabel.setVisible(true);
        passwordErrorLabel.setManaged(true);
    }
    
    private void hideErrors() {
        passwordErrorLabel.setVisible(false);
        passwordErrorLabel.setManaged(false);
    }
}
