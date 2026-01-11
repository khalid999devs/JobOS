package com.jobos.desktop.controller.auth;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.auth.AuthResponse;
import com.jobos.shared.dto.auth.LoginRequest;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    
    private final Router router = Router.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ApiClient apiClient = ApiClient.getInstance();
    
    @FXML
    private void onBack() {
        router.navigate(Route.WELCOME);
    }
    
    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        
        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        hideError();
        setLoading(true);
        
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        
        apiClient.post("/api/auth/login", request, ApiResponse.class)
            .thenAccept(response -> {
                @SuppressWarnings("unchecked")
                ApiResponse<AuthResponse> authResp = (ApiResponse<AuthResponse>) response;
                if (authResp.isSuccess() && authResp.getResult() != null) {
                    AuthResponse auth = parseAuthResponse(authResp);
                    boolean profileCompleted = auth.getProfileCompleted() != null && auth.getProfileCompleted();
                    
                    sessionManager.login(
                        auth.getUserId(),
                        auth.getEmail(),
                        auth.getRole(),
                        auth.getAccessToken(),
                        auth.getRefreshToken(),
                        profileCompleted
                    );
                    
                    Platform.runLater(() -> {
                        setLoading(false);
                        Toast.success("Welcome back!");
                        if (profileCompleted) {
                            router.navigateToRoleDashboard();
                        } else {
                            router.navigate(Route.PROFILE_SETUP);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("Login failed. Please try again.");
                    });
                }
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    
                    // Unwrap the exception
                    Throwable cause = e.getCause();
                    if (cause == null) cause = e;
                    
                    if (cause instanceof ApiClient.ApiException apiEx) {
                        if (apiEx.getStatusCode() == 401) {
                            showError("Invalid email or password");
                        } else if (apiEx.getStatusCode() >= 500) {
                            showError("Server error. Please try again later.");
                        } else {
                            showError(apiEx.getMessage());
                        }
                    } else {
                        showError("Connection error. Please check your internet.");
                    }
                });
                return null;
            });
    }
    
    @SuppressWarnings("unchecked")
    private AuthResponse parseAuthResponse(ApiResponse<?> response) {
        Object result = response.getResult();
        if (result instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) result;
            AuthResponse auth = new AuthResponse();
            auth.setAccessToken((String) map.get("accessToken"));
            auth.setRefreshToken((String) map.get("refreshToken"));
            auth.setUserId((String) map.get("userId"));
            auth.setEmail((String) map.get("email"));
            auth.setRole((String) map.get("role"));
            auth.setProfileCompleted((Boolean) map.get("profileCompleted"));
            return auth;
        }
        return (AuthResponse) result;
    }
    
    @FXML
    private void onForgotPassword() {
        router.navigate(Route.FORGOT_PASSWORD);
    }
    
    @FXML
    private void onCreateAccount() {
        router.navigate(Route.REGISTER);
    }
    
    @FXML
    private void onSignUp() {
        router.navigate(Route.REGISTER);
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
        loginButton.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        
        if (loading) {
            Toast.info("Signing in...");
        }
    }
}
