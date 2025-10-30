package com.s3manager.controller;

import com.s3manager.dto.common.ApiResponse;
import com.s3manager.dto.s3.BucketDTO;
import com.s3manager.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buckets")
@RequiredArgsConstructor
public class S3BucketController {

    private final S3Service s3Service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BucketDTO>>> listBuckets(
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        List<BucketDTO> buckets = s3Service.listBuckets(authentication.getName(), credentialId);
        return ResponseEntity.ok(ApiResponse.success(buckets));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createBucket(
            @RequestParam String bucketName,
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        s3Service.createBucket(authentication.getName(), bucketName, credentialId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bucket created successfully", null));
    }

    @DeleteMapping("/{bucketName}")
    public ResponseEntity<ApiResponse<Void>> deleteBucket(
            @PathVariable String bucketName,
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        s3Service.deleteBucket(authentication.getName(), bucketName, credentialId);
        return ResponseEntity.ok(ApiResponse.success("Bucket deleted successfully", null));
    }
}
