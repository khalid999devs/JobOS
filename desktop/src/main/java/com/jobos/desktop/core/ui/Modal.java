package com.jobos.desktop.core.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.function.Consumer;

public class Modal {
    
    private static StackPane rootContainer;
    
    public static void setContainer(StackPane root) {
        rootContainer = root;
    }
    
    public static void show(String title, Node content) {
        show(title, content, null, null);
    }
    
    public static void show(String title, Node content, String confirmText, Runnable onConfirm) {
        show(title, content, confirmText, onConfirm, "Cancel", null);
    }
    
    public static void show(String title, Node content, String confirmText, Runnable onConfirm,
                            String cancelText, Runnable onCancel) {
        if (rootContainer == null) {
            System.err.println("Modal container not initialized");
            return;
        }
        
        StackPane overlay = createOverlay();
        VBox modal = createModal(title, content, confirmText, onConfirm, cancelText, onCancel, overlay);
        
        overlay.getChildren().add(modal);
        rootContainer.getChildren().add(overlay);
        
        animateIn(overlay, modal);
    }
    
    public static void confirm(String title, String message, Runnable onConfirm) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #B3B3B3; -fx-font-size: 14px;");
        
        show(title, messageLabel, "Confirm", onConfirm);
    }
    
    public static void alert(String title, String message) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: #B3B3B3; -fx-font-size: 14px;");
        
        show(title, messageLabel, "OK", () -> {}, null, null);
    }
    
    public static void input(String title, String placeholder, Consumer<String> onSubmit) {
        javafx.scene.control.TextField textField = new javafx.scene.control.TextField();
        textField.setPromptText(placeholder);
        textField.setPrefWidth(300);
        
        show(title, textField, "Submit", () -> onSubmit.accept(textField.getText()));
    }
    
    private static StackPane createOverlay() {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.6);");
        overlay.setAlignment(Pos.CENTER);
        return overlay;
    }
    
    private static VBox createModal(String title, Node content, String confirmText, Runnable onConfirm,
                                    String cancelText, Runnable onCancel, StackPane overlay) {
        VBox modal = new VBox(20);
        modal.setMaxWidth(480);
        modal.setMaxHeight(Region.USE_PREF_SIZE);
        modal.setAlignment(Pos.TOP_LEFT);
        modal.setPadding(new Insets(24));
        modal.setStyle("""
            -fx-background-color: #1E1E1E;
            -fx-background-radius: 12;
            -fx-border-color: #3D3D3D;
            -fx-border-radius: 12;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 16, 0, 0, 8);
            """);
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        Button closeButton = new Button("✕");
        closeButton.setStyle("""
            -fx-background-color: transparent;
            -fx-text-fill: #808080;
            -fx-font-size: 16px;
            -fx-cursor: hand;
            -fx-padding: 4 8;
            """);
        closeButton.setOnMouseEntered(e -> closeButton.setStyle(closeButton.getStyle() + "-fx-text-fill: white;"));
        closeButton.setOnMouseExited(e -> closeButton.setStyle(closeButton.getStyle().replace("-fx-text-fill: white;", "-fx-text-fill: #808080;")));
        closeButton.setOnAction(e -> close(overlay));
        
        header.getChildren().addAll(titleLabel, closeButton);
        
        VBox body = new VBox();
        body.getChildren().add(content);
        VBox.setVgrow(body, Priority.ALWAYS);
        
        modal.getChildren().addAll(header, body);
        
        if (confirmText != null || cancelText != null) {
            HBox footer = new HBox(12);
            footer.setAlignment(Pos.CENTER_RIGHT);
            
            if (cancelText != null) {
                Button cancelButton = createButton(cancelText, false);
                cancelButton.setOnAction(e -> {
                    if (onCancel != null) onCancel.run();
                    close(overlay);
                });
                footer.getChildren().add(cancelButton);
            }
            
            if (confirmText != null) {
                Button confirmButton = createButton(confirmText, true);
                confirmButton.setOnAction(e -> {
                    if (onConfirm != null) onConfirm.run();
                    close(overlay);
                });
                footer.getChildren().add(confirmButton);
            }
            
            modal.getChildren().add(footer);
        }
        
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                close(overlay);
            }
        });
        
        return modal;
    }
    
    private static Button createButton(String text, boolean primary) {
        Button button = new Button(text);
        if (primary) {
            button.setStyle("""
                -fx-background-color: #1E88E5;
                -fx-text-fill: white;
                -fx-font-weight: 500;
                -fx-padding: 10 20;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                """);
        } else {
            button.setStyle("""
                -fx-background-color: #2D2D2D;
                -fx-text-fill: #B3B3B3;
                -fx-font-weight: 500;
                -fx-padding: 10 20;
                -fx-background-radius: 8;
                -fx-border-color: #3D3D3D;
                -fx-border-radius: 8;
                -fx-cursor: hand;
                """);
        }
        return button;
    }
    
    private static void close(StackPane overlay) {
        if (rootContainer == null) return;
        
        FadeTransition fade = new FadeTransition(Duration.millis(150), overlay);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> rootContainer.getChildren().remove(overlay));
        fade.play();
    }
    
    private static void animateIn(StackPane overlay, VBox modal) {
        overlay.setOpacity(0);
        modal.setScaleX(0.9);
        modal.setScaleY(0.9);
        
        FadeTransition overlayFade = new FadeTransition(Duration.millis(200), overlay);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);
        
        ScaleTransition modalScale = new ScaleTransition(Duration.millis(200), modal);
        modalScale.setFromX(0.9);
        modalScale.setFromY(0.9);
        modalScale.setToX(1);
        modalScale.setToY(1);
        
        overlayFade.play();
        modalScale.play();
    }
}
