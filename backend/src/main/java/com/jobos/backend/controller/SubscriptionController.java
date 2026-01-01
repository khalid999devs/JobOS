package com.jobos.backend.controller;

import com.jobos.backend.service.CreditService;
import com.jobos.shared.dto.credit.SubscribeRequest;
import com.jobos.shared.dto.credit.SubscriptionPlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.jobos.backend.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/plans")
@Tag(name = "Subscription Plans", description = "Subscription plan management endpoints")
@SecurityRequirement(name = "bearer-auth")
public class SubscriptionController {

    private final CreditService creditService;

    public SubscriptionController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping
    @Operation(summary = "Get all plans", description = "Get all available subscription plans with current plan indication")
    public ResponseEntity<List<SubscriptionPlanResponse>> getAllPlans(@AuthenticationPrincipal AuthenticatedUser user) {
        UUID userId = user.getUserId();
        List<SubscriptionPlanResponse> response = creditService.getAllPlans(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Subscribe to plan", description = "Subscribe to a subscription plan")
    public ResponseEntity<SubscriptionPlanResponse> subscribe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SubscribeRequest request) {
        UUID userId = user.getUserId();
        SubscriptionPlanResponse response = creditService.subscribe(userId, request);
        return ResponseEntity.ok(response);
    }
}
