package com.jobos.backend.controller;

import com.jobos.backend.security.AuthenticatedUser;
import com.jobos.backend.service.JobSearchService;
import com.jobos.backend.service.JobService;
import com.jobos.shared.dto.job.JobListResponse;
import com.jobos.shared.dto.job.JobPostResponse;
import com.jobos.shared.dto.job.JobSearchRequest;
import com.jobos.shared.dto.job.JobSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobSearchController {

    @Autowired
    private JobSearchService jobSearchService;

    @Autowired
    private JobService jobService;

    @PostMapping("/search")
    public ResponseEntity<JobSearchResponse> searchJobs(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestBody JobSearchRequest searchRequest) {
        UUID userId = authenticatedUser != null ? authenticatedUser.getUserId() : null;
        JobSearchResponse response = jobSearchService.searchJobs(searchRequest, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostResponse> getJobById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id) {
        UUID userId = authenticatedUser != null ? authenticatedUser.getUserId() : null;
        JobPostResponse response = jobService.getJobById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<Map<String, String>> saveJob(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id) {
        jobSearchService.saveJob(authenticatedUser.getUserId(), id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Job saved successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/save")
    public ResponseEntity<Map<String, String>> unsaveJob(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id) {
        jobSearchService.unsaveJob(authenticatedUser.getUserId(), id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Job unsaved successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/saved")
    public ResponseEntity<Map<String, Object>> getSavedJobs(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "savedAt"));

        Page<JobListResponse> savedJobsPage = jobSearchService.getSavedJobs(authenticatedUser.getUserId(), pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("jobs", savedJobsPage.getContent());
        response.put("currentPage", savedJobsPage.getNumber());
        response.put("totalPages", savedJobsPage.getTotalPages());
        response.put("totalElements", savedJobsPage.getTotalElements());
        response.put("pageSize", savedJobsPage.getSize());
        response.put("hasNext", savedJobsPage.hasNext());
        response.put("hasPrevious", savedJobsPage.hasPrevious());

        return ResponseEntity.ok(response);
    }
}
