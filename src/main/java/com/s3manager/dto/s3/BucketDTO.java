package com.s3manager.dto.s3;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BucketDTO {
    private String name;
    private Instant creationDate;
    private String region;
    private Long objectCount;
    private Long totalSize;
}




