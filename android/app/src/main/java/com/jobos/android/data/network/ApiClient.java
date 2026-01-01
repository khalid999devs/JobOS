package com.jobos.android.data.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.android.data.model.ApiResponse;
import com.jobos.android.data.model.PingResponse;
import com.jobos.android.config.ApiConfig;
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
            ApiResponse<PingResponse> apiResponse = objectMapper.readValue(
                responseBody, 
                new TypeReference<ApiResponse<PingResponse>>() {}
            );
            
            if (!apiResponse.isSuccess()) {
                throw new Exception(apiResponse.getMessage() != null ? apiResponse.getMessage() : "API request failed");
            }
            
            return apiResponse.getResult();
        }
    }
}
