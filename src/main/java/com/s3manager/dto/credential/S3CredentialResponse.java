package com.s3manager.dto.credential;

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
public class S3CredentialResponse {
    private UUID id;
    private String alias;
    private String accessKey;
    private String region;
    private String endpoint;
    private Boolean isDefault;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime lastValidatedAt;
}

