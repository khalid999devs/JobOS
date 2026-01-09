package com.jobos.android.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.jobos.android.R;
import com.jobos.android.data.local.SessionManager;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.main.MainActivity;

import java.util.Map;

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * Handles both data messages and notification messages.
 */
public class JobOSFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "JobOSFCMService";
    private static final String CHANNEL_ID = "jobos_notifications";
    private static final String CHANNEL_NAME = "JobOS Notifications";
    public static final String ACTION_NEW_NOTIFICATION = "com.jobos.android.NEW_NOTIFICATION";
    public static final String EXTRA_NOTIFICATION_DATA = "notification_data";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Send the new FCM token to the backend
        sendTokenToServer(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Handle data payload
        if (remoteMessage.getData().size() > 0) {
            handleDataMessage(remoteMessage.getData());
        }

        // Handle notification payload (when app is in foreground)
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            showNotification(title, body, remoteMessage.getData());
        }

        // Broadcast to refresh notifications in app
        broadcastNewNotification(remoteMessage);
    }

    private void handleDataMessage(Map<String, String> data) {
        String type = data.get("type");
        String title = data.get("title");
        String message = data.get("message");

        // Show notification for data messages
        if (title != null && message != null) {
            showNotification(title, message, data);
        }

        // Handle specific notification types
        if (type != null) {
            switch (type) {
                case "JOB_APPLICATION_UPDATE":
                    // Refresh applications
                    broadcastAction("REFRESH_APPLICATIONS");
                    break;
                case "NEW_JOB_MATCH":
                    // Refresh job listings
                    broadcastAction("REFRESH_JOBS");
                    break;
                case "CV_STATUS_UPDATE":
                    // Refresh CV list
                    broadcastAction("REFRESH_CVS");
                    break;
                case "PROFILE_UPDATE":
                    // Refresh profile
                    broadcastAction("REFRESH_PROFILE");
                    break;
                default:
                    // General notification refresh
                    broadcastAction("REFRESH_NOTIFICATIONS");
                    break;
            }
        }
    }

    private void showNotification(String title, String body, Map<String, String> data) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Add notification data to intent
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title != null ? title : "JobOS")
                .setContentText(body != null ? body : "New notification")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.notify(requestCode, notificationBuilder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("JobOS push notifications");
            channel.enableLights(true);
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void sendTokenToServer(String token) {
        SessionManager sessionManager = new SessionManager(this);
        String accessToken = sessionManager.getAccessToken();

        // Only send token if user is logged in
        if (accessToken != null && !accessToken.isEmpty()) {
            ApiService apiService = ApiService.getInstance(this);
            apiService.updateFcmToken(accessToken, token, new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    android.util.Log.d(TAG, "FCM token updated successfully");
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e(TAG, "Failed to update FCM token: " + error);
                }
            });
        }
    }

    private void broadcastNewNotification(RemoteMessage remoteMessage) {
        Intent intent = new Intent(ACTION_NEW_NOTIFICATION);
        intent.setPackage(getPackageName());
        
        // Add notification data
        if (remoteMessage.getNotification() != null) {
            intent.putExtra("title", remoteMessage.getNotification().getTitle());
            intent.putExtra("body", remoteMessage.getNotification().getBody());
        }
        
        // Add data payload
        if (remoteMessage.getData().size() > 0) {
            for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        sendBroadcast(intent);
    }

    private void broadcastAction(String action) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);
    }
}
