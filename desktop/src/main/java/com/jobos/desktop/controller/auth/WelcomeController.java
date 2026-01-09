package com.jobos.desktop.controller.auth;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import javafx.fxml.FXML;

public class WelcomeController {
    
    private final Router router = Router.getInstance();
    
    @FXML
    private void onSignIn() {
        router.navigate(Route.LOGIN);
    }
    
    @FXML
    private void onCreateAccount() {
        router.navigate(Route.REGISTER);
    }
}
