package com.jobos.backend.controller;

import com.jobos.backend.service.NotificationService;
import com.jobos.shared.dto.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<String>> sendNotification(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String title = request.get("title");
        String body = request.get("body");

        if (userId == null || title == null || body == null) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Missing required fields: userId, title, body"));
        }

        notificationService.publishUserNotification(userId, title, body);
        return ResponseEntity.ok(ApiResponse.success(
            "Notification sent to user: " + userId,
            "Notification sent successfully"
        ));
    }
}
