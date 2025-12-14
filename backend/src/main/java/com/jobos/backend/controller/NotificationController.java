package com.jobos.backend.controller;

import com.jobos.backend.service.NotificationService;
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
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, String> request) {
        try {
            String userId = request.get("userId");
            String title = request.get("title");
            String body = request.get("body");

            if (userId == null || title == null || body == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Missing required fields: userId, title, body"));
            }

            notificationService.publishUserNotification(userId, title, body);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Notification sent to user: " + userId
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
