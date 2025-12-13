package com.jobos.backend.service;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jobos.backend.config.FirebaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final FirebaseConfig firebaseConfig;

    public NotificationService(FirebaseConfig firebaseConfig) {
        this.firebaseConfig = firebaseConfig;
    }

    public void publishUserNotification(String userId, String title, String body) {
        if (!firebaseConfig.isFirebaseInitialized()) {
            logger.debug("Firebase not initialized, skipping notification");
            return;
        }

        try {
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("notifications")
                    .push();

            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("createdAt", System.currentTimeMillis());
            notification.put("read", false);

            ref.setValueAsync(notification);
            logger.info("Published notification to user {}", userId);
        } catch (Exception e) {
            logger.error("Failed to publish notification: {}", e.getMessage());
        }
    }
}
