package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.shared.dto.application.ApplicationRequest;
import com.jobos.shared.dto.application.ApplicationResponse;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.cv.CVResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ApplicationService {

    private final ApiClient apiClient = ApiClient.getInstance();

    public CompletableFuture<ApiResponse<ApplicationResponse>> apply(ApplicationRequest request) {
        return apiClient.post("/api/applications", request, new TypeReference<ApiResponse<ApplicationResponse>>() {});
    }

    public CompletableFuture<Map<String, Object>> getMyApplications(int page, int size) {
        String url = "/api/applications?page=" + page + "&size=" + size;
        return apiClient.get(url, new TypeReference<Map<String, Object>>() {});
    }

    public CompletableFuture<ApiResponse<ApplicationResponse>> getApplicationById(String id) {
        return apiClient.get("/api/applications/" + id, new TypeReference<ApiResponse<ApplicationResponse>>() {});
    }

    public CompletableFuture<CVResponse> getApplicantCV(String applicationId) {
        return apiClient.get("/api/applications/" + applicationId + "/cv", new TypeReference<CVResponse>() {});
    }
}