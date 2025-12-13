package com.jobos.desktop.controller;

import com.jobos.desktop.service.ApiService;
import com.jobos.shared.dto.common.PingResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class MainController {

    @FXML
    private Label responseLabel;

    @FXML
    private Button pingButton;

    private final ApiService apiService;

    public MainController() {
        this.apiService = new ApiService();
    }

    @FXML
    private void initialize() {
        responseLabel.setText("Backend response will appear here");
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
