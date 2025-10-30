package com.s3manager.service;

import com.s3manager.domain.entity.S3Credential;
import com.s3manager.domain.entity.User;
import com.s3manager.dto.credential.*;
import com.s3manager.exception.BadRequestException;
import com.s3manager.exception.NotFoundException;
import com.s3manager.repository.S3CredentialRepository;
import com.s3manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;


import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3CredentialService {

    private final S3CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final StringEncryptor stringEncryptor;
    private final AuditService auditService;

    @Transactional
    public S3CredentialResponse createCredential(String username, S3CredentialRequest request) {
        log.info("Creating S3 credential for user: {}", username);

        User user = getUserByUsername(username);

        // Check if alias already exists for this user
        if (credentialRepository.existsByUserAndAlias(user, request.getAlias())) {
            throw new BadRequestException("Credential with this alias already exists");
        }

        // Validate credentials
        validateS3Credentials(request);

        // If this is set as default, clear other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            credentialRepository.clearDefaultCredentials(user);
        }

        // Encrypt secret key
        String encryptedSecretKey = stringEncryptor.encrypt(request.getSecretKey());

        S3Credential credential = S3Credential.builder()
                .user(user)
                .alias(request.getAlias())
                .accessKey(request.getAccessKey())
                .secretKey(encryptedSecretKey)
                .region(request.getRegion())
                .endpoint(request.getEndpoint())
                .isDefault(request.getIsDefault())
                .isActive(true)
                .lastValidatedAt(LocalDateTime.now())
                .build();

        credential = credentialRepository.save(credential);

        auditService.logCreateCredential(user, credential.getAlias());

        log.info("S3 credential created successfully: {} for user: {}",
                credential.getAlias(), username);

        return mapToResponse(credential);
    }

    @Transactional(readOnly = true)
    public List<S3CredentialResponse> getUserCredentials(String username) {
        User user = getUserByUsername(username);

        List<S3Credential> credentials = credentialRepository.findByUserAndIsActiveTrue(user);

        return credentials.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public S3CredentialResponse getCredentialById(String username, UUID credentialId) {
        User user = getUserByUsername(username);

        S3Credential credential = credentialRepository.findByIdAndUser(credentialId, user)
                .orElseThrow(() -> new NotFoundException("Credential not found"));

        return mapToResponse(credential);
    }

    @Transactional(readOnly = true)
    public S3Credential getDefaultCredential(String username) {
        User user = getUserByUsername(username);

        return credentialRepository.findByUserAndIsDefaultTrue(user)
                .orElseThrow(() -> new NotFoundException("No default credential set"));
    }

    @Transactional(readOnly = true)
    public S3Credential getCredentialByIdInternal(String username, UUID credentialId) {
        User user = getUserByUsername(username);

        return credentialRepository.findByIdAndUser(credentialId, user)
                .orElseThrow(() -> new NotFoundException("Credential not found"));
    }

    @Transactional
    public S3CredentialResponse updateCredential(
            String username,
            UUID credentialId,
            S3CredentialRequest request) {

        log.info("Updating S3 credential: {} for user: {}", credentialId, username);

        User user = getUserByUsername(username);

        S3Credential credential = credentialRepository.findByIdAndUser(credentialId, user)
                .orElseThrow(() -> new NotFoundException("Credential not found"));

        // Check if new alias conflicts with existing ones (excluding current)
        if (!credential.getAlias().equals(request.getAlias()) &&
                credentialRepository.existsByUserAndAlias(user, request.getAlias())) {
            throw new BadRequestException("Credential with this alias already exists");
        }

        // Validate new credentials
        validateS3Credentials(request);

        // If setting as default, clear other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !credential.getIsDefault()) {
            credentialRepository.clearDefaultCredentials(user);
        }

        // Update fields
        credential.setAlias(request.getAlias());
        credential.setAccessKey(request.getAccessKey());
        credential.setSecretKey(stringEncryptor.encrypt(request.getSecretKey()));
        credential.setRegion(request.getRegion());
        credential.setEndpoint(request.getEndpoint());
        credential.setIsDefault(request.getIsDefault());
        credential.setLastValidatedAt(LocalDateTime.now());

        credential = credentialRepository.save(credential);

        auditService.logUpdateCredential(user, credential.getAlias());

        log.info("S3 credential updated successfully: {}", credentialId);

        return mapToResponse(credential);
    }

    @Transactional
    public void deleteCredential(String username, UUID credentialId) {
        log.info("Deleting S3 credential: {} for user: {}", credentialId, username);

        User user = getUserByUsername(username);

        S3Credential credential = credentialRepository.findByIdAndUser(credentialId, user)
                .orElseThrow(() -> new NotFoundException("Credential not found"));

        // Soft delete
        credentialRepository.softDelete(credentialId);

        auditService.logDeleteCredential(user, credential.getAlias());

        log.info("S3 credential deleted successfully: {}", credentialId);
    }

    @Transactional
    public void setDefaultCredential(String username, UUID credentialId) {
        log.info("Setting default credential: {} for user: {}", credentialId, username);

        User user = getUserByUsername(username);

        S3Credential credential = credentialRepository.findByIdAndUser(credentialId, user)
                .orElseThrow(() -> new NotFoundException("Credential not found"));

        credentialRepository.clearDefaultCredentials(user);
        credential.setIsDefault(true);
        credentialRepository.save(credential);

        log.info("Default credential set successfully: {}", credentialId);
    }

    @Transactional
    public CredentialValidationResponse validateCredential(String username, UUID credentialId) {
        log.info("Validating S3 credential: {} for user: {}", credentialId, username);

        User user = getUserByUsername(username);

        S3Credential credential = credentialRepository.findByIdAndUser(credentialId, user)
                .orElseThrow(() -> new NotFoundException("Credential not found"));

        try {
            // Test credentials by listing buckets
            S3Client s3Client = createS3Client(credential);
            s3Client.listBuckets();
            s3Client.close();

            // Update last validated timestamp
            credential.setLastValidatedAt(LocalDateTime.now());
            credentialRepository.save(credential);

            auditService.logValidateCredential(user, credential.getAlias(), true);

            return CredentialValidationResponse.builder()
                    .valid(true)
                    .message("Credentials are valid")
                    .validatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Credential validation failed: {}", e.getMessage());
            auditService.logValidateCredential(user, credential.getAlias(), false);

            return CredentialValidationResponse.builder()
                    .valid(false)
                    .message("Validation failed: " + e.getMessage())
                    .validatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private void validateS3Credentials(S3CredentialRequest request) {
        try {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                    request.getAccessKey(),
                    request.getSecretKey()
            );

            S3ClientBuilder s3Builder = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .region(Region.of(request.getRegion()));

            if (request.getEndpoint() != null && !request.getEndpoint().isEmpty()) {
                s3Builder.endpointOverride(URI.create(request.getEndpoint()));
            }

            S3Client s3Client = s3Builder.build();
            s3Client.listBuckets();
            s3Client.close();

        } catch (Exception e) {
            log.error("S3 credential validation failed: {}", e.getMessage());
            throw new BadRequestException("Invalid S3 credentials: " + e.getMessage());
        }
    }

    public S3Client createS3Client(S3Credential credential) {
        String decryptedSecretKey = stringEncryptor.decrypt(credential.getSecretKey());

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                credential.getAccessKey(),
                decryptedSecretKey
        );

        S3ClientBuilder s3Builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(credential.getRegion()));

        if (credential.getEndpoint() != null && !credential.getEndpoint().isEmpty()) {
            s3Builder.endpointOverride(URI.create(credential.getEndpoint()));
        }

        return s3Builder.build();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private S3CredentialResponse mapToResponse(S3Credential credential) {
        return S3CredentialResponse.builder()
                .id(credential.getId())
                .alias(credential.getAlias())
                .accessKey(credential.getAccessKey())
                .region(credential.getRegion())
                .endpoint(credential.getEndpoint())
                .isDefault(credential.getIsDefault())
                .isActive(credential.getIsActive())
                .createdAt(credential.getCreatedAt())
                .lastValidatedAt(credential.getLastValidatedAt())
                .build();
    }
}