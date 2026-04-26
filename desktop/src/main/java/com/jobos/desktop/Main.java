package com.jobos.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.desktop.core.config.AppConfig;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.state.WindowStateManager;
import com.jobos.desktop.core.ui.Dialogs;
import com.jobos.desktop.core.ui.LoadingOverlay;
import com.jobos.desktop.core.ui.Modal;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {
    
    private Stage primaryStage;
    private StackPane rootContainer;
    private WindowStateManager windowStateManager;
    
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        
        AppConfig.getInstance();
        
        rootContainer = new StackPane();
        rootContainer.getStyleClass().add("root");
        
        Scene scene = new Scene(rootContainer);
        scene.getStylesheets().addAll(
            Objects.requireNonNull(getClass().getResource("/css/theme.css")).toExternalForm(),
            Objects.requireNonNull(getClass().getResource("/css/components.css")).toExternalForm()
        );
        
        Toast.setContainer(rootContainer);
        Dialogs.setContainer(rootContainer);
        Modal.setContainer(rootContainer);
        LoadingOverlay.setContainer(rootContainer);
        
        Router router = Router.getInstance();
        router.setSceneUpdater(this::updateRoot);
        router.setTitleUpdater(this::updateTitle);
        
        stage.setTitle("JobOS");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.setScene(scene);
        
        try {
            Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/icon.png")));
            stage.getIcons().add(icon);
        } catch (Exception ignored) {}
        
        windowStateManager = new WindowStateManager(System.getProperty("user.dir"));
        windowStateManager.applyTo(stage);
        windowStateManager.bindTo(stage);
        
        stage.show();
        
        initializeApp();
    }
    
    private void initializeApp() {
        SessionManager session = SessionManager.getInstance();
        Router router = Router.getInstance();
        
        if (session.hasStoredSession()) {
            if (session.getUserRole() != null) {
                router.navigateToRoleDashboard();
                refreshProfileInBackground();
            } else {
                LoadingOverlay.show("Restoring session...");
                fetchProfileAndNavigate(session, router);
            }
        } else {
            router.navigate(Route.WELCOME);
        }
    }
    
    private void refreshProfileInBackground() {
        SessionManager session = SessionManager.getInstance();
        ApiClient.getInstance().get("/api/users/me", new TypeReference<ProfileResponse>() {})
            .thenAccept(profile -> {
                if (profile != null) {
                    Platform.runLater(() -> session.updateProfile(profile));
                }
            })
            .exceptionally(e -> null);
    }
    
    private void fetchProfileAndNavigate(SessionManager session, Router router) {
        ApiClient.getInstance().get("/api/users/me", new TypeReference<ProfileResponse>() {})
            .thenAccept(profile -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    if (profile != null) {
                        session.updateProfile(profile);
                        router.navigateToRoleDashboard();
                    } else {
                        router.navigate(Route.WELCOME);
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    router.navigate(Route.WELCOME);
                });
                return null;
            });
    }
    
    private void updateRoot(Parent root) {
        rootContainer.getChildren().clear();
        rootContainer.getChildren().add(root);
    }
    
    private void updateTitle(String title) {
        primaryStage.setTitle(title + " - JobOS");
    }
    
    @Override
    public void stop() {
        if (windowStateManager != null) {
            windowStateManager.saveState();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
