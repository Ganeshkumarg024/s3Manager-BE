package com.s3manager.service;

import com.s3manager.domain.entity.S3Credential;
import com.s3manager.domain.entity.User;
import com.s3manager.dto.analytics.*;
import com.s3manager.dto.s3.BucketDTO;
import com.s3manager.dto.s3.S3ObjectDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final S3CredentialService credentialService;
    private final S3Service s3Service;
    private final AuditService auditService;

    @Cacheable(value = "storageAnalytics", key = "#username")
    public StorageAnalyticsDTO getStorageAnalytics(String username, UUID credentialId) {
        log.info("Generating storage analytics for user: {}", username);

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            // Get all buckets
            List<BucketDTO> buckets = s3Service.listBuckets(username, credentialId);

            Map<String, Long> sizeByBucket = new HashMap<>();
            Map<String, Long> objectsByBucket = new HashMap<>();
            Map<String, Long> sizeByFileType = new HashMap<>();
            List<LargestFileDTO> largestFiles = new ArrayList<>();
            List<OldestFileDTO> oldestFiles = new ArrayList<>();

            long totalSize = 0;
            long totalObjects = 0;

            // Analyze each bucket
            for (BucketDTO bucket : buckets) {
                BucketAnalysis analysis = analyzeBucket(s3Client, bucket.getName());

                sizeByBucket.put(bucket.getName(), analysis.totalSize);
                objectsByBucket.put(bucket.getName(), analysis.objectCount);

                totalSize += analysis.totalSize;
                totalObjects += analysis.objectCount;

                // Merge file type statistics
                analysis.sizeByType.forEach((type, size) ->
                        sizeByFileType.merge(type, size, Long::sum)
                );

                largestFiles.addAll(analysis.largestFiles);
                oldestFiles.addAll(analysis.oldestFiles);
            }

            // Sort and limit largest files
            largestFiles = largestFiles.stream()
                    .sorted(Comparator.comparing(LargestFileDTO::getSize).reversed())
                    .limit(10)
                    .collect(Collectors.toList());

            // Sort and limit oldest files
            oldestFiles = oldestFiles.stream()
                    .sorted(Comparator.comparing(OldestFileDTO::getLastModified))
                    .limit(10)
                    .collect(Collectors.toList());

            return StorageAnalyticsDTO.builder()
                    .totalSize(totalSize)
                    .totalObjects(totalObjects)
                    .totalBuckets(buckets.size())
                    .sizeByBucket(sizeByBucket)
                    .objectsByBucket(objectsByBucket)
                    .sizeByFileType(sizeByFileType)
                    .largestFiles(largestFiles)
                    .oldestFiles(oldestFiles)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate storage analytics: {}", e.getMessage());
            throw new RuntimeException("Failed to generate storage analytics: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    private BucketAnalysis analyzeBucket(S3Client s3Client, String bucketName) {
        log.debug("Analyzing bucket: {}", bucketName);

        BucketAnalysis analysis = new BucketAnalysis();
        analysis.sizeByType = new HashMap<>();
        analysis.largestFiles = new ArrayList<>();
        analysis.oldestFiles = new ArrayList<>();

        String continuationToken = null;

        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(1000);

            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

            for (S3Object s3Object : response.contents()) {
                // Skip folders
                if (s3Object.key().endsWith("/")) {
                    continue;
                }

                analysis.totalSize += s3Object.size();
                analysis.objectCount++;

                // Analyze file type
                String fileType = getFileType(s3Object.key());
                analysis.sizeByType.merge(fileType, s3Object.size(), Long::sum);

                // Track largest files
                analysis.largestFiles.add(LargestFileDTO.builder()
                        .key(s3Object.key())
                        .bucket(bucketName)
                        .size(s3Object.size())
                        .contentType(fileType)
                        .build());

                // Track oldest files
                analysis.oldestFiles.add(OldestFileDTO.builder()
                        .key(s3Object.key())
                        .bucket(bucketName)
                        .lastModified(s3Object.lastModified())
                        .build());
            }

            continuationToken = response.nextContinuationToken();

        } while (continuationToken != null);

        // Keep only top items
        analysis.largestFiles = analysis.largestFiles.stream()
                .sorted(Comparator.comparing(LargestFileDTO::getSize).reversed())
                .limit(5)
                .collect(Collectors.toList());

        analysis.oldestFiles = analysis.oldestFiles.stream()
                .sorted(Comparator.comparing(OldestFileDTO::getLastModified))
                .limit(5)
                .collect(Collectors.toList());

        return analysis;
    }

    private String getFileType(String key) {
        int lastDot = key.lastIndexOf('.');
        if (lastDot == -1 || lastDot == key.length() - 1) {
            return "unknown";
        }
        return key.substring(lastDot + 1).toLowerCase();
    }

    private S3Credential getCredential(String username, UUID credentialId) {
        if (credentialId != null) {
            return credentialService.getCredentialByIdInternal(username, credentialId);
        } else {
            return credentialService.getDefaultCredential(username);
        }
    }

    // Helper class for bucket analysis
    private static class BucketAnalysis {
        long totalSize = 0;
        long objectCount = 0;
        Map<String, Long> sizeByType;
        List<LargestFileDTO> largestFiles;
        List<OldestFileDTO> oldestFiles;
    }
}