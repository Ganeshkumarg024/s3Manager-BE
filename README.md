# s3Manager-BE
# S3 Manager - Production-Ready Backend

A comprehensive, secure, and scalable Spring Boot backend for managing AWS S3 and S3-compatible storage services.

## 🚀 Features

### Core Functionality
- ✅ **User Authentication & Authorization** - JWT-based with refresh tokens
- ✅ **Multi-Credential Management** - Store and manage multiple S3 accounts per user
- ✅ **Complete S3 Operations** - Upload, download, delete, copy, move, list
- ✅ **Bucket Management** - Create, delete, and list buckets
- ✅ **Presigned URLs** - Secure, time-limited access to objects
- ✅ **Storage Analytics** - Comprehensive usage statistics and insights
- ✅ **Audit Logging** - Complete activity tracking with AOP
- ✅ **Encrypted Credentials** - Jasypt encryption for sensitive data

### Security Features
- 🔐 JWT authentication with refresh tokens
- 🔐 BCrypt password hashing (strength 12)
- 🔐 Role-based access control (ADMIN/USER)
- 🔐 AES-256 credential encryption
- 🔐 Account lockout after failed attempts
- 🔐 CORS configuration
- 🔐 SQL injection prevention

### Production-Ready
- 📊 Prometheus metrics
- 📊 Health checks
- 📊 Async operations
- 📊 Connection pooling
- 📊 Caching strategy
- 📊 Error handling
- 📊 Request/Response logging
- 📊 Database migrations (Flyway)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│           REST API Layer                     │
│  (Controllers + Exception Handlers)          │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│          Service Layer                       │
│  • AuthService                               │
│  • S3Service                                 │
│  • S3CredentialService                       │
│  • AnalyticsService                          │
│  • AuditService                              │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│      Repository Layer (Spring Data JPA)     │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│         PostgreSQL Database                  │
└─────────────────────────────────────────────┘
```

## 📋 Prerequisites

- Java 17+
- PostgreSQL 15+
- Gradle 8+
- Docker & Docker Compose (optional)

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd s3-manager-backend
```

### 2. Configure Environment Variables

Create a `.env` file:
```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=s3manager
DB_USER=postgres
DB_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_very_long_secure_jwt_secret_key_minimum_512_bits
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Encryption
JASYPT_PASSWORD=your_secure_encryption_password

# CORS
CORS_ORIGINS=http://localhost:3000,http://localhost:5173

# Server
SERVER_PORT=8080
```

### 3. Database Setup

**Option A: Using Docker**
```bash
docker run -d \
  --name s3manager-postgres \
  -e POSTGRES_DB=s3manager \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

**Option B: Local PostgreSQL**
```sql
CREATE DATABASE s3manager;
```

### 4. Build the Application

```bash
# Using Gradle Wrapper
./gradlew clean build

# Skip tests for faster build
./gradlew clean build -x test
```

### 5. Run the Application

**Option A: Using Gradle**
```bash
./gradlew bootRun
```

**Option B: Using JAR**
```bash
java -jar build/libs/s3-manager-1.0.0.jar
```

**Option C: Using Docker Compose**
```bash
docker-compose up -d
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123"
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "johndoe",
  "password": "SecurePass123"
}
```

Response:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": "uuid",
      "username": "johndoe",
      "email": "john@example.com",
      "role": "USER"
    }
  }
}
```

#### Refresh Token
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "your_refresh_token"
}
```

### S3 Credential Endpoints

#### Create Credential
```http
POST /api/v1/credentials
Authorization: Bearer <token>
Content-Type: application/json

{
  "alias": "My AWS Account",
  "accessKey": "AKIAIOSFODNN7EXAMPLE",
  "secretKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY",
  "region": "us-east-1",
  "endpoint": null,
  "isDefault": true
}
```

#### List Credentials
```http
GET /api/v1/credentials
Authorization: Bearer <token>
```

#### Validate Credential
```http
POST /api/v1/credentials/{credentialId}/validate
Authorization: Bearer <token>
```

### Bucket Endpoints

#### List Buckets
```http
GET /api/v1/buckets?credentialId=<uuid>
Authorization: Bearer <token>
```

#### Create Bucket
```http
POST /api/v1/buckets?bucketName=my-bucket&credentialId=<uuid>
Authorization: Bearer <token>
```

#### Delete Bucket
```http
DELETE /api/v1/buckets/{bucketName}?credentialId=<uuid>
Authorization: Bearer <token>
```

### Object Endpoints

#### List Objects
```http
POST /api/v1/objects/list
Authorization: Bearer <token>
Content-Type: application/json

{
  "bucket": "my-bucket",
  "prefix": "folder/",
  "delimiter": "/",
  "maxKeys": 1000,
  "credentialId": "uuid"
}
```

#### Upload Object
```http
POST /api/v1/objects/upload
Authorization: Bearer <token>
Content-Type: multipart/form-data

bucket: my-bucket
key: folder/file.txt
file: <file>
credentialId: <uuid>
```

#### Download Object
```http
GET /api/v1/objects/download?bucket=my-bucket&key=file.txt&credentialId=<uuid>
Authorization: Bearer <token>
```

#### Delete Object
```http
DELETE /api/v1/objects?bucket=my-bucket&key=file.txt&credentialId=<uuid>
Authorization: Bearer <token>
```

#### Copy Object
```http
POST /api/v1/objects/copy
Authorization: Bearer <token>
Content-Type: application/json

{
  "sourceBucket": "source-bucket",
  "sourceKey": "source.txt",
  "destinationBucket": "dest-bucket",
  "destinationKey": "destination.txt",
  "credentialId": "uuid"
}
```

#### Generate Presigned URL
```http
POST /api/v1/objects/presigned-url
Authorization: Bearer <token>
Content-Type: application/json

{
  "bucket": "my-bucket",
  "key": "file.txt",
  "expirationSeconds": 3600,
  "credentialId": "uuid"
}
```

### Analytics Endpoints

#### Get Storage Analytics
```http
GET /api/v1/analytics/storage?credentialId=<uuid>
Authorization: Bearer <token>
```

Response:
```json
{
  "success": true,
  "data": {
    "totalSize": 1073741824,
    "totalObjects": 1500,
    "totalBuckets": 5,
    "sizeByBucket": {
      "bucket1": 536870912,
      "bucket2": 268435456
    },
    "objectsByBucket": {
      "bucket1": 800,
      "bucket2": 700
    },
    "sizeByFileType": {
      "jpg": 314572800,
      "pdf": 209715200
    },
    "largestFiles": [...],
    "oldestFiles": [...]
  }
}
```

### Audit Endpoints

#### Get Audit Logs
```http
GET /api/v1/audit/logs?page=0&size=20&action=UPLOAD_OBJECT
Authorization: Bearer <token>
```

#### Get Audit Statistics
```http
GET /api/v1/audit/stats
Authorization: Bearer <token>
```






<div align="center">

**⭐ Star this repo if you find it helpful!**

Made with ❤️ by [Ganesh Kumar](https://github.com/Ganeshkumarg024)

</div>






