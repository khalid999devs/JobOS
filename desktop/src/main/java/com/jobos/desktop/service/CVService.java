package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.cv.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CVService {

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper();

    // ===== CV CRUD =====
    
    public CompletableFuture<Map<String, Object>> getMyCVs(int page, int size) {
        String url = "/api/cvs?page=" + page + "&size=" + size;
        return apiClient.get(url, new TypeReference<Map<String, Object>>() {});
    }

    public CompletableFuture<List<CVListResponse>> getAllCVs() {
        return apiClient.get("/api/cvs?page=0&size=100", new TypeReference<Map<String, Object>>() {})
                .thenApply(response -> {
                    Object cvs = response.get("cvs");
                    if (cvs instanceof List<?> list) {
                        return list.stream()
                                .filter(item -> item instanceof Map)
                                .map(item -> mapToCVListResponse((Map<String, Object>) item))
                                .toList();
                    }
                    return List.of();
                });
    }

    public CompletableFuture<CVResponse> getCVById(String id) {
        return apiClient.get("/api/cvs/" + id, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<CVResponse> createCV(CVCreateRequest request) {
        return apiClient.post("/api/cvs", request, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<CVResponse> updateCV(String id, CVUpdateRequest request) {
        return apiClient.patch("/api/cvs/" + id, request, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<Void> deleteCV(String id) {
        return apiClient.delete("/api/cvs/" + id, new TypeReference<ApiResponse<Void>>() {})
                .thenApply(r -> null);
    }

    public CompletableFuture<CVResponse> setDefaultCV(String id) {
        return apiClient.patch("/api/cvs/" + id + "/default", null, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    // ===== CV Sections =====
    
    public CompletableFuture<CVResponse> addSection(String cvId, CVSectionRequest request) {
        return apiClient.post("/api/cvs/" + cvId + "/sections", request, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<CVResponse> updateSection(String cvId, String sectionId, CVSectionRequest request) {
        return apiClient.patch("/api/cvs/" + cvId + "/sections/" + sectionId, request, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<CVResponse> deleteSection(String cvId, String sectionId) {
        return apiClient.delete("/api/cvs/" + cvId + "/sections/" + sectionId, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<CVResponse> reorderSections(String cvId, List<String> sectionIds) {
        Map<String, Object> body = Map.of("sectionIds", sectionIds);
        return apiClient.patch("/api/cvs/" + cvId + "/sections/reorder", body, new TypeReference<ApiResponse<CVResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    // ===== Templates =====
    
    public CompletableFuture<List<CVTemplateResponse>> getTemplates(String category) {
        String url = "/api/cv-templates";
        if (category != null && !category.isEmpty()) {
            url += "?category=" + category;
        }
        return apiClient.get(url, new TypeReference<ApiResponse<List<CVTemplateResponse>>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<CVTemplateResponse> getTemplateById(String id) {
        return apiClient.get("/api/cv-templates/" + id, new TypeReference<ApiResponse<CVTemplateResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    public CompletableFuture<CVTemplateResponse> unlockTemplate(String id) {
        return apiClient.post("/api/cv-templates/" + id + "/unlock", null, new TypeReference<ApiResponse<CVTemplateResponse>>() {})
                .thenApply(ApiResponse::getResult);
    }

    // ===== Helpers =====
    
    private CVListResponse mapToCVListResponse(Map<String, Object> map) {
        CVListResponse cv = new CVListResponse();
        cv.setId((String) map.get("id"));
        cv.setTitle((String) map.get("title"));
        cv.setTemplateName((String) map.get("templateName"));
        cv.setIsDefault((Boolean) map.get("isDefault"));
        cv.setVisibility((String) map.get("visibility"));
        if (map.get("sectionCount") != null) {
            cv.setSectionCount(((Number) map.get("sectionCount")).intValue());
        }
        return cv;
    }
}
