package com.jobos.desktop.core.navigation;

import com.jobos.desktop.core.session.SessionManager;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Router {
    
    private static Router instance;
    
    private final RouteGuard routeGuard;
    private final Map<String, Object> routeParams;
    
    private Route currentRoute;
    private Consumer<Parent> sceneUpdater;
    private BorderPane appShell;
    private StackPane contentArea;
    private Consumer<String> titleUpdater;
    private boolean shellInitialized = false;
    
    private Router() {
        this.routeGuard = RouteGuard.getInstance();
        this.routeParams = new HashMap<>();
    }
    
    public static Router getInstance() {
        if (instance == null) {
            instance = new Router();
        }
        return instance;
    }
    
    public void setSceneUpdater(Consumer<Parent> updater) {
        this.sceneUpdater = updater;
    }
    
    public void setTitleUpdater(Consumer<String> updater) {
        this.titleUpdater = updater;
    }
    
    public void navigate(Route route) {
        navigate(route, (Map<String, Object>) null);
    }
    
    public void navigate(Route route, String singleParam) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", singleParam);
        navigate(route, params);
    }
    
    public void navigate(Route route, Map<String, Object> params) {
        if (route == currentRoute && route.showShell()) {
            return;
        }
        
        RouteGuard.RouteGuardResult guardResult = routeGuard.canActivate(route);
        
        if (!guardResult.isAllowed()) {
            Route redirectRoute = guardResult.getRedirectRoute();
            if (redirectRoute != null && redirectRoute != route) {
                navigate(redirectRoute);
            }
            return;
        }
        
        routeParams.clear();
        if (params != null) {
            routeParams.putAll(params);
        }
        
        currentRoute = route;
        
        try {
            if (route.showShell()) {
                loadShellRoute(route);
            } else {
                loadPublicRoute(route);
            }
        } catch (IOException e) {
            System.err.println("Failed to load route: " + route.getPath());
        }
    }
    
    private void loadShellRoute(Route route) throws IOException {
        Parent content = loadFxml(route);
        
        if (!shellInitialized || appShell == null || contentArea == null) {
            loadAppShell();
            shellInitialized = true;
        }
        
        contentArea.getChildren().clear();
        contentArea.getChildren().add(content);
        
        if (titleUpdater != null) {
            titleUpdater.accept(route.getTitle());
        }
        
        if (sceneUpdater != null && !isShellDisplayed()) {
            sceneUpdater.accept(appShell);
        }
    }
    
    private void loadPublicRoute(Route route) throws IOException {
        Parent content = loadFxml(route);
        
        shellInitialized = false;
        appShell = null;
        contentArea = null;
        titleUpdater = null;
        
        if (sceneUpdater != null) {
            sceneUpdater.accept(content);
        }
    }
    
    private boolean isShellDisplayed() {
        return appShell != null && appShell.getScene() != null;
    }
    
    private void loadAppShell() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/shell/app-shell.fxml"));
        appShell = loader.load();
        
        Object controller = loader.getController();
        if (controller instanceof AppShellAware aware) {
            aware.setRouter(this);
            contentArea = aware.getContentArea();
            titleUpdater = aware::setTitle;
        }
    }
    
    private Parent loadFxml(Route route) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(route.getFxmlPath()));
        return loader.load();
    }
    
    public Route getCurrentRoute() {
        return currentRoute;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getParam(String key) {
        return (T) routeParams.get(key);
    }
    
    public void navigateToRoleDashboard() {
        SessionManager session = SessionManager.getInstance();
        Route dashboard = routeGuard.getDefaultRouteForRole(session.getUserRole());
        navigate(dashboard);
    }
    
    public void resetShell() {
        shellInitialized = false;
        appShell = null;
        contentArea = null;
    }
    
    public interface AppShellAware {
        void setRouter(Router router);
        StackPane getContentArea();
        void setTitle(String title);
    }
}
