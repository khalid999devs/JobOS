package com.jobos.desktop.ui;

import com.jobos.desktop.core.ApiClient;
import com.jobos.shared.dto.common.PingResponse;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class AppView {
    private final ApiClient apiClient;
    private Label responseLabel;

    public AppView() {
        this.apiClient = new ApiClient("http://localhost:8080");
    }

    public void show(Stage stage) {
        responseLabel = new Label("Backend response will appear here");
        responseLabel.setStyle("-fx-font-size: 14px;");

        Button pingButton = new Button("Ping backend");
        pingButton.setOnAction(e -> pingBackend());

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getChildren().addAll(responseLabel, pingButton);

        Scene scene = new Scene(root, 400, 200);
        stage.setTitle("JobOS Desktop (Dev)");
        stage.setScene(scene);
        stage.show();
    }

    private void pingBackend() {
        try {
            PingResponse response = apiClient.ping();
            responseLabel.setText(response.getMessage());
        } catch (Exception e) {
            responseLabel.setText("Error: " + e.getMessage());
        }
    }
}
