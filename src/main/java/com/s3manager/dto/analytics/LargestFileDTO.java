package com.s3manager.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LargestFileDTO {
    private String key;
    private String bucket;
    private Long size;
    private String contentType;
}
