package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.shared.dto.application.ApplicantResponse;
import com.jobos.shared.dto.application.ApplicationStatusUpdateRequest;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.job.JobPostRequest;
import com.jobos.shared.dto.job.JobPostResponse;
import com.jobos.shared.dto.job.JobPostUpdateRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JobPostService {

    private final ApiClient apiClient = ApiClient.getInstance();

    public CompletableFuture<Map<String, Object>> getMyJobPosts(int page, int size, String status) {
        StringBuilder url = new StringBuilder("/api/job-posts?page=" + page + "&size=" + size);
        if (status != null && !status.isEmpty()) {
            url.append("&status=").append(status);
        }
        return apiClient.get(url.toString(), new TypeReference<Map<String, Object>>() {});
    }

    public CompletableFuture<JobPostResponse> getJobPostById(String id) {
        return apiClient.get("/api/job-posts/" + id, JobPostResponse.class);
    }

    public CompletableFuture<JobPostResponse> createJobPost(JobPostRequest request) {
        return apiClient.post("/api/job-posts", request, JobPostResponse.class);
    }

    public CompletableFuture<JobPostResponse> updateJobPost(String id, JobPostUpdateRequest request) {
        return apiClient.patch("/api/job-posts/" + id, request, JobPostResponse.class);
    }

    public CompletableFuture<Map<String, String>> closeJob(String id) {
        return apiClient.post("/api/job-posts/" + id + "/close", null, new TypeReference<Map<String, String>>() {});
    }

    public CompletableFuture<Map<String, String>> reopenJob(String id) {
        return apiClient.post("/api/job-posts/" + id + "/reopen", null, new TypeReference<Map<String, String>>() {});
    }

    public CompletableFuture<Map<String, Object>> getJobApplicants(String jobId, int page, int size, String status) {
        StringBuilder url = new StringBuilder("/api/job-posts/" + jobId + "/applicants?page=" + page + "&size=" + size);
        if (status != null && !status.isEmpty()) {
            url.append("&status=").append(status);
        }
        return apiClient.get(url.toString(), new TypeReference<Map<String, Object>>() {});
    }

    public CompletableFuture<ApiResponse<ApplicantResponse>> updateApplicationStatus(String applicationId, ApplicationStatusUpdateRequest request) {
        return apiClient.patch("/api/applications/" + applicationId + "/status", request, new TypeReference<ApiResponse<ApplicantResponse>>() {});
    }
}
