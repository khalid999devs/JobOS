package com.jobos.backend.controller;

import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.common.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("ok"), "Service is healthy");
    }
}
