package com.jobos.backend.controller;

import com.jobos.backend.service.CreditService;
import com.jobos.shared.dto.credit.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/credits")
@Tag(name = "Credits", description = "Credit management endpoints")
@SecurityRequirement(name = "bearer-auth")
public class CreditController {

    private final CreditService creditService;

    public CreditController(CreditService creditService) {
        this.creditService = creditService;
    }

    @GetMapping("/balance")
    @Operation(summary = "Get credit balance", description = "Get user's current credit balance")
    public ResponseEntity<CreditBalanceResponse> getBalance(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CreditBalanceResponse response = creditService.getBalance(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/purchase")
    @Operation(summary = "Purchase credits", description = "Purchase credits using payment method")
    public ResponseEntity<CreditBalanceResponse> purchaseCredits(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreditPurchaseRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        CreditBalanceResponse response = creditService.purchaseCredits(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get credit transactions", description = "Get paginated credit transaction history")
    public ResponseEntity<Page<CreditTransactionResponse>> getTransactions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Pageable pageable = PageRequest.of(page, size);
        Page<CreditTransactionResponse> response = creditService.getTransactions(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
