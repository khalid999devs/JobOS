package com.jobos.desktop.core.ui;

import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
        currentOverlay.setStyle("-fx-background-color: rgba(250, 250, 250, 0.95);");
        currentOverlay.setAlignment(Pos.CENTER);
        
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: white; -fx-padding: 32; -fx-background-radius: 16;");
        content.setMaxWidth(200);
        content.setMaxHeight(150);
        
        // Add shadow effect
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setOffsetY(4);
        shadow.setColor(Color.rgb(0, 0, 0, 0.15));
        content.setEffect(shadow);
        
        // Use ProgressIndicator for a nicer look
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        spinner.setStyle("-fx-progress-color: #0F766E;");
        
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-font-weight: 500;");
        
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
