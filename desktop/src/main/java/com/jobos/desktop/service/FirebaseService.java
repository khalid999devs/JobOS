package com.jobos.desktop.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.*;
import com.google.auth.oauth2.GoogleCredentials;
import javafx.application.Platform;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Consumer;

public class FirebaseService {
    private static final String DATABASE_URL = "https://jobos-4e21e-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static FirebaseService instance;
    private DatabaseReference notificationsRef;

    private FirebaseService() throws IOException {
        initializeFirebase();
    }

    public static FirebaseService getInstance() throws IOException {
        if (instance == null) {
            instance = new FirebaseService();
        }
        return instance;
    }

    private void initializeFirebase() throws IOException {
        String serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON_PATH");
        if (serviceAccountPath == null) {
            throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_JSON_PATH environment variable not set");
        }

        FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setDatabaseUrl(DATABASE_URL)
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }

    public void listenToNotifications(String userId, Consumer<NotificationData> onNotification, Consumer<Boolean> onConnectionChange) {
        // Connection state listener
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null) {
                    Platform.runLater(() -> onConnectionChange.accept(connected));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Platform.runLater(() -> onConnectionChange.accept(false));
            }
        });

        // Notifications listener
        notificationsRef = FirebaseDatabase.getInstance()
                .getReference("users/" + userId + "/notifications");

        notificationsRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName) {
                NotificationData notification = snapshot.getValue(NotificationData.class);
                if (notification != null) {
                    Platform.runLater(() -> onNotification.accept(notification));
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onChildRemoved(DataSnapshot snapshot) {}

            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName) {}

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Firebase error: " + error.getMessage());
            }
        });
    }

    public static class NotificationData {
        private String title;
        private String body;
        private Long createdAt;
        private Boolean read;

        public NotificationData() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }

        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

        public Boolean getRead() { return read; }
        public void setRead(Boolean read) { this.read = read; }
    }
}
