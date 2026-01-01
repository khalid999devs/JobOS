package com.jobos.shared.dto.notification;

import jakarta.validation.constraints.NotBlank;

public class UpdateFcmTokenRequest {

    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    public UpdateFcmTokenRequest() {
    }

    public UpdateFcmTokenRequest(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
