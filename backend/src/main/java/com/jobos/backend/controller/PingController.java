package com.jobos.backend.controller;

import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.common.PingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    @GetMapping("/ping")
    public ApiResponse<PingResponse> ping() {
        return ApiResponse.success(new PingResponse("pong"));
    }
}
