package com.s3manager.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageAnalyticsDTO {
    private Long totalSize;
    private Long totalObjects;
    private Integer totalBuckets;
    private Map<String, Long> sizeByBucket;
    private Map<String, Long> objectsByBucket;
    private Map<String, Long> sizeByFileType;
    private List<LargestFileDTO> largestFiles;
    private List<OldestFileDTO> oldestFiles;
}

