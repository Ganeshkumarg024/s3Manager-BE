package com.s3manager.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;
import java.util.UUID;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadRequest {
    private String bucket;
    private String key;
    private String contentType;
    private Map<String, String> metadata;
    private UUID credentialId;
}

