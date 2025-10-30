package com.s3manager.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "s3_credentials", indexes = {
        @Index(name = "idx_credential_user", columnList = "user_id"),
        @Index(name = "idx_credential_alias", columnList = "alias")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class S3Credential extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(nullable = false, length = 500)
    private String accessKey;

    @Column(nullable = false, length = 1000)
    private String secretKey; // Encrypted

    @Column(nullable = false, length = 50)
    private String region;

    @Column(length = 200)
    private String endpoint; // For S3-compatible services

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(nullable = false)
    private Boolean isActive = true;

    private LocalDateTime lastValidatedAt;
}