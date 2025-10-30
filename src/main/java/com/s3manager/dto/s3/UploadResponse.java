package com.s3manager.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {
    private String key;
    private String bucket;
    private String eTag;
    private String versionId;
    private Long size;
}

