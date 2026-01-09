package com.jobos.desktop.core.ui;

import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

public class LoadingOverlay {
    
    private static StackPane rootContainer;
    private static StackPane currentOverlay;
    
    public static void setContainer(StackPane root) {
        rootContainer = root;
    }
    
    public static void show() {
        show("Loading...");
    }
    
    public static void show(String message) {
        if (rootContainer == null || currentOverlay != null) return;
        
        currentOverlay = new StackPane();
        currentOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        currentOverlay.setAlignment(Pos.CENTER);
        
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        
        Arc spinner = new Arc();
        spinner.setRadiusX(24);
        spinner.setRadiusY(24);
        spinner.setStartAngle(0);
        spinner.setLength(270);
        spinner.setStyle("-fx-stroke: #1E88E5; -fx-stroke-width: 4; -fx-fill: transparent;");
        spinner.setStrokeLineCap(StrokeLineCap.ROUND);
        
        RotateTransition rotate = new RotateTransition(Duration.seconds(1), spinner);
        rotate.setByAngle(360);
        rotate.setCycleCount(RotateTransition.INDEFINITE);
        rotate.play();
        
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        
        content.getChildren().addAll(spinner, messageLabel);
        currentOverlay.getChildren().add(content);
        rootContainer.getChildren().add(currentOverlay);
    }
    
    public static void hide() {
        if (rootContainer != null && currentOverlay != null) {
            rootContainer.getChildren().remove(currentOverlay);
            currentOverlay = null;
        }
    }
    
    public static boolean isShowing() {
        return currentOverlay != null;
    }
}
