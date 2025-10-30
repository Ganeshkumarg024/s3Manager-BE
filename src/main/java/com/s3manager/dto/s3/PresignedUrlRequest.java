package com.s3manager.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {
    private String bucket;
    private String key;
    private Integer expirationSeconds;
    private UUID credentialId;
}

