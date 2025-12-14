package com.jobos.android;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jobos.android.dto.PingResponse;
import com.jobos.android.dto.Notification;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "JobOS";
    private static final String USER_ID = "test-user-123";
    
    private TextView responseText;
    private TextView notificationsText;
    private ApiClient apiClient;
    private ExecutorService executor;
    private DatabaseReference notificationsRef;
    private ChildEventListener notificationListener;
    private StringBuilder notificationList = new StringBuilder();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = new ApiClient();
        executor = Executors.newSingleThreadExecutor();

        responseText = findViewById(R.id.responseText);
        notificationsText = findViewById(R.id.notificationsText);
        Button pingButton = findViewById(R.id.pingButton);

        pingButton.setOnClickListener(v -> pingBackend());
        setupFirebaseListener();
    }

    private void pingBackend() {
        executor.execute(() -> {
            try {
                PingResponse response = apiClient.ping();
                runOnUiThread(() ->
                    responseText.setText("Response: " + response.getMessage())
                );
            } catch (Exception e) {
                runOnUiThread(() ->
                    responseText.setText("Error: " + e.getMessage())
                );
            }
        });
    }

    private void setupFirebaseListener() {
        Log.d(TAG, "Setting up Firebase listener...");
        FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://jobos-4e21e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        notificationsRef = database.getReference("users/" + USER_ID + "/notifications");
        Log.d(TAG, "Firebase reference: users/" + USER_ID + "/notifications");

        notificationListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "Firebase onChildAdded: " + snapshot.getKey());
                Notification notification = snapshot.getValue(Notification.class);
                if (notification != null) {
                    Log.d(TAG, "Notification: " + notification.getTitle());
                    String time = dateFormat.format(new Date());
                    String newNotif = "[" + time + "] " + notification.getTitle() + "\n" + 
                                     notification.getBody() + "\n\n";
                    notificationList.insert(0, newNotif);
                    runOnUiThread(() -> notificationsText.setText(notificationList.toString()));
                } else {
                    Log.w(TAG, "Notification is null");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildChanged: " + snapshot.getKey());
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onChildRemoved: " + snapshot.getKey());
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Log.d(TAG, "onChildMoved: " + snapshot.getKey());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase error: " + error.getMessage());
                runOnUiThread(() -> notificationsText.setText("Firebase Error: " + error.getMessage()));
            }
        };

        notificationsRef.addChildEventListener(notificationListener);
        Log.d(TAG, "Firebase listener attached");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        if (notificationsRef != null && notificationListener != null) {
            notificationsRef.removeEventListener(notificationListener);
        }
    }
}