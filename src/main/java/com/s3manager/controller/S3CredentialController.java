package com.s3manager.controller;

import com.s3manager.dto.common.ApiResponse;
import com.s3manager.dto.credential.*;
import com.s3manager.service.S3CredentialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credentials")
@RequiredArgsConstructor
public class S3CredentialController {

    private final S3CredentialService credentialService;

    @PostMapping
    public ResponseEntity<ApiResponse<S3CredentialResponse>> createCredential(
            @Valid @RequestBody S3CredentialRequest request,
            Authentication authentication) {
        S3CredentialResponse response = credentialService.createCredential(
                authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Credential created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<S3CredentialResponse>>> getUserCredentials(
            Authentication authentication) {
        List<S3CredentialResponse> credentials = credentialService.getUserCredentials(
                authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(credentials));
    }

    @GetMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<S3CredentialResponse>> getCredential(
            @PathVariable UUID credentialId,
            Authentication authentication) {
        S3CredentialResponse response = credentialService.getCredentialById(
                authentication.getName(), credentialId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<S3CredentialResponse>> updateCredential(
            @PathVariable UUID credentialId,
            @Valid @RequestBody S3CredentialRequest request,
            Authentication authentication) {
        S3CredentialResponse response = credentialService.updateCredential(
                authentication.getName(), credentialId, request);
        return ResponseEntity.ok(ApiResponse.success("Credential updated successfully", response));
    }

    @DeleteMapping("/{credentialId}")
    public ResponseEntity<ApiResponse<Void>> deleteCredential(
            @PathVariable UUID credentialId,
            Authentication authentication) {
        credentialService.deleteCredential(authentication.getName(), credentialId);
        return ResponseEntity.ok(ApiResponse.success("Credential deleted successfully", null));
    }

    @PostMapping("/{credentialId}/set-default")
    public ResponseEntity<ApiResponse<Void>> setDefaultCredential(
            @PathVariable UUID credentialId,
            Authentication authentication) {
        credentialService.setDefaultCredential(authentication.getName(), credentialId);
        return ResponseEntity.ok(ApiResponse.success("Default credential set successfully", null));
    }

    @PostMapping("/{credentialId}/validate")
    public ResponseEntity<ApiResponse<CredentialValidationResponse>> validateCredential(
            @PathVariable UUID credentialId,
            Authentication authentication) {
        CredentialValidationResponse response = credentialService.validateCredential(
                authentication.getName(), credentialId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
