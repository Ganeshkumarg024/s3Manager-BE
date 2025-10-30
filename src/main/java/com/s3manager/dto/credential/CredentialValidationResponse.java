package com.s3manager.dto.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialValidationResponse {
    private Boolean valid;
    private String message;
    private LocalDateTime validatedAt;
}