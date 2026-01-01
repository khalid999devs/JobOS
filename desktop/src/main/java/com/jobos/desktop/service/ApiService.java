package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.desktop.util.Constants;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.common.HealthResponse;
import com.jobos.shared.dto.common.PingResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ApiService {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public ApiService() {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public PingResponse ping() throws Exception {
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + "/api/ping")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("HTTP " + response.code());
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

    public HealthResponse health() throws Exception {
        Request request = new Request.Builder()
                .url(Constants.API_BASE_URL + "/api/health")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("HTTP " + response.code());
            }
            String responseBody = response.body().string();
            ApiResponse<HealthResponse> apiResponse = objectMapper.readValue(
                responseBody, 
                new TypeReference<ApiResponse<HealthResponse>>() {}
            );
            
            if (!apiResponse.isSuccess()) {
                throw new Exception(apiResponse.getMessage() != null ? apiResponse.getMessage() : "API request failed");
            }
            
            return apiResponse.getResult();
        }
    }
}
