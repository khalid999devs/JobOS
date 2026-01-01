package com.jobos.desktop;

import com.jobos.desktop.core.config.AppConfig;
import com.jobos.desktop.core.config.WindowStateManager;
import com.jobos.desktop.core.navigation.NavigationManager;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.session.SessionManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class Main extends Application {
    private WindowStateManager windowStateManager;
    private Scene scene;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        windowStateManager = new WindowStateManager();
        
        // Start with welcome screen
        Parent root = NavigationManager.getInstance().navigate(Route.WELCOME);
        
        scene = new Scene(
            root,
            AppConfig.DEFAULT_WINDOW_WIDTH,
            AppConfig.DEFAULT_WINDOW_HEIGHT
        );
        
        scene.getStylesheets().add(
            getClass().getResource("/css/theme.css").toExternalForm()
        );
        
        // Listen for navigation changes
        NavigationManager.getInstance().setNavigationListener(this::onNavigate);
        
        primaryStage.setTitle(AppConfig.APP_TITLE);
        primaryStage.setMinWidth(AppConfig.MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(AppConfig.MIN_WINDOW_HEIGHT);
        primaryStage.setScene(scene);
        
        windowStateManager.restoreWindowState(primaryStage);
        
        primaryStage.setOnCloseRequest(e -> {
            windowStateManager.saveWindowState(primaryStage);
        });
        
        primaryStage.show();
    }
    
    private void onNavigate(Parent newRoot) {
        scene.setRoot(newRoot);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
