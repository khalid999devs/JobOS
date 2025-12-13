package com.jobos.android;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.jobos.android.dto.PingResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private TextView responseText;
    private ApiClient apiClient;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiClient = new ApiClient();
        executor = Executors.newSingleThreadExecutor();

        responseText = findViewById(R.id.responseText);
        Button pingButton = findViewById(R.id.pingButton);

        pingButton.setOnClickListener(v -> pingBackend());
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}