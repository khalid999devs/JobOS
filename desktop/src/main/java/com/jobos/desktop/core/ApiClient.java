package com.jobos.desktop.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.shared.dto.common.PingResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiClient {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
    }

    public PingResponse ping() throws Exception {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/ping")
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
