package com.jobos.backend.controller;

import com.jobos.backend.security.AuthenticatedUser;
import com.jobos.backend.service.ApplicationService;
import com.jobos.backend.service.JobService;
import com.jobos.shared.dto.application.ApplicantResponse;
import com.jobos.shared.dto.job.JobListResponse;
import com.jobos.shared.dto.job.JobPostRequest;
import com.jobos.shared.dto.job.JobPostResponse;
import com.jobos.shared.dto.job.JobPostUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/job-posts")
public class JobPostController {

    private final JobService jobService;
    private final ApplicationService applicationService;

    public JobPostController(JobService jobService, ApplicationService applicationService) {
        this.jobService = jobService;
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<JobPostResponse> createJob(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody JobPostRequest request) {
        JobPostResponse response = jobService.createJob(
                authenticatedUser.getUserId(), 
                authenticatedUser.getRole(), 
                request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyJobs(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(direction, sortBy));

        Page<JobListResponse> jobPage = jobService.getMyJobs(
                authenticatedUser.getUserId(), 
                authenticatedUser.getRole(), 
                status, 
                pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("jobs", jobPage.getContent());
        response.put("currentPage", jobPage.getNumber());
        response.put("totalPages", jobPage.getTotalPages());
        response.put("totalElements", jobPage.getTotalElements());
        response.put("pageSize", jobPage.getSize());
        response.put("hasNext", jobPage.hasNext());
        response.put("hasPrevious", jobPage.hasPrevious());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostResponse> getJobById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id) {
        JobPostResponse response = jobService.getJobById(id, authenticatedUser.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<JobPostResponse> updateJob(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id,
            @Valid @RequestBody JobPostUpdateRequest request) {
        JobPostResponse response = jobService.updateJob(
                id, 
                authenticatedUser.getUserId(), 
                authenticatedUser.getRole(), 
                request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/applicants")
    public ResponseEntity<Map<String, Object>> getJobApplicants(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<ApplicantResponse> applicantPage = applicationService.getJobApplicants(
                id, 
                authenticatedUser.getUserId(), 
                status, 
                pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("applicants", applicantPage.getContent());
        response.put("currentPage", applicantPage.getNumber());
        response.put("totalPages", applicantPage.getTotalPages());
        response.put("totalElements", applicantPage.getTotalElements());
        response.put("pageSize", applicantPage.getSize());
        response.put("hasNext", applicantPage.hasNext());
        response.put("hasPrevious", applicantPage.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Map<String, String>> closeJob(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id) {
        jobService.closeJob(id, authenticatedUser.getUserId(), authenticatedUser.getRole());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Job closed successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<Map<String, String>> reopenJob(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id) {
        jobService.reopenJob(id, authenticatedUser.getUserId(), authenticatedUser.getRole());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Job reopened successfully");
        return ResponseEntity.ok(response);
    }
}
