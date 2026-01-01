package com.jobos.desktop.controller;

import com.jobos.desktop.core.navigation.NavigationManager;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.model.UserRole;
import com.jobos.desktop.service.ApiService;
import com.jobos.shared.dto.auth.LoginRequest;
import com.jobos.shared.dto.auth.AuthResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

public class LoginController {
    
    @FXML
    private TextField emailField;
    
    @FXML
    private PasswordField passwordField;
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Label forgotPasswordLink;
    
    @FXML
    private Label registerLink;
    
    private final ApiService apiService;
    
    public LoginController() {
        this.apiService = new ApiService();
    }
    
    @FXML
    private void initialize() {
        // Setup hover effects for links
        setupLinkHover(forgotPasswordLink);
        setupLinkHover(registerLink);
    }
    
    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        // Prototype mode - no backend integration yet
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");
        errorLabel.setVisible(false);
        
        // Simulate login delay
        new Thread(() -> {
            try {
                Thread.sleep(500); // Brief delay for UX
                
                Platform.runLater(() -> {
                    // Mock successful login
                    SessionManager.getInstance().login(
                        "mock-access-token",
                        "mock-refresh-token",
                        "user-123",
                        email,
                        UserRole.JOB_SEEKER
                    );
                    
                    // Navigate to dashboard
                    NavigationManager.getInstance().navigate(Route.SEEKER_DASHBOARD);
                });
            } catch (InterruptedException e) {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");
                });
            }
        }).start();
    }
    
    @FXML
    private void handleForgotPassword(MouseEvent event) {
        NavigationManager.getInstance().navigate(Route.FORGOT_PASSWORD);
    }
    
    @FXML
    private void handleRegisterLink(MouseEvent event) {
        NavigationManager.getInstance().navigate(Route.REGISTER);
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
    
    private void setupLinkHover(Label link) {
        link.setOnMouseEntered(e -> link.setStyle("-fx-underline: true;"));
        link.setOnMouseExited(e -> link.setStyle("-fx-underline: false;"));
    }
}
