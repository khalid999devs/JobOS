package com.jobos.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);
    private boolean firebaseInitialized = false;

    @PostConstruct
    public void initialize() {
        String serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON_PATH");
        
        if (serviceAccountPath == null || serviceAccountPath.isEmpty()) {
            logger.warn("FIREBASE_SERVICE_ACCOUNT_JSON_PATH not set. Firebase features disabled.");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            
            FirebaseApp.initializeApp(options);
            firebaseInitialized = true;
            logger.info("Firebase initialized successfully");
        } catch (Exception e) {
            logger.warn("Failed to initialize Firebase: {}. Firebase features disabled.", e.getMessage());
        }
    }

    public boolean isFirebaseInitialized() {
        return firebaseInitialized;
    }
}
