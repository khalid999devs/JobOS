package com.jobos.backend.controller;

import com.jobos.backend.service.NotificationService;
import com.jobos.shared.dto.notification.NotificationPreferenceResponse;
import com.jobos.shared.dto.notification.NotificationResponse;
import com.jobos.shared.dto.notification.UpdatePreferenceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.jobos.backend.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management endpoints")
@SecurityRequirement(name = "bearer-auth")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Get notifications", description = "Get paginated list of notifications")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = user.getUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> response = notificationService.getNotifications(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getUserId();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark notification as read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String notificationId) {
        UUID userId = user.getUserId();
        UUID notifId = UUID.fromString(notificationId);
        NotificationResponse response = notificationService.markAsRead(userId, notifId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get preferences", description = "Get notification preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreferences(@AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getUserId();
        NotificationPreferenceResponse response = notificationService.getPreferences(userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/preferences")
    @Operation(summary = "Update preferences", description = "Update notification preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdatePreferenceRequest request) {
        UUID userId = user.getUserId();
        NotificationPreferenceResponse response = notificationService.updatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
}
