package com.jobos.backend.controller;

import com.jobos.backend.security.AuthenticatedUser;
import com.jobos.backend.service.CVService;
import com.jobos.shared.dto.cv.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cvs")
@Tag(name = "CVs", description = "CV management endpoints")
@SecurityRequirement(name = "bearer-auth")
public class CVController {

    private final CVService cvService;

    public CVController(CVService cvService) {
        this.cvService = cvService;
    }

    @PostMapping
    @Operation(summary = "Create a new CV", description = "Create a new CV. Maximum 5 CVs per user. First CV is automatically set as default.")
    public ResponseEntity<CVResponse> createCV(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CVCreateRequest request) {
        UUID userId = user.getUserId();
        CVResponse response = cvService.createCV(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get my CVs", description = "Get paginated list of user's CVs (simplified view without sections)")
    public ResponseEntity<Page<CVListResponse>> getMyCVs(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = user.getUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<CVListResponse> cvs = cvService.getMyCVs(userId, pageable);
        return ResponseEntity.ok(cvs);
    }

    @GetMapping("/{cvId}")
    @Operation(summary = "Get CV by ID", description = "Get full CV details with all sections")
    public ResponseEntity<CVResponse> getCVById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        CVResponse response = cvService.getCVById(cvUUID, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{cvId}")
    @Operation(summary = "Update CV", description = "Update CV properties (title, templateId, visibility). All fields are optional.")
    public ResponseEntity<CVResponse> updateCV(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId,
            @Valid @RequestBody CVUpdateRequest request) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        CVResponse response = cvService.updateCV(cvUUID, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cvId}")
    @Operation(summary = "Delete CV", description = "Delete a CV and all its sections")
    public ResponseEntity<Void> deleteCV(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        cvService.deleteCV(cvUUID, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cvId}/default")
    @Operation(summary = "Set CV as default", description = "Set this CV as the default CV. Clears previous default.")
    public ResponseEntity<CVResponse> setDefaultCV(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        CVResponse response = cvService.setDefaultCV(cvUUID, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{cvId}/sections")
    @Operation(summary = "Add section to CV", description = "Add a new section to CV. Maximum 15 sections per CV. Section is added at the end.")
    public ResponseEntity<CVSectionResponse> addSection(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId,
            @Valid @RequestBody CVSectionRequest request) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        CVSectionResponse response = cvService.addSection(cvUUID, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{cvId}/sections/{sectionId}")
    @Operation(summary = "Update section", description = "Update section properties. All fields are optional.")
    public ResponseEntity<CVSectionResponse> updateSection(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId,
            @PathVariable String sectionId,
            @Valid @RequestBody CVSectionRequest request) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        UUID sectionUUID = UUID.fromString(sectionId);
        CVSectionResponse response = cvService.updateSection(cvUUID, sectionUUID, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{cvId}/sections/{sectionId}")
    @Operation(summary = "Delete section", description = "Delete a section from CV")
    public ResponseEntity<Void> deleteSection(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId,
            @PathVariable String sectionId) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        UUID sectionUUID = UUID.fromString(sectionId);
        cvService.deleteSection(cvUUID, sectionUUID, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cvId}/sections/reorder")
    @Operation(summary = "Reorder sections", description = "Reorder sections in CV. Provide array of section IDs in desired order.")
    public ResponseEntity<CVResponse> reorderSections(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String cvId,
            @Valid @RequestBody SectionReorderRequest request) {
        UUID userId = user.getUserId();
        UUID cvUUID = UUID.fromString(cvId);
        CVResponse response = cvService.reorderSections(cvUUID, userId, request);
        return ResponseEntity.ok(response);
    }
}
