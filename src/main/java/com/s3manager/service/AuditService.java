package com.s3manager.service;

import com.s3manager.domain.entity.AuditAction;
import com.s3manager.domain.entity.AuditLog;
import com.s3manager.domain.entity.AuditStatus;
import com.s3manager.domain.entity.User;
import com.s3manager.dto.audit.AuditLogDTO;
import com.s3manager.dto.common.PageResponse;
import com.s3manager.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    // ================== Authentication Audit ==================

    @Async
    @Transactional
    public void logLoginSuccess(User user) {
        createAuditLog(user, AuditAction.LOGIN, null, null, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logLoginFailure(User user) {
        createAuditLog(user, AuditAction.LOGIN_FAILED, null, null, AuditStatus.FAILURE, "Invalid credentials");
    }

    @Async
    @Transactional
    public void logLogout(User user) {
        createAuditLog(user, AuditAction.LOGOUT, null, null, AuditStatus.SUCCESS, null);
    }

    // ================== Bucket Audit ==================

    @Async
    @Transactional
    public void logCreateBucket(User user, String bucketName) {
        createAuditLog(user, AuditAction.CREATE_BUCKET, bucketName, null, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logDeleteBucket(User user, String bucketName) {
        createAuditLog(user, AuditAction.DELETE_BUCKET, bucketName, null, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logListBuckets(User user, int count) {
        createAuditLog(user, AuditAction.LIST_BUCKETS, null, null, AuditStatus.SUCCESS,
                String.format("Listed %d buckets", count));
    }

    // ================== Object Audit ==================

    @Async
    @Transactional
    public void logUploadObject(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.UPLOAD_OBJECT, bucketName, objectKey, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logDownloadObject(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.DOWNLOAD_OBJECT, bucketName, objectKey, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logDeleteObject(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.DELETE_OBJECT, bucketName, objectKey, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logCopyObject(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.COPY_OBJECT, bucketName, objectKey, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logMoveObject(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.MOVE_OBJECT, bucketName, objectKey, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logRenameObject(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.RENAME_OBJECT, bucketName, objectKey, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logListObjects(User user, String bucketName) {
        createAuditLog(user, AuditAction.LIST_OBJECTS, bucketName, null, AuditStatus.SUCCESS, null);
    }

    // ================== Credential Audit ==================

    @Async
    @Transactional
    public void logCreateCredential(User user, String alias) {
        createAuditLog(user, AuditAction.CREATE_CREDENTIAL, null, alias, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logUpdateCredential(User user, String alias) {
        createAuditLog(user, AuditAction.UPDATE_CREDENTIAL, null, alias, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logDeleteCredential(User user, String alias) {
        createAuditLog(user, AuditAction.DELETE_CREDENTIAL, null, alias, AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logValidateCredential(User user, String alias, boolean success) {
        createAuditLog(user, AuditAction.VALIDATE_CREDENTIAL, null, alias,
                success ? AuditStatus.SUCCESS : AuditStatus.FAILURE, null);
    }

    // ================== Preview Audit ==================

    @Async
    @Transactional
    public void logGeneratePresignedUrl(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.GENERATE_PRESIGNED_URL, bucketName, objectKey,
                AuditStatus.SUCCESS, null);
    }

    @Async
    @Transactional
    public void logPreviewObject(User user, String bucketName, String objectKey) {
        createAuditLog(user, AuditAction.PREVIEW_OBJECT, bucketName, objectKey, AuditStatus.SUCCESS, null);
    }

    // ================== Analytics Audit ==================

    @Async
    @Transactional
    public void logViewAnalytics(User user) {
        createAuditLog(user, AuditAction.VIEW_ANALYTICS, null, null, AuditStatus.SUCCESS, null);
    }

    // ================== Query Methods ==================

    @Transactional(readOnly = true)
    public PageResponse<AuditLogDTO> getUserAuditLogs(
            User user,
            int page,
            int size,
            AuditAction action,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        Page<AuditLog> auditPage;

        if (action != null) {
            auditPage = auditLogRepository.findByUserAndAction(user, action, pageable);
        } else if (startDate != null && endDate != null) {
            auditPage = auditLogRepository.findByUserAndTimestampBetween(
                    user, startDate, endDate, pageable);
        } else {
            auditPage = auditLogRepository.findByUser(user, pageable);
        }

        List<AuditLogDTO> content = auditPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return PageResponse.<AuditLogDTO>builder()
                .content(content)
                .pageNumber(auditPage.getNumber())
                .pageSize(auditPage.getSize())
                .totalElements(auditPage.getTotalElements())
                .totalPages(auditPage.getTotalPages())
                .last(auditPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public long getSuccessfulOperationsCount(User user) {
        return auditLogRepository.countByUserAndStatus(user, AuditStatus.SUCCESS);
    }

    @Transactional(readOnly = true)
    public long getFailedOperationsCount(User user) {
        return auditLogRepository.countByUserAndStatus(user, AuditStatus.FAILURE);
    }

    // ================== Cleanup ==================

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        auditLogRepository.deleteByTimestampBefore(threshold);
        log.info("Cleaned up audit logs older than 90 days");
    }

    // ================== Helper Methods ==================

    private void createAuditLog(
            User user,
            AuditAction action,
            String bucketName,
            String objectKey,
            AuditStatus status,
            String errorMessage) {

        HttpServletRequest request = getCurrentRequest();

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .bucketName(bucketName)
                .objectKey(objectKey)
                .timestamp(LocalDateTime.now())
                .status(status)
                .errorMessage(errorMessage)
                .ipAddress(request != null ? getClientIpAddress(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

        auditLogRepository.save(auditLog);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AuditLogDTO mapToDTO(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .bucketName(auditLog.getBucketName())
                .objectKey(auditLog.getObjectKey())
                .timestamp(auditLog.getTimestamp())
                .status(auditLog.getStatus())
                .errorMessage(auditLog.getErrorMessage())
                .ipAddress(auditLog.getIpAddress())
                .build();
    }
}