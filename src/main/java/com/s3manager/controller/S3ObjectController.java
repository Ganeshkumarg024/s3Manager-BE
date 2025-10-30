package com.s3manager.controller;

import com.s3manager.dto.common.ApiResponse;
import com.s3manager.dto.s3.*;
import com.s3manager.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/objects")
@RequiredArgsConstructor
public class S3ObjectController {

    private final S3Service s3Service;

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<ListObjectsResponse>> listObjects(
            @Valid @RequestBody ListObjectsRequest request,
            Authentication authentication) {
        ListObjectsResponse response = s3Service.listObjects(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadObject(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        UploadResponse response = s3Service.uploadObject(
                authentication.getName(), bucket, key, file, credentialId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Object uploaded successfully", response));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadObject(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        byte[] data = s3Service.downloadObject(authentication.getName(), bucket, key, credentialId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteObject(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        s3Service.deleteObject(authentication.getName(), bucket, key, credentialId);
        return ResponseEntity.ok(ApiResponse.success("Object deleted successfully", null));
    }

    @PostMapping("/copy")
    public ResponseEntity<ApiResponse<Void>> copyObject(
            @Valid @RequestBody CopyMoveRequest request,
            Authentication authentication) {
        s3Service.copyObject(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Object copied successfully", null));
    }

    @PostMapping("/move")
    public ResponseEntity<ApiResponse<Void>> moveObject(
            @Valid @RequestBody CopyMoveRequest request,
            Authentication authentication) {
        s3Service.moveObject(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Object moved successfully", null));
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request,
            Authentication authentication) {
        PresignedUrlResponse response = s3Service.generatePresignedUrl(
                authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/metadata")
    public ResponseEntity<ApiResponse<S3ObjectDTO>> getObjectMetadata(
            @RequestParam String bucket,
            @RequestParam String key,
            @RequestParam(required = false) UUID credentialId,
            Authentication authentication) {
        S3ObjectDTO metadata = s3Service.getObjectMetadata(
                authentication.getName(), bucket, key, credentialId);
        return ResponseEntity.ok(ApiResponse.success(metadata));
    }
}