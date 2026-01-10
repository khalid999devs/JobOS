package com.jobos.desktop.core.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class WindowStateManager {
    
    private static final String STATE_FILE = "window-state.json";
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final File stateFile;
    private WindowState state;
    
    public WindowStateManager(String appDir) {
        this.stateFile = new File(appDir, STATE_FILE);
        this.state = loadState();
    }
    
    private WindowState loadState() {
        if (stateFile.exists()) {
            try {
                return mapper.readValue(stateFile, WindowState.class);
            } catch (IOException e) {
                // Ignore load error
            }
        }
        return new WindowState();
    }
    
    public void saveState() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(stateFile, state);
        } catch (IOException e) {
            // Silently handle error
        }
    }
    
    public void applyTo(Stage stage) {
        stage.setWidth(state.width);
        stage.setHeight(state.height);
        
        if (state.x != null && state.y != null) {
            stage.setX(state.x);
            stage.setY(state.y);
        } else {
            stage.centerOnScreen();
        }
        
        stage.setMaximized(state.maximized);
    }
    
    public void captureFrom(Stage stage) {
        if (!stage.isMaximized()) {
            state.width = stage.getWidth();
            state.height = stage.getHeight();
            state.x = stage.getX();
            state.y = stage.getY();
        }
        state.maximized = stage.isMaximized();
    }
    
    public void bindTo(Stage stage) {
        stage.widthProperty().addListener((obs, old, val) -> captureFrom(stage));
        stage.heightProperty().addListener((obs, old, val) -> captureFrom(stage));
        stage.xProperty().addListener((obs, old, val) -> captureFrom(stage));
        stage.yProperty().addListener((obs, old, val) -> captureFrom(stage));
        stage.maximizedProperty().addListener((obs, old, val) -> captureFrom(stage));
        
        stage.setOnCloseRequest(e -> saveState());
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WindowState {
        public double width = 1200;
        public double height = 800;
        public Double x;
        public Double y;
        public boolean maximized = false;
    }
}
