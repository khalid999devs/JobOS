package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.notification.NotificationResponse;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class NotificationService {

    private static NotificationService instance;
    private final ApiClient apiClient = ApiClient.getInstance();
    private ScheduledExecutorService scheduler;
    private Consumer<Long> unreadCountListener;
    private boolean isPolling = false;

    private NotificationService() {}

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public CompletableFuture<Map<String, Object>> getNotifications(int page, int size) {
        return apiClient.get("/api/notifications?page=" + page + "&size=" + size, 
                new TypeReference<Map<String, Object>>() {});
    }

    public CompletableFuture<ApiResponse<Long>> getUnreadCount() {
        return apiClient.get("/api/notifications/unread-count", 
                new TypeReference<ApiResponse<Long>>() {});
    }

    public CompletableFuture<ApiResponse<NotificationResponse>> markAsRead(String notificationId) {
        return apiClient.patch("/api/notifications/" + notificationId + "/read", null, 
                new TypeReference<ApiResponse<NotificationResponse>>() {});
    }

    public CompletableFuture<ApiResponse<Void>> markAllAsRead() {
        return apiClient.patch("/api/notifications/read-all", null, 
                new TypeReference<ApiResponse<Void>>() {});
    }

    public CompletableFuture<ApiResponse<Object>> getPreferences() {
        return apiClient.get("/api/notifications/preferences", 
                new TypeReference<ApiResponse<Object>>() {});
    }

    public CompletableFuture<ApiResponse<Object>> updatePreferences(Map<String, Object> preferences) {
        return apiClient.patch("/api/notifications/preferences", preferences, 
                new TypeReference<ApiResponse<Object>>() {});
    }

    public void setUnreadCountListener(Consumer<Long> listener) {
        this.unreadCountListener = listener;
    }

    public void startPolling(int intervalSeconds) {
        if (isPolling) return;
        
        isPolling = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                getUnreadCount()
                    .thenAccept(response -> {
                        if (response != null && response.getResult() != null && unreadCountListener != null) {
                            unreadCountListener.accept(response.getResult());
                        }
                    })
                    .exceptionally(e -> null);
            } catch (Exception e) {
                // Ignore polling errors
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stopPolling() {
        isPolling = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    public void refreshUnreadCount() {
        getUnreadCount()
            .thenAccept(response -> {
                if (response != null && response.getResult() != null && unreadCountListener != null) {
                    unreadCountListener.accept(response.getResult());
                }
            })
            .exceptionally(e -> null);
    }
}
