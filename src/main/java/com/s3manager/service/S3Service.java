package com.s3manager.service;

import com.s3manager.domain.entity.S3Credential;
import com.s3manager.domain.entity.User;
import com.s3manager.dto.s3.*;
import com.s3manager.dto.s3.ListObjectsRequest;
import com.s3manager.dto.s3.ListObjectsResponse;
import com.s3manager.exception.S3OperationException;
import com.s3manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3CredentialService credentialService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Value("${app.s3.presigned-url-expiration}")
    private int presignedUrlExpiration;

    @Value("${app.s3.max-upload-size}")
    private long maxUploadSize;

    // ================== Bucket Operations ==================

    public List<BucketDTO> listBuckets(String username, UUID credentialId) {
        log.info("Listing buckets for user: {}", username);

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            ListBucketsResponse response = s3Client.listBuckets();

            List<BucketDTO> buckets = response.buckets().stream()
                    .map(bucket -> BucketDTO.builder()
                            .name(bucket.name())
                            .creationDate(bucket.creationDate())
                            .region(credential.getRegion())
                            .build())
                    .collect(Collectors.toList());

            User user = getUser(username);
            auditService.logListBuckets(user, buckets.size());

            log.info("Listed {} buckets for user: {}", buckets.size(), username);
            return buckets;

        } catch (Exception e) {
            log.error("Failed to list buckets: {}", e.getMessage());
            throw new S3OperationException("Failed to list buckets: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    public void createBucket(String username, String bucketName, UUID credentialId) {
        log.info("Creating bucket: {} for user: {}", bucketName, username);

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.createBucket(request);

            User user = getUser(username);
            auditService.logCreateBucket(user, bucketName);

            log.info("Bucket created successfully: {}", bucketName);

        } catch (Exception e) {
            log.error("Failed to create bucket: {}", e.getMessage());
            throw new S3OperationException("Failed to create bucket: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    public void deleteBucket(String username, String bucketName, UUID credentialId) {
        log.info("Deleting bucket: {} for user: {}", bucketName, username);

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.deleteBucket(request);

            User user = getUser(username);
            auditService.logDeleteBucket(user, bucketName);

            log.info("Bucket deleted successfully: {}", bucketName);

        } catch (Exception e) {
            log.error("Failed to delete bucket: {}", e.getMessage());
            throw new S3OperationException("Failed to delete bucket: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    // ================== Object Operations ==================

    public ListObjectsResponse listObjects(String username, ListObjectsRequest request) {
        log.info("Listing objects in bucket: {} for user: {}", request.getBucket(), username);

        S3Credential credential = getCredential(username, request.getCredentialId());
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            ListObjectsV2Request.Builder listBuilder = ListObjectsV2Request.builder()
                    .bucket(request.getBucket());

            if (request.getPrefix() != null) {
                listBuilder.prefix(request.getPrefix());
            }
            if (request.getDelimiter() != null) {
                listBuilder.delimiter(request.getDelimiter());
            }
            if (request.getMaxKeys() != null) {
                listBuilder.maxKeys(request.getMaxKeys());
            }
            if (request.getContinuationToken() != null) {
                listBuilder.continuationToken(request.getContinuationToken());
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(listBuilder.build());

            List<S3ObjectDTO> objects = response.contents().stream()
                    .map(s3Object -> S3ObjectDTO.builder()
                            .key(s3Object.key())
                            .bucket(request.getBucket())
                            .size(s3Object.size())
                            .lastModified(s3Object.lastModified())
                            .eTag(s3Object.eTag())
                            .storageClass(s3Object.storageClassAsString())
                            .isFolder(s3Object.key().endsWith("/"))
                            .build())
                    .collect(Collectors.toList());

            List<String> commonPrefixes = response.commonPrefixes().stream()
                    .map(CommonPrefix::prefix)
                    .collect(Collectors.toList());

            User user = getUser(username);
            auditService.logListObjects(user, request.getBucket());

            return ListObjectsResponse.builder()
                    .objects(objects)
                    .commonPrefixes(commonPrefixes)
                    .nextContinuationToken(response.nextContinuationToken())
                    .isTruncated(response.isTruncated())
                    .keyCount(response.keyCount())
                    .build();

        } catch (Exception e) {
            log.error("Failed to list objects: {}", e.getMessage());
            throw new S3OperationException("Failed to list objects: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    public UploadResponse uploadObject(
            String username,
            String bucket,
            String key,
            MultipartFile file,
            UUID credentialId) {

        log.info("Uploading object: {} to bucket: {} for user: {}", key, bucket, username);

        if (file.getSize() > maxUploadSize) {
            throw new S3OperationException("File size exceeds maximum allowed size");
        }

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType());

            PutObjectResponse response = s3Client.putObject(
                    putBuilder.build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            User user = getUser(username);
            auditService.logUploadObject(user, bucket, key);

            log.info("Object uploaded successfully: {}", key);

            return UploadResponse.builder()
                    .key(key)
                    .bucket(bucket)
                    .eTag(response.eTag())
                    .versionId(response.versionId())
                    .size(file.getSize())
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload object: {}", e.getMessage());
            throw new S3OperationException("Failed to upload object: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    public byte[] downloadObject(String username, String bucket, String key, UUID credentialId) {
        log.info("Downloading object: {} from bucket: {} for user: {}", key, bucket, username);

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            byte[] data = s3Client.getObjectAsBytes(request).asByteArray();

            User user = getUser(username);
            auditService.logDownloadObject(user, bucket, key);

            log.info("Object downloaded successfully: {}", key);
            return data;

        } catch (Exception e) {
            log.error("Failed to download object: {}", e.getMessage());
            throw new S3OperationException("Failed to download object: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    public void deleteObject(String username, String bucket, String key, UUID credentialId) {
        log.info("Deleting object: {} from bucket: {} for user: {}", key, bucket, username);

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);

            User user = getUser(username);
            auditService.logDeleteObject(user, bucket, key);

            log.info("Object deleted successfully: {}", key);

        } catch (Exception e) {
            log.error("Failed to delete object: {}", e.getMessage());
            throw new S3OperationException("Failed to delete object: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    public void copyObject(String username, CopyMoveRequest request) {
        log.info("Copying object from {}:{} to {}:{} for user: {}",
                request.getSourceBucket(), request.getSourceKey(),
                request.getDestinationBucket(), request.getDestinationKey(), username);

        S3Credential credential = getCredential(username, request.getCredentialId());
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(request.getSourceBucket())
                    .sourceKey(request.getSourceKey())
                    .destinationBucket(request.getDestinationBucket())
                    .destinationKey(request.getDestinationKey())
                    .build();

            s3Client.copyObject(copyRequest);

            User user = getUser(username);
            auditService.logCopyObject(user, request.getSourceBucket(), request.getSourceKey());

            log.info("Object copied successfully");

        } catch (Exception e) {
            log.error("Failed to copy object: {}", e.getMessage());
            throw new S3OperationException("Failed to copy object: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    public void moveObject(String username, CopyMoveRequest request) {
        log.info("Moving object from {}:{} to {}:{} for user: {}",
                request.getSourceBucket(), request.getSourceKey(),
                request.getDestinationBucket(), request.getDestinationKey(), username);

        // Copy first
        copyObject(username, request);

        // Then delete source
        deleteObject(username, request.getSourceBucket(),
                request.getSourceKey(), request.getCredentialId());

        User user = getUser(username);
        auditService.logMoveObject(user, request.getSourceBucket(), request.getSourceKey());

        log.info("Object moved successfully");
    }

    public PresignedUrlResponse generatePresignedUrl(
            String username,
            PresignedUrlRequest request) {

        log.info("Generating presigned URL for {}:{} for user: {}",
                request.getBucket(), request.getKey(), username);

        S3Credential credential = getCredential(username, request.getCredentialId());

        try {
            S3Presigner presigner = S3Presigner.create();

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(request.getBucket())
                    .key(request.getKey())
                    .build();

            Duration duration = Duration.ofSeconds(
                    request.getExpirationSeconds() != null ?
                            request.getExpirationSeconds() : presignedUrlExpiration
            );

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

            User user = getUser(username);
            auditService.logGeneratePresignedUrl(user, request.getBucket(), request.getKey());

            presigner.close();

            return PresignedUrlResponse.builder()
                    .url(presignedRequest.url().toString())
                    .expiresAt(Instant.now().plus(duration))
                    .key(request.getKey())
                    .bucket(request.getBucket())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            throw new S3OperationException("Failed to generate presigned URL: " + e.getMessage());
        }
    }

    public S3ObjectDTO getObjectMetadata(String username, String bucket, String key, UUID credentialId) {
        log.info("Getting metadata for object: {} in bucket: {}", key, bucket);

        S3Credential credential = getCredential(username, credentialId);
        S3Client s3Client = credentialService.createS3Client(credential);

        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);

            return S3ObjectDTO.builder()
                    .key(key)
                    .bucket(bucket)
                    .size(response.contentLength())
                    .lastModified(response.lastModified())
                    .eTag(response.eTag())
                    .contentType(response.contentType())
                    .metadata(response.metadata())
                    .storageClass(response.storageClassAsString())
                    .build();

        } catch (Exception e) {
            log.error("Failed to get object metadata: {}", e.getMessage());
            throw new S3OperationException("Failed to get object metadata: " + e.getMessage());
        } finally {
            s3Client.close();
        }
    }

    // ================== Helper Methods ==================

    private S3Credential getCredential(String username, UUID credentialId) {
        if (credentialId != null) {
            return credentialService.getCredentialByIdInternal(username, credentialId);
        } else {
            return credentialService.getDefaultCredential(username);
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new S3OperationException("User not found"));
    }
}