package com.s3manager.dto.audit;

import com.s3manager.domain.entity.AuditAction;
import com.s3manager.domain.entity.AuditStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    private UUID id;
    private AuditAction action;
    private String bucketName;
    private String objectKey;
    private LocalDateTime timestamp;
    private AuditStatus status;
    private String errorMessage;
    private String ipAddress;
}
