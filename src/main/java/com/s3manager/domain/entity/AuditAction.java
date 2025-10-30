package com.s3manager.domain.entity;

public enum AuditAction {
    // Authentication
    LOGIN,
    LOGOUT,
    LOGIN_FAILED,

    // Bucket Operations
    CREATE_BUCKET,
    DELETE_BUCKET,
    LIST_BUCKETS,

    // Object Operations
    UPLOAD_OBJECT,
    DOWNLOAD_OBJECT,
    DELETE_OBJECT,
    COPY_OBJECT,
    MOVE_OBJECT,
    RENAME_OBJECT,
    LIST_OBJECTS,

    // Credential Operations
    CREATE_CREDENTIAL,
    UPDATE_CREDENTIAL,
    DELETE_CREDENTIAL,
    VALIDATE_CREDENTIAL,

    // Preview Operations
    GENERATE_PRESIGNED_URL,
    PREVIEW_OBJECT,

    // Analytics
    VIEW_ANALYTICS,
    EXPORT_ANALYTICS
}
