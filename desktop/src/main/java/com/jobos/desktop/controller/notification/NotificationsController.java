package com.jobos.desktop.controller.notification;

import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.SkeletonLoader;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.NotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NotificationsController implements Initializable {
    
    @FXML private VBox notificationsList;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label unreadCountLabel;
    @FXML private HBox paginationContainer;
    
    private final NotificationService notificationService = NotificationService.getInstance();
    private final Router router = Router.getInstance();
    
    private int currentPage = 0;
    private int totalPages = 1;
    private String filter = "all";

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilters();
        loadNotifications();
    }

    private void setupFilters() {
        if (filterCombo != null) {
            filterCombo.getItems().addAll("All", "Unread");
            filterCombo.setValue("All");
            filterCombo.setOnAction(e -> {
                filter = "Unread".equals(filterCombo.getValue()) ? "unread" : "all";
                currentPage = 0;
                loadNotifications();
            });
        }
    }

    @FXML
    private void onMarkAllRead() {
        Toast.info("Marking all as read...");
        
        notificationService.markAllAsRead()
            .thenAccept(response -> Platform.runLater(() -> {
                Toast.success("All notifications marked as read");
                loadNotifications();
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Toast.error("Failed to mark all as read");
                });
                return null;
            });
    }

    private void loadNotifications() {
        // Show skeleton loading
        notificationsList.getChildren().setAll(SkeletonLoader.createSkeletonList(5));
        
        notificationService.getNotifications(currentPage, 20)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    renderNotifications(response);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    showEmptyState("Failed to load notifications");
                });
                return null;
            });
    }

    @SuppressWarnings("unchecked")
    private void renderNotifications(Map<String, Object> response) {
        notificationsList.getChildren().clear();
        
        Object resultObj = response.get("result");
        Map<String, Object> pageData = null;
        
        if (resultObj instanceof Map) {
            pageData = (Map<String, Object>) resultObj;
        } else {
            pageData = response;
        }
        
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.get("content");
        if (content == null) {
            content = (List<Map<String, Object>>) pageData.get("notifications");
        }
        
        Object totalObj = pageData.get("totalElements");
        int total = totalObj != null ? ((Number) totalObj).intValue() : 0;
        totalPages = pageData.get("totalPages") != null ? ((Number) pageData.get("totalPages")).intValue() : 1;
        
        long unreadCount = 0;
        if (content != null) {
            for (Map<String, Object> n : content) {
                Boolean isRead = (Boolean) n.get("isRead");
                if (!Boolean.TRUE.equals(isRead)) {
                    unreadCount++;
                }
            }
        }
        
        if (unreadCountLabel != null) {
            unreadCountLabel.setText(unreadCount + " unread");
        }
        
        if (content == null || content.isEmpty()) {
            showEmptyState("No notifications yet");
            return;
        }
        
        for (Map<String, Object> notification : content) {
            if ("unread".equals(filter)) {
                Boolean isRead = (Boolean) notification.get("isRead");
                if (Boolean.TRUE.equals(isRead)) continue;
            }
            notificationsList.getChildren().add(createNotificationCard(notification));
        }
        
        if (notificationsList.getChildren().isEmpty()) {
            showEmptyState("No " + filter + " notifications");
        }
        
        renderPagination();
    }

    private VBox createNotificationCard(Map<String, Object> notification) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-cursor: hand;");
        
        String id = getString(notification, "id");
        String type = getString(notification, "notificationType");
        String title = getString(notification, "title");
        String message = getString(notification, "message");
        String actionUrl = getString(notification, "actionUrl");
        Boolean isRead = (Boolean) notification.get("isRead");
        String createdAt = getString(notification, "createdAt");
        
        if (!Boolean.TRUE.equals(isRead)) {
            card.setStyle(card.getStyle() + "-fx-border-color: #0F766E; -fx-border-width: 0 0 0 4;");
        }
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label icon = new Label(getIconForType(type));
        icon.setStyle("-fx-font-size: 20px;");
        
        Label titleLabel = new Label(title != null ? title : "Notification");
        titleLabel.getStyleClass().add("h4");
        titleLabel.setStyle("-fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        if (!Boolean.TRUE.equals(isRead)) {
            Label unreadDot = new Label("●");
            unreadDot.setStyle("-fx-text-fill: #0F766E;");
            header.getChildren().add(unreadDot);
        }
        
        header.getChildren().addAll(icon, titleLabel);
        
        Label messageLabel = new Label(message != null ? message : "");
        messageLabel.getStyleClass().add("label-secondary");
        messageLabel.setWrapText(true);
        
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        if (createdAt != null) {
            Label dateLabel = new Label(formatDate(createdAt));
            dateLabel.getStyleClass().add("label-muted");
            footer.getChildren().add(dateLabel);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().add(spacer);
        
        if (!Boolean.TRUE.equals(isRead)) {
            Button markReadBtn = new Button("Mark as read");
            markReadBtn.getStyleClass().add("button-secondary");
            markReadBtn.setOnAction(e -> {
                e.consume();
                markAsRead(id);
            });
            footer.getChildren().add(markReadBtn);
        }
        
        card.getChildren().addAll(header, messageLabel, footer);
        
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            
            if (!Boolean.TRUE.equals(isRead)) {
                markAsRead(id);
            }
            
            if (actionUrl != null && !actionUrl.isEmpty()) {
                handleActionUrl(actionUrl);
            }
        });
        
        return card;
    }

    private void markAsRead(String notificationId) {
        notificationService.markAsRead(notificationId)
            .thenAccept(response -> Platform.runLater(this::loadNotifications))
            .exceptionally(e -> null);
    }

    private void handleActionUrl(String actionUrl) {
        // Parse action URL and navigate
        // Format could be: /jobs/{id}, /applications/{id}, etc.
        // For now, just refresh notifications
        Toast.info("Navigation to: " + actionUrl);
    }

    private String getIconForType(String type) {
        if (type == null) return "🔔";
        return switch (type.toUpperCase()) {
            case "APPLICATION_RECEIVED" -> "📨";
            case "APPLICATION_STATUS_CHANGED" -> "📋";
            case "JOB_POSTED" -> "📝";
            case "JOB_EXPIRED" -> "⏰";
            case "NEW_MATCH" -> "✨";
            case "MESSAGE" -> "💬";
            default -> "🔔";
        };
    }

    private void showEmptyState(String message) {
        notificationsList.getChildren().clear();
        
        VBox emptyState = new VBox(16);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(48));
        
        Label icon = new Label("🔔");
        icon.setStyle("-fx-font-size: 48px;");
        
        Label title = new Label("No notifications");
        title.getStyleClass().add("h3");
        
        Label desc = new Label(message);
        desc.getStyleClass().add("label-secondary");
        
        emptyState.getChildren().addAll(icon, title, desc);
        notificationsList.getChildren().add(emptyState);
    }

    private void renderPagination() {
        if (paginationContainer == null) return;
        paginationContainer.getChildren().clear();
        
        if (totalPages <= 1) return;
        
        Button prevBtn = new Button("← Previous");
        prevBtn.getStyleClass().add("button-secondary");
        prevBtn.setDisable(currentPage == 0);
        prevBtn.setOnAction(e -> {
            currentPage--;
            loadNotifications();
        });
        
        Label pageLabel = new Label("Page " + (currentPage + 1) + " of " + totalPages);
        pageLabel.getStyleClass().add("label-secondary");
        
        Button nextBtn = new Button("Next →");
        nextBtn.getStyleClass().add("button-secondary");
        nextBtn.setDisable(currentPage >= totalPages - 1);
        nextBtn.setOnAction(e -> {
            currentPage++;
            loadNotifications();
        });
        
        paginationContainer.getChildren().addAll(prevBtn, pageLabel, nextBtn);
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String formatDate(String dateStr) {
        try {
            LocalDateTime dt = LocalDateTime.parse(dateStr.substring(0, 19));
            return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
