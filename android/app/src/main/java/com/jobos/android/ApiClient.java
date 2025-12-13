package com.jobos.android;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.android.dto.PingResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClient {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public ApiClient() {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public PingResponse ping() throws Exception {
        Request request = new Request.Builder()
                .url(ApiConfig.BASE_URL + "/api/ping")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("HTTP error: " + response.code());
            }
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, PingResponse.class);
        }
    }
}
