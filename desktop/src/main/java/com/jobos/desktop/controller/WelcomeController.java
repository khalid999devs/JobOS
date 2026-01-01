package com.jobos.desktop.controller;

import com.jobos.desktop.core.navigation.NavigationManager;
import com.jobos.desktop.core.navigation.Route;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class WelcomeController {
    
    @FXML
    private Button loginButton;
    
    @FXML
    private Button registerButton;
    
    @FXML
    private void handleLogin() {
        NavigationManager.getInstance().navigate(Route.LOGIN);
    }
    
    @FXML
    private void handleRegister() {
        NavigationManager.getInstance().navigate(Route.REGISTER);
    }
}
