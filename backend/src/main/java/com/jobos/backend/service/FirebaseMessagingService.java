package com.jobos.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.jobos.backend.config.FirebaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseMessagingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FirebaseMessagingService.class);
    private final FirebaseConfig firebaseConfig;

    public FirebaseMessagingService(FirebaseConfig firebaseConfig) {
        this.firebaseConfig = firebaseConfig;
    }

    public boolean sendPushNotification(String fcmToken, String title, String body, String actionUrl) {
        if (!firebaseConfig.isFirebaseInitialized()) {
            logger.warn("Firebase not initialized. Push notification not sent.");
            return false;
        }

        if (fcmToken == null || fcmToken.isEmpty()) {
            logger.debug("No FCM token provided. Push notification not sent.");
            return false;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            if (actionUrl != null && !actionUrl.isEmpty()) {
                messageBuilder.putData("actionUrl", actionUrl);
            }

            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent push notification: {}", response);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send push notification to token {}: {}", 
                    fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", 
                    e.getMessage());
            return false;
        }
    }

    public boolean sendPushNotificationWithData(String fcmToken, String title, String body, 
                                                 java.util.Map<String, String> data) {
        if (!firebaseConfig.isFirebaseInitialized()) {
            logger.warn("Firebase not initialized. Push notification not sent.");
            return false;
        }

        if (fcmToken == null || fcmToken.isEmpty()) {
            logger.debug("No FCM token provided. Push notification not sent.");
            return false;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent push notification with data: {}", response);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send push notification: {}", e.getMessage());
            return false;
        }
    }
}
