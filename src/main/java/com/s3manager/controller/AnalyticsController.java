package com.s3manager.controller;

import com.s3manager.dto.analytics.StorageAnalyticsDTO;
import com.s3manager.dto.common.ApiResponse;
import com.s3manager.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/storage")
    public ResponseEntity<ApiResponse<StorageAnalyticsDTO>> getStorageAnalytics(
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        StorageAnalyticsDTO analytics = analyticsService.getStorageAnalytics(
                authentication.getName(), credentialId);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}