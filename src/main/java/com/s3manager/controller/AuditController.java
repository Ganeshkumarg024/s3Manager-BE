package com.s3manager.controller;

import com.s3manager.domain.entity.AuditAction;
import com.s3manager.domain.entity.User;
import com.s3manager.dto.audit.AuditLogDTO;
import com.s3manager.dto.common.ApiResponse;
import com.s3manager.dto.common.PageResponse;
import com.s3manager.repository.UserRepository;
import com.s3manager.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final UserRepository userRepository;

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogDTO>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            Authentication authentication) {

        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PageResponse<AuditLogDTO> logs = auditService.getUserAuditLogs(
                user, page, size, action, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AuditStats>> getAuditStats(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuditStats stats = AuditStats.builder()
                .successfulOperations(auditService.getSuccessfulOperationsCount(user))
                .failedOperations(auditService.getFailedOperationsCount(user))
                .build();

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @lombok.Data
    @lombok.Builder
    private static class AuditStats {
        private long successfulOperations;
        private long failedOperations;
    }
}