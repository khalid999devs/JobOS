package com.jobos.backend.service;

import com.jobos.backend.domain.notification.Notification;
import com.jobos.backend.domain.notification.NotificationPreference;
import com.jobos.backend.domain.notification.NotificationType;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.repository.NotificationPreferenceRepository;
import com.jobos.backend.repository.NotificationRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.shared.dto.notification.NotificationPreferenceResponse;
import com.jobos.shared.dto.notification.NotificationResponse;
import com.jobos.shared.dto.notification.UpdatePreferenceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final FirebaseMessagingService firebaseMessagingService;

    public NotificationService(NotificationRepository notificationRepository,
                              NotificationPreferenceRepository preferenceRepository,
                              UserRepository userRepository,
                              FirebaseMessagingService firebaseMessagingService) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.firebaseMessagingService = firebaseMessagingService;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Page<Notification> notifications = notificationRepository.findByUser(user, pageable);
        return notifications.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    @Transactional
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        notification.setIsRead(true);
        notification = notificationRepository.save(notification);

        return mapToResponse(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        notificationRepository.markAllAsRead(user);
    }

    @Transactional
    public void createNotification(User user, NotificationType type, String title, String message, String actionUrl) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setIsRead(false);
        notificationRepository.save(notification);

        sendPushNotificationIfEnabled(user, type, title, message, actionUrl);
    }

    private void sendPushNotificationIfEnabled(User user, NotificationType type, String title, String message, String actionUrl) {
        try {
            NotificationPreference pref = preferenceRepository.findByUser(user).orElse(null);
            
            if (pref == null) {
                logger.debug("No preferences found for user {}, skipping push notification", user.getId());
                return;
            }

            if (!Boolean.TRUE.equals(pref.getPushEnabled())) {
                logger.debug("Push notifications disabled for user {}", user.getId());
                return;
            }

            if (!isNotificationTypeEnabled(pref, type)) {
                logger.debug("Notification type {} disabled for user {}", type, user.getId());
                return;
            }

            String fcmToken = user.getFcmToken();
            if (fcmToken == null || fcmToken.isEmpty()) {
                logger.debug("No FCM token for user {}, skipping push notification", user.getId());
                return;
            }

            firebaseMessagingService.sendPushNotification(fcmToken, title, message, actionUrl);
        } catch (Exception e) {
            logger.error("Failed to send push notification to user {}: {}", user.getId(), e.getMessage());
        }
    }

    private boolean isNotificationTypeEnabled(NotificationPreference pref, NotificationType type) {
        return switch (type) {
            case APPLICATION_STATUS_CHANGE -> Boolean.TRUE.equals(pref.getApplicationUpdates());
            case NEW_JOB_MATCH -> Boolean.TRUE.equals(pref.getJobRecommendations());
            case MESSAGE_RECEIVED -> true;
            case CREDIT_LOW -> true;
            case SUBSCRIPTION_EXPIRING -> true;
            case SYSTEM_ANNOUNCEMENT -> true;
        };
    }

    @Transactional
    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        String cleanToken = fcmToken != null ? fcmToken.replaceAll("^\"|\"$", "").trim() : null;
        user.setFcmToken(cleanToken);
        userRepository.save(user);
        logger.info("FCM token updated for user {}", userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        NotificationPreference pref = preferenceRepository.findByUser(user)
                .orElseGet(() -> initializePreferences(user));

        return mapToPreferenceResponse(pref);
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(UUID userId, UpdatePreferenceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        NotificationPreference pref = preferenceRepository.findByUser(user)
                .orElseGet(() -> initializePreferences(user));

        if (request.getEmailEnabled() != null) pref.setEmailEnabled(request.getEmailEnabled());
        if (request.getPushEnabled() != null) pref.setPushEnabled(request.getPushEnabled());
        if (request.getApplicationUpdates() != null) pref.setApplicationUpdates(request.getApplicationUpdates());
        if (request.getJobRecommendations() != null) pref.setJobRecommendations(request.getJobRecommendations());
        if (request.getMarketingEmails() != null) pref.setMarketingEmails(request.getMarketingEmails());

        pref = preferenceRepository.save(pref);
        return mapToPreferenceResponse(pref);
    }

    private NotificationPreference initializePreferences(User user) {
        NotificationPreference pref = new NotificationPreference();
        pref.setUser(user);
        pref.setEmailEnabled(true);
        pref.setPushEnabled(true);
        pref.setApplicationUpdates(true);
        pref.setJobRecommendations(true);
        pref.setMarketingEmails(false);
        return preferenceRepository.save(pref);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId().toString());
        response.setNotificationType(notification.getNotificationType().name());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setActionUrl(notification.getActionUrl());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt().toString());
        return response;
    }

    private NotificationPreferenceResponse mapToPreferenceResponse(NotificationPreference pref) {
        NotificationPreferenceResponse response = new NotificationPreferenceResponse();
        response.setUserId(pref.getUser().getId().toString());
        response.setEmailEnabled(pref.getEmailEnabled());
        response.setPushEnabled(pref.getPushEnabled());
        response.setApplicationUpdates(pref.getApplicationUpdates());
        response.setJobRecommendations(pref.getJobRecommendations());
        response.setMarketingEmails(pref.getMarketingEmails());
        return response;
    }
}
