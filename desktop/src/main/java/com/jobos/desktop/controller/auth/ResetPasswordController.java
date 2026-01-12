package com.jobos.desktop.controller.auth;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.auth.ForgotPasswordRequest;
import com.jobos.shared.dto.auth.ResetPasswordRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class ResetPasswordController implements Initializable {
    
    @FXML private TextField codeField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;
    
    private final Router router = Router.getInstance();
    private final ApiClient apiClient = ApiClient.getInstance();
    private String email;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        email = router.getParam("email");
    }
    
    @FXML
    private void onBack() {
        router.navigate(Route.FORGOT_PASSWORD);
    }
    
    @FXML
    private void onSubmit() {
        String code = codeField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        if (code.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        if (password.length() < 8) {
            showError("Password must be at least 8 characters");
            return;
        }
        
        hideError();
        setLoading(true);
        
        ResetPasswordRequest request = new ResetPasswordRequest();
        if (email != null) {
            request.setEmail(email);
        }
        request.setOtp(code);
        request.setNewPassword(password);
        
        apiClient.post("/api/auth/reset-password", request, Object.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    Toast.success("Password reset successfully!");
                    router.navigate(Route.LOGIN);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    if (e.getCause() instanceof ApiClient.ApiException apiEx) {
                        if (apiEx.getStatusCode() == 400) {
                            showError("Invalid or expired reset code");
                        } else {
                            showError("Failed to reset password. Please try again.");
                        }
                    } else {
                        showError("Connection error. Please check your internet.");
                    }
                });
                return null;
            });
    }
    
    @FXML
    private void onResend() {
        if (email == null || email.isEmpty()) {
            router.navigate(Route.FORGOT_PASSWORD);
            return;
        }
        
        setLoading(true);
        
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        
        apiClient.post("/api/auth/forgot-password", request, Object.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    Toast.success("New reset code sent!");
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    Toast.error("Failed to resend code");
                });
                return null;
            });
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
    
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
    
    private void setLoading(boolean loading) {
        submitButton.setDisable(loading);
        codeField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        
        if (loading) {
            Toast.info("Resetting password...");
        }
    }
}
