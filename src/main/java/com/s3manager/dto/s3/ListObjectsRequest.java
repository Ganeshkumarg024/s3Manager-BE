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
public class ListObjectsRequest {
    private String bucket;
    private String prefix;
    private String delimiter;
    private Integer maxKeys;
    private String continuationToken;
    private UUID credentialId;
}
