package com.s3manager.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OldestFileDTO {
    private String key;
    private String bucket;
    private Instant lastModified;
}
