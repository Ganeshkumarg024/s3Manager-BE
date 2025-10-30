package com.s3manager.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListObjectsResponse {
    private List<S3ObjectDTO> objects;
    private List<String> commonPrefixes;
    private String nextContinuationToken;
    private Boolean isTruncated;
    private Integer keyCount;
}
