package com.jobos.backend.controller;

import com.jobos.backend.service.CVTemplateService;
import com.jobos.shared.dto.cv.CVTemplateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cv-templates")
@Tag(name = "CV Templates", description = "CV template management endpoints")
@SecurityRequirement(name = "bearer-auth")
public class CVTemplateController {

    private final CVTemplateService cvTemplateService;

    public CVTemplateController(CVTemplateService cvTemplateService) {
        this.cvTemplateService = cvTemplateService;
    }

    @GetMapping
    @Operation(summary = "Get all templates", description = "Get all available CV templates. Premium templates show isUnlocked status based on user's unlocks. Filter by category (optional).")
    public ResponseEntity<List<CVTemplateResponse>> getAllTemplates(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String category) {
        UUID userId = UUID.fromString(jwt.getSubject());
        List<CVTemplateResponse> templates = cvTemplateService.getAllTemplates(userId, category);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Get template by ID", description = "Get CV template details with unlock status")
    public ResponseEntity<CVTemplateResponse> getTemplateById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String templateId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID templateUUID = UUID.fromString(templateId);
        CVTemplateResponse response = cvTemplateService.getTemplateById(templateUUID, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{templateId}/unlock")
    @Operation(summary = "Unlock premium template", description = "Unlock a premium template using credits. Free templates are already unlocked.")
    public ResponseEntity<CVTemplateResponse> unlockTemplate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String templateId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UUID templateUUID = UUID.fromString(templateId);
        CVTemplateResponse response = cvTemplateService.unlockTemplate(templateUUID, userId);
        return ResponseEntity.ok(response);
    }
}
