package com.jobos.desktop.controller.auth;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.LoadingOverlay;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.auth.ForgotPasswordRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ForgotPasswordController {
    
    @FXML private TextField emailField;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private Button submitButton;
    
    private final Router router = Router.getInstance();
    private final ApiClient apiClient = ApiClient.getInstance();
    
    @FXML
    private void onBack() {
        router.navigate(Route.LOGIN);
    }
    
    @FXML
    private void onSubmit() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            showError("Please enter your email");
            return;
        }
        
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }
        
        hideMessages();
        setLoading(true);
        
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        
        apiClient.post("/api/auth/forgot-password", request, Object.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    showSuccess("Reset code sent! Check your email for instructions.");
                    
                    java.util.concurrent.CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS)
                        .execute(() -> Platform.runLater(() -> {
                            router.navigate(Route.RESET_PASSWORD, Map.of("email", email));
                        }));
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    if (e.getCause() instanceof ApiClient.ApiException apiEx) {
                        if (apiEx.getStatusCode() == 404) {
                            showError("No account found with this email");
                        } else {
                            showError("Failed to send reset code. Please try again.");
                        }
                    } else {
                        showError("Connection error. Please check your internet.");
                    }
                });
                return null;
            });
    }
    
    @FXML
    private void onSignIn() {
        router.navigate(Route.LOGIN);
    }
    
    private void showError(String message) {
        successLabel.setVisible(false);
        successLabel.setManaged(false);
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
    
    private void showSuccess(String message) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setText(message);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }
    
    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
    
    private void setLoading(boolean loading) {
        submitButton.setDisable(loading);
        emailField.setDisable(loading);
        
        if (loading) {
            LoadingOverlay.show("Sending reset code...");
        } else {
            LoadingOverlay.hide();
        }
    }
    
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
