package com.s3manager.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3ObjectDTO {
    private String key;
    private String bucket;
    private Long size;
    private Instant lastModified;
    private String eTag;
    private String storageClass;
    private String contentType;
    private Map<String, String> metadata;
    private Boolean isFolder;
}

