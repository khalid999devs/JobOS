package com.jobos.desktop.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class WindowStateManager {
    private static final String STATE_FILE = "window-state.json";
    private final ObjectMapper mapper = new ObjectMapper();
    
    public void saveWindowState(Stage stage) {
        WindowState state = new WindowState(
            stage.getX(),
            stage.getY(),
            stage.getWidth(),
            stage.getHeight(),
            stage.isMaximized()
        );
        
        try {
            mapper.writeValue(new File(STATE_FILE), state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void restoreWindowState(Stage stage) {
        File file = new File(STATE_FILE);
        if (!file.exists()) {
            stage.setMaximized(true);
            return;
        }
        
        try {
            WindowState state = mapper.readValue(file, WindowState.class);
            
            if (isValidState(state)) {
                stage.setX(state.x);
                stage.setY(state.y);
                stage.setWidth(state.width);
                stage.setHeight(state.height);
                stage.setMaximized(state.maximized);
            } else {
                stage.setMaximized(true);
            }
        } catch (IOException e) {
            stage.setMaximized(true);
        }
    }
    
    private boolean isValidState(WindowState state) {
        return state.width >= AppConfig.MIN_WINDOW_WIDTH 
            && state.height >= AppConfig.MIN_WINDOW_HEIGHT
            && state.x >= -100 && state.y >= -100;
    }
    
    private static class WindowState {
        public double x;
        public double y;
        public double width;
        public double height;
        public boolean maximized;
        
        public WindowState() {}
        
        public WindowState(double x, double y, double width, double height, boolean maximized) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.maximized = maximized;
        }
    }
}
