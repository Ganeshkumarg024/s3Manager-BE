package com.s3manager.repository;

import com.s3manager.domain.entity.S3Credential;
import com.s3manager.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface S3CredentialRepository extends JpaRepository<S3Credential, UUID> {

    List<S3Credential> findByUserAndIsActiveTrue(User user);

    List<S3Credential> findByUser(User user);

    Optional<S3Credential> findByIdAndUser(UUID id, User user);

    Optional<S3Credential> findByUserAndIsDefaultTrue(User user);

    boolean existsByUserAndAlias(User user, String alias);

    @Modifying
    @Query("UPDATE S3Credential c SET c.isDefault = false WHERE c.user = ?1")
    void clearDefaultCredentials(User user);

    @Modifying
    @Query("UPDATE S3Credential c SET c.isActive = false WHERE c.id = ?1")
    void softDelete(UUID credentialId);

    long countByUser(User user);
}