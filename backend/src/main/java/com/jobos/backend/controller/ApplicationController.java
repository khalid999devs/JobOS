package com.jobos.backend.controller;

import com.jobos.backend.security.AuthenticatedUser;
import com.jobos.backend.service.ApplicationService;
import com.jobos.shared.dto.application.*;
import com.jobos.shared.dto.common.ApiResponse;
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
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse response = applicationService.apply(authenticatedUser.getUserId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Application submitted successfully"));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyApplications(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<ApplicationListResponse> applicationPage = applicationService.getMyApplications(authenticatedUser.getUserId(), pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("applications", applicationPage.getContent());
        response.put("currentPage", applicationPage.getNumber());
        response.put("totalPages", applicationPage.getTotalPages());
        response.put("totalElements", applicationPage.getTotalElements());
        response.put("pageSize", applicationPage.getSize());
        response.put("hasNext", applicationPage.hasNext());
        response.put("hasPrevious", applicationPage.hasPrevious());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getApplicationById(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id) {
        ApplicationResponse response = applicationService.getApplicationById(id, authenticatedUser.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response, "Application retrieved successfully"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        ApplicationResponse response = applicationService.updateApplicationStatus(id, authenticatedUser.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Application status updated successfully"));
    }
}
