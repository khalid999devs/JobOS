package com.jobos.desktop.core.ui;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class SkeletonLoader {
    
    private static final String SKELETON_BASE = "#E5E7EB";
    
    public static VBox createInlineLoader(String message) {
        VBox loader = new VBox(12);
        loader.setAlignment(Pos.CENTER);
        loader.setPadding(new Insets(32));
        loader.setStyle("-fx-background-color: rgba(250, 250, 250, 0.9);");
        
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(36, 36);
        spinner.setStyle("-fx-progress-color: #0F766E;");
        
        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        loader.getChildren().addAll(spinner, label);
        return loader;
    }
    
    public static VBox createSkeletonCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #E5E7EB; -fx-border-radius: 12;");
        
        Region titleSkeleton = createSkeletonRect(200, 20);
        Region subtitleSkeleton = createSkeletonRect(150, 14);
        
        HBox metaRow = new HBox(16);
        metaRow.getChildren().addAll(
            createSkeletonRect(80, 14),
            createSkeletonRect(100, 14)
        );
        
        card.getChildren().addAll(titleSkeleton, subtitleSkeleton, metaRow);
        animateSkeleton(card);
        return card;
    }
    
    public static VBox createSkeletonList(int count) {
        VBox list = new VBox(12);
        for (int i = 0; i < count; i++) {
            list.getChildren().add(createSkeletonCard());
        }
        return list;
    }
    
    public static VBox createSkeletonStatCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        
        HBox content = new HBox(12);
        content.setAlignment(Pos.CENTER_LEFT);
        
        Region iconSkeleton = createSkeletonRect(44, 44);
        iconSkeleton.setStyle(iconSkeleton.getStyle() + "-fx-background-radius: 10;");
        
        VBox textBox = new VBox(6);
        textBox.getChildren().addAll(
            createSkeletonRect(60, 24),
            createSkeletonRect(80, 14)
        );
        
        content.getChildren().addAll(iconSkeleton, textBox);
        card.getChildren().add(content);
        animateSkeleton(card);
        return card;
    }
    
    public static HBox createSkeletonStatsRow(int count) {
        HBox row = new HBox(20);
        for (int i = 0; i < count; i++) {
            VBox stat = createSkeletonStatCard();
            HBox.setHgrow(stat, Priority.ALWAYS);
            row.getChildren().add(stat);
        }
        return row;
    }
    
    public static HBox createSkeletonApplicantRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        
        Region avatar = createSkeletonRect(40, 40);
        avatar.setStyle(avatar.getStyle() + "-fx-background-radius: 20;");
        
        VBox info = new VBox(4);
        info.getChildren().addAll(
            createSkeletonRect(120, 16),
            createSkeletonRect(160, 12)
        );
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Region badge = createSkeletonRect(70, 24);
        badge.setStyle(badge.getStyle() + "-fx-background-radius: 4;");
        
        row.getChildren().addAll(avatar, info, badge);
        animateSkeleton(row);
        return row;
    }
    
    public static VBox createSkeletonApplicantsList(int count) {
        VBox list = new VBox(8);
        for (int i = 0; i < count; i++) {
            list.getChildren().add(createSkeletonApplicantRow());
        }
        return list;
    }
    
    public static VBox createSkeletonJobCard() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8;");
        
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().addAll(
            createSkeletonRect(140, 16),
            createSkeletonRect(60, 20)
        );
        
        HBox metaRow = new HBox(12);
        metaRow.getChildren().addAll(
            createSkeletonRect(100, 12),
            createSkeletonRect(80, 12)
        );
        
        card.getChildren().addAll(titleRow, metaRow);
        animateSkeleton(card);
        return card;
    }
    
    public static VBox createSkeletonJobList(int count) {
        VBox list = new VBox(8);
        for (int i = 0; i < count; i++) {
            list.getChildren().add(createSkeletonJobCard());
        }
        return list;
    }
    
    private static Region createSkeletonRect(double width, double height) {
        Region rect = new Region();
        rect.setMinSize(width, height);
        rect.setMaxSize(width, height);
        rect.setStyle("-fx-background-color: " + SKELETON_BASE + "; -fx-background-radius: 4;");
        return rect;
    }
    
    private static void animateSkeleton(javafx.scene.Node node) {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(500), new KeyValue(node.opacityProperty(), 0.5)),
            new KeyFrame(Duration.millis(1000), new KeyValue(node.opacityProperty(), 1.0))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
