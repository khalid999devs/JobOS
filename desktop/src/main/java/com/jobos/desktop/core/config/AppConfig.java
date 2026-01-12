package com.jobos.desktop.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    
    public static final String APP_TITLE = "JobOS";
    public static final int MIN_WINDOW_WIDTH = 860;
    public static final int MIN_WINDOW_HEIGHT = 600;
    public static final int DEFAULT_WINDOW_WIDTH = 1280;
    public static final int DEFAULT_WINDOW_HEIGHT = 800;
    
    private static AppConfig instance;
    private final Properties properties;
    
    private AppConfig() {
        properties = new Properties();
        loadProperties();
    }
    
    public static AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }
    
    private void loadProperties() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("Failed to load app.properties");
        }
    }
    
    public String getApiBaseUrl() {
        String envUrl = System.getenv("JOBOS_API_BASE_URL");
        if (envUrl != null && !envUrl.isEmpty()) {
            return envUrl;
        }
        return properties.getProperty("api.baseUrl", "http://localhost:8080");
    }
    
    public int getPollingIntervalSeconds() {
        String envInterval = System.getenv("JOBOS_POLL_NOTIF_SECONDS");
        if (envInterval != null && !envInterval.isEmpty()) {
            try {
                return Integer.parseInt(envInterval);
            } catch (NumberFormatException ignored) {}
        }
        return Integer.parseInt(properties.getProperty("polling.notificationsSeconds", "30"));
    }
}
