package com.jobos.desktop.controller;

import com.jobos.desktop.service.ApiService;
import com.jobos.desktop.service.FirebaseService;
import com.jobos.shared.dto.common.PingResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainController {
    private static final String USER_ID = "test-user-123";

    @FXML
    private Label responseLabel;

    @FXML
    private Button pingButton;

    @FXML
    private TextArea notificationsArea;

    private final ApiService apiService;
    private final SimpleDateFormat dateFormat;
    private final StringBuilder notificationList;

    public MainController() {
        this.apiService = new ApiService();
        this.dateFormat = new SimpleDateFormat("HH:mm:ss");
        this.notificationList = new StringBuilder();
    }

    @FXML
    private void initialize() {
        responseLabel.setText("Backend response will appear here");
        notificationsArea.setText("Waiting for notifications...");
        setupFirebaseListener();
    }

    private void setupFirebaseListener() {
        new Thread(() -> {
            try {
                FirebaseService firebaseService = FirebaseService.getInstance();
                firebaseService.listenToNotifications(USER_ID, 
                    notification -> {
                        String time = dateFormat.format(new Date(
                            notification.getCreatedAt() != null ? notification.getCreatedAt() : System.currentTimeMillis()
                        ));
                        String newNotif = "[" + time + "] " + notification.getTitle() + "\n" +
                                         notification.getBody() + "\n\n";
                        
                        notificationList.insert(0, newNotif);
                        notificationsArea.setText(notificationList.toString());
                    },
                    connected -> {
                        responseLabel.setText(connected ? "Firebase: Connected" : "Firebase: Disconnected");
                    }
                );
            } catch (Exception e) {
                Platform.runLater(() -> {
                    responseLabel.setText("Firebase Error: " + e.getMessage());
                    notificationsArea.setText("Failed to connect to Firebase");
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handlePingButton() {
        pingButton.setDisable(true);
        responseLabel.setText("Connecting...");

        new Thread(() -> {
            try {
                PingResponse response = apiService.ping();
                Platform.runLater(() -> {
                    responseLabel.setText(response.getMessage());
                    responseLabel.getStyleClass().remove("error");
                    responseLabel.getStyleClass().add("success");
                    pingButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    responseLabel.setText("Error: " + e.getMessage());
                    responseLabel.getStyleClass().remove("success");
                    responseLabel.getStyleClass().add("error");
                    pingButton.setDisable(false);
                });
            }
        }).start();
    }
}
