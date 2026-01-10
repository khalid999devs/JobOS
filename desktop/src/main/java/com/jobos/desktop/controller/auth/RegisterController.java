package com.jobos.desktop.controller.auth;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.auth.AuthResponse;
import com.jobos.shared.dto.auth.RegisterRequest;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {
    
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    
    private final Router router = Router.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ApiClient apiClient = ApiClient.getInstance();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleComboBox.setItems(FXCollections.observableArrayList("Job Seeker", "Job Poster"));
    }
    
    @FXML
    private void onBack() {
        router.navigate(Route.WELCOME);
    }
    
    @FXML
    private void onRegister() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String selectedRole = roleComboBox.getValue();
        
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }
        
        if (selectedRole == null) {
            showError("Please select your role");
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
        
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }
        
        hideError();
        setLoading(true);
        
        String role = selectedRole.equals("Job Seeker") ? "SEEKER" : "POSTER";
        
        RegisterRequest request = new RegisterRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(role);
        
        apiClient.post("/api/auth/register", request, ApiResponse.class)
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
                        Toast.success("Account created successfully!");
                        if (profileCompleted) {
                            router.navigateToRoleDashboard();
                        } else {
                            router.navigate(Route.PROFILE_SETUP);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError("Registration failed. Please try again.");
                    });
                }
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    if (e.getCause() instanceof ApiClient.ApiException apiEx) {
                        if (apiEx.getStatusCode() == 409) {
                            showError("An account with this email already exists");
                        } else if (apiEx.getStatusCode() == 400) {
                            showError("Invalid registration data");
                        } else {
                            showError("Registration failed. Please try again.");
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
    private void onSignIn() {
        router.navigate(Route.LOGIN);
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
        registerButton.setDisable(loading);
        firstNameField.setDisable(loading);
        lastNameField.setDisable(loading);
        emailField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        roleComboBox.setDisable(loading);
        
        if (loading) {
            Toast.info("Creating account...");
        }
    }
    
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
