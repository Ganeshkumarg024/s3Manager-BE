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
public class CopyMoveRequest {
    private String sourceBucket;
    private String sourceKey;
    private String destinationBucket;
    private String destinationKey;
    private UUID credentialId;
}

