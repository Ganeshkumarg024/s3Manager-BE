package com.s3manager.dto.credential;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3CredentialRequest {

    @NotBlank(message = "Alias is required")
    @Size(max = 100, message = "Alias must not exceed 100 characters")
    private String alias;

    @NotBlank(message = "Access key is required")
    private String accessKey;

    @NotBlank(message = "Secret key is required")
    private String secretKey;

    @NotBlank(message = "Region is required")
    private String region;

    private String endpoint;

    private Boolean isDefault = false;
}