package com.jobos.desktop.core.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class Toast {
    
    public enum Type { SUCCESS, ERROR, WARNING, INFO }
    
    private static VBox container;
    
    public static void setContainer(StackPane root) {
        container = new VBox(8);
        container.setPickOnBounds(false);
        container.setAlignment(Pos.TOP_RIGHT);
        container.setMaxWidth(400);
        container.setTranslateX(-24);
        container.setTranslateY(24);
        
        StackPane.setAlignment(container, Pos.TOP_RIGHT);
        root.getChildren().add(container);
    }
    
    public static void show(String message) {
        show(message, Type.INFO);
    }
    
    public static void success(String message) {
        show(message, Type.SUCCESS);
    }
    
    public static void error(String message) {
        show(message, Type.ERROR);
    }
    
    public static void warning(String message) {
        show(message, Type.WARNING);
    }
    
    public static void info(String message) {
        show(message, Type.INFO);
    }
    
    public static void show(String message, Type type) {
        show(message, type, 4000);
    }
    
    public static void show(String message, Type type, int durationMs) {
        if (container == null) {
            System.err.println("Toast container not initialized");
            return;
        }
        
        HBox toast = createToast(message, type);
        container.getChildren().add(0, toast);
        
        animateIn(toast);
        
        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));
        pause.setOnFinished(e -> animateOut(toast));
        pause.play();
    }
    
    private static HBox createToast(String message, Type type) {
        HBox toast = new HBox(12);
        toast.setAlignment(Pos.CENTER_LEFT);
        toast.getStyleClass().addAll("toast", "toast-" + type.name().toLowerCase());
        
        Label icon = new Label(getIcon(type));
        icon.getStyleClass().add("toast-icon");
        
        Label text = new Label(message);
        text.getStyleClass().add("toast-message");
        text.setWrapText(true);
        
        toast.getChildren().addAll(icon, text);
        
        toast.setStyle(getToastStyle(type));
        
        return toast;
    }
    
    private static String getIcon(Type type) {
        return switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "✕";
            case WARNING -> "⚠";
            case INFO -> "ℹ";
        };
    }
    
    private static String getToastStyle(Type type) {
        String baseStyle = "-fx-background-color: #1E1E1E; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 12 16; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1 1 1 4; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 8, 0, 0, 4);";
        
        String borderColor = switch (type) {
            case SUCCESS -> "#4CAF50";
            case ERROR -> "#F44336";
            case WARNING -> "#FFC107";
            case INFO -> "#2196F3";
        };
        
        return baseStyle + "-fx-border-color: #3D3D3D #3D3D3D #3D3D3D " + borderColor + ";";
    }
    
    private static void animateIn(HBox toast) {
        toast.setOpacity(0);
        toast.setTranslateX(50);
        
        FadeTransition fade = new FadeTransition(Duration.millis(200), toast);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition translate = new TranslateTransition(Duration.millis(200), toast);
        translate.setFromX(50);
        translate.setToX(0);
        
        fade.play();
        translate.play();
    }
    
    private static void animateOut(HBox toast) {
        FadeTransition fade = new FadeTransition(Duration.millis(200), toast);
        fade.setFromValue(1);
        fade.setToValue(0);
        
        TranslateTransition translate = new TranslateTransition(Duration.millis(200), toast);
        translate.setFromX(0);
        translate.setToX(50);
        
        fade.setOnFinished(e -> container.getChildren().remove(toast));
        
        fade.play();
        translate.play();
    }
}
