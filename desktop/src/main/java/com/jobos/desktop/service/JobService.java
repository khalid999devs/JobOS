package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.shared.dto.job.JobPostResponse;
import com.jobos.shared.dto.job.JobSearchRequest;
import com.jobos.shared.dto.job.JobSearchResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JobService {

    private final ApiClient apiClient = ApiClient.getInstance();

    public CompletableFuture<JobSearchResponse> searchJobs(JobSearchRequest request) {
        return apiClient.post("/api/jobs/search", request, JobSearchResponse.class);
    }

    public CompletableFuture<JobSearchResponse> searchJobs(String keywords, String location, 
                                                           List<String> workModes, List<String> jobTypes,
                                                           List<String> experienceLevels, 
                                                           Integer salaryMin, Integer salaryMax,
                                                           int page, int size) {
        JobSearchRequest request = new JobSearchRequest();
        request.setKeywords(keywords);
        request.setLocation(location);
        request.setWorkModes(workModes);
        request.setJobTypes(jobTypes);
        request.setExperienceLevels(experienceLevels);
        request.setSalaryMin(salaryMin);
        request.setSalaryMax(salaryMax);
        request.setPage(page);
        request.setSize(size);
        return apiClient.post("/api/jobs/search", request, JobSearchResponse.class);
    }
    
    /**
     * @deprecated Use the version with salary parameters instead
     */
    @Deprecated
    public CompletableFuture<JobSearchResponse> searchJobs(String keywords, String location, 
                                                           List<String> workModes, List<String> jobTypes,
                                                           List<String> experienceLevels, int page, int size) {
        return searchJobs(keywords, location, workModes, jobTypes, experienceLevels, null, null, page, size);
    }

    public CompletableFuture<JobPostResponse> getJobById(String jobId) {
        return apiClient.get("/api/jobs/" + jobId, JobPostResponse.class);
    }

    public CompletableFuture<Map<String, String>> saveJob(String jobId) {
        return apiClient.post("/api/jobs/" + jobId + "/save", null, new TypeReference<Map<String, String>>() {});
    }

    public CompletableFuture<Void> unsaveJob(String jobId) {
        return apiClient.delete("/api/jobs/" + jobId + "/save");
    }

    public CompletableFuture<Map<String, Object>> getSavedJobs(int page, int size) {
        String url = "/api/jobs/saved?page=" + page + "&size=" + size;
        return apiClient.get(url, new TypeReference<Map<String, Object>>() {});
    }
}
