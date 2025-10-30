package com.s3manager.repository;

import com.s3manager.domain.entity.AuditAction;
import com.s3manager.domain.entity.AuditLog;
import com.s3manager.domain.entity.AuditStatus;
import com.s3manager.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByUser(User user, Pageable pageable);

    Page<AuditLog> findByUserAndAction(User user, AuditAction action, Pageable pageable);

    Page<AuditLog> findByUserAndTimestampBetween(
            User user,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    List<AuditLog> findByUserAndBucketNameAndAction(
            User user,
            String bucketName,
            AuditAction action
    );

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.user = ?1 AND a.status = ?2")
    long countByUserAndStatus(User user, AuditStatus status);

    @Query("SELECT a FROM AuditLog a WHERE a.timestamp < ?1")
    List<AuditLog> findOldLogs(LocalDateTime threshold);

    void deleteByTimestampBefore(LocalDateTime threshold);
}