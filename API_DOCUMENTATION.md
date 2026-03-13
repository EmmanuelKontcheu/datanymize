# Datanymize API Documentation

## Overview

The Datanymize API provides RESTful endpoints for managing database connections, extracting schemas, detecting PII, configuring anonymization rules, executing anonymization jobs, and exporting data.

### Base URL
```
http://localhost:8080/api
```

### Authentication
All endpoints require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

### Response Format
All responses follow a standard format:
```json
{
  "success": true,
  "data": { /* response data */ },
  "message": "Operation successful",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## Authentication Endpoints

### Login
```
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password"
}

Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400,
    "user": {
      "id": "user-123",
      "email": "user@example.com",
      "name": "John Doe"
    }
  }
}
```

### Logout
```
POST /auth/logout
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "message": "Logged out successfully"
}
```

### Refresh Token
```
POST /auth/refresh
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400
  }
}
```

## Connection Management Endpoints

### Create Connection
```
POST /connections
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Production Database",
  "type": "postgresql",
  "host": "db.example.com",
  "port": 5432,
  "database": "production",
  "username": "db_user",
  "password": "db_password",
  "useTLS": true,
  "verifyCertificate": true
}

Response:
{
  "success": true,
  "data": {
    "id": "conn-123",
    "name": "Production Database",
    "type": "postgresql",
    "host": "db.example.com",
    "port": 5432,
    "database": "production",
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

### List Connections
```
GET /connections
Authorization: Bearer <jwt_token>

Query Parameters:
- page: 1
- pageSize: 10
- sort: name
- order: asc

Response:
{
  "success": true,
  "data": [
    {
      "id": "conn-123",
      "name": "Production Database",
      "type": "postgresql",
      "host": "db.example.com",
      "port": 5432,
      "database": "production",
      "status": "connected",
      "lastTestedAt": "2024-01-01T12:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "total": 5
  }
}
```

### Get Connection
```
GET /connections/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "id": "conn-123",
    "name": "Production Database",
    "type": "postgresql",
    "host": "db.example.com",
    "port": 5432,
    "database": "production",
    "status": "connected",
    "createdAt": "2024-01-01T12:00:00Z",
    "lastTestedAt": "2024-01-01T12:00:00Z"
  }
}
```

### Test Connection
```
POST /connections/{id}/test
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "status": "connected",
    "message": "Connection successful",
    "responseTime": 125
  }
}
```

### Update Connection
```
PUT /connections/{id}
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Updated Database",
  "host": "new-host.example.com"
}

Response:
{
  "success": true,
  "data": {
    "id": "conn-123",
    "name": "Updated Database",
    "host": "new-host.example.com"
  }
}
```

### Delete Connection
```
DELETE /connections/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "message": "Connection deleted successfully"
}
```

## Schema Management Endpoints

### Extract Schema
```
POST /schemas/extract
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "connectionId": "conn-123"
}

Response:
{
  "success": true,
  "data": {
    "id": "schema-123",
    "databaseName": "production",
    "tables": [
      {
        "name": "users",
        "rowCount": 10000,
        "columns": [
          {
            "name": "id",
            "dataType": "INTEGER",
            "nullable": false,
            "isPrimaryKey": true
          },
          {
            "name": "email",
            "dataType": "VARCHAR(255)",
            "nullable": false
          }
        ],
        "primaryKeys": ["id"],
        "foreignKeys": []
      }
    ],
    "extractedAt": "2024-01-01T12:00:00Z"
  }
}
```

### Get Schema
```
GET /schemas/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "id": "schema-123",
    "databaseName": "production",
    "tables": [ /* ... */ ]
  }
}
```

### Compare Schemas
```
POST /schemas/compare
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "sourceSchemaId": "schema-123",
  "targetSchemaId": "schema-456"
}

Response:
{
  "success": true,
  "data": {
    "differences": [
      {
        "type": "table_missing",
        "table": "audit_logs",
        "source": "present",
        "target": "missing"
      }
    ],
    "compatible": false
  }
}
```

## PII Detection Endpoints

### Start PII Scan
```
POST /pii-scans
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "connectionId": "conn-123",
  "schemaId": "schema-123"
}

Response:
{
  "success": true,
  "data": {
    "id": "scan-123",
    "status": "IN_PROGRESS",
    "progress": 0,
    "startedAt": "2024-01-01T12:00:00Z"
  }
}
```

### Get PII Scan Status
```
GET /pii-scans/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "id": "scan-123",
    "status": "COMPLETED",
    "progress": 100,
    "startedAt": "2024-01-01T12:00:00Z",
    "completedAt": "2024-01-01T12:05:00Z"
  }
}
```

### Get PII Scan Results
```
GET /pii-scans/{id}/results
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "id": "scan-123",
    "classifications": [
      {
        "tableName": "users",
        "columnName": "email",
        "dataType": "VARCHAR(255)",
        "category": "EMAIL",
        "confidence": 95,
        "detectionMethod": "pattern",
        "evidence": ["user@example.com", "admin@example.com"]
      },
      {
        "tableName": "users",
        "columnName": "phone",
        "dataType": "VARCHAR(20)",
        "category": "PHONE",
        "confidence": 88,
        "detectionMethod": "pattern",
        "evidence": ["+1-555-0123", "+1-555-0124"]
      }
    ],
    "scanTime": "2024-01-01T12:05:00Z"
  }
}
```

### Override PII Classification
```
POST /pii-scans/{id}/override
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "tableName": "users",
  "columnName": "custom_field",
  "category": "IDENTIFIER",
  "confidence": 100
}

Response:
{
  "success": true,
  "message": "Classification overridden successfully"
}
```

## Configuration Management Endpoints

### Create Configuration
```
POST /configurations
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Production Config",
  "format": "yaml",
  "content": "version: \"1.0\"\ntables:\n  users:\n    columns:\n      email:\n        transformer: fake_email"
}

Response:
{
  "success": true,
  "data": {
    "id": "config-123",
    "name": "Production Config",
    "version": 1,
    "format": "yaml",
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

### Get Configuration
```
GET /configurations/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "id": "config-123",
    "name": "Production Config",
    "version": 1,
    "format": "yaml",
    "content": "version: \"1.0\"\n..."
  }
}
```

### Update Configuration
```
PUT /configurations/{id}
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "content": "version: \"1.0\"\n..."
}

Response:
{
  "success": true,
  "data": {
    "id": "config-123",
    "version": 2,
    "updatedAt": "2024-01-01T12:05:00Z"
  }
}
```

### Get Configuration Versions
```
GET /configurations/{id}/versions
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": [
    {
      "version": 2,
      "createdAt": "2024-01-01T12:05:00Z",
      "changes": "Updated email transformer"
    },
    {
      "version": 1,
      "createdAt": "2024-01-01T12:00:00Z",
      "changes": "Initial configuration"
    }
  ]
}
```

### Restore Configuration Version
```
POST /configurations/{id}/restore
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "version": 1
}

Response:
{
  "success": true,
  "data": {
    "id": "config-123",
    "version": 3,
    "restoredFrom": 1
  }
}
```

## Anonymization Endpoints

### Start Anonymization
```
POST /anonymizations
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "sourceConnectionId": "conn-123",
  "targetConnectionId": "conn-456",
  "configurationId": "config-123"
}

Response:
{
  "success": true,
  "data": {
    "jobId": "job-123",
    "status": "STARTED",
    "sourceConnectionId": "conn-123",
    "targetConnectionId": "conn-456",
    "startedAt": "2024-01-01T12:00:00Z"
  }
}
```

### Get Anonymization Status
```
GET /anonymizations/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "jobId": "job-123",
    "status": "IN_PROGRESS",
    "progress": 45,
    "rowsProcessed": 4500,
    "errorMessage": null
  }
}
```

### Get Anonymization Progress
```
GET /anonymizations/{id}/progress
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "jobId": "job-123",
    "progress": 45,
    "rowsProcessed": 4500,
    "totalRows": 10000,
    "currentTable": "users",
    "estimatedTimeRemaining": 300
  }
}
```

### Cancel Anonymization
```
POST /anonymizations/{id}/cancel
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "message": "Anonymization cancelled successfully"
}
```

### Get Anonymization Results
```
GET /anonymizations/{id}/results
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "jobId": "job-123",
    "status": "COMPLETED",
    "rowsProcessed": 10000,
    "duration": 600000,
    "tableStats": [
      {
        "tableName": "users",
        "rowsProcessed": 10000,
        "status": "COMPLETED"
      }
    ]
  }
}
```

## Export Endpoints

### Start Export
```
POST /exports
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "jobId": "job-123",
  "format": "sql_dump",
  "databaseType": "postgresql"
}

Response:
{
  "success": true,
  "data": {
    "exportId": "export-123",
    "status": "IN_PROGRESS",
    "format": "sql_dump"
  }
}
```

### Get Export Status
```
GET /exports/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "exportId": "export-123",
    "status": "COMPLETED",
    "format": "sql_dump",
    "fileSize": 1024000,
    "downloadUrl": "/exports/export-123/download"
  }
}
```

### Download Export
```
GET /exports/{id}/download
Authorization: Bearer <jwt_token>

Response: Binary file download
```

## Audit Log Endpoints

### List Audit Logs
```
GET /audit-logs
Authorization: Bearer <jwt_token>

Query Parameters:
- action: START_ANONYMIZATION
- userId: user-123
- fromDate: 2024-01-01
- toDate: 2024-01-31
- page: 1
- pageSize: 10

Response:
{
  "success": true,
  "data": [
    {
      "id": "log-123",
      "timestamp": "2024-01-01T12:00:00Z",
      "userId": "user-123",
      "action": "START_ANONYMIZATION",
      "resource": "job-123",
      "result": "SUCCESS",
      "metadata": {
        "sourceDb": "production",
        "targetDb": "staging"
      }
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 10,
    "total": 150
  }
}
```

### Get Audit Log
```
GET /audit-logs/{id}
Authorization: Bearer <jwt_token>

Response:
{
  "success": true,
  "data": {
    "id": "log-123",
    "timestamp": "2024-01-01T12:00:00Z",
    "userId": "user-123",
    "action": "START_ANONYMIZATION",
    "resource": "job-123",
    "result": "SUCCESS",
    "metadata": { /* ... */ },
    "ipAddress": "192.168.1.1"
  }
}
```

### Export Audit Logs
```
POST /audit-logs/export
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "format": "csv",
  "fromDate": "2024-01-01",
  "toDate": "2024-01-31"
}

Response: CSV file download
```

## Error Responses

### 400 Bad Request
```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Invalid request parameters",
    "details": {
      "field": "email",
      "issue": "Invalid email format"
    }
  }
}
```

### 401 Unauthorized
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required"
  }
}
```

### 403 Forbidden
```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "Access denied"
  }
}
```

### 404 Not Found
```json
{
  "success": false,
  "error": {
    "code": "NOT_FOUND",
    "message": "Resource not found"
  }
}
```

### 500 Internal Server Error
```json
{
  "success": false,
  "error": {
    "code": "INTERNAL_ERROR",
    "message": "An unexpected error occurred"
  }
}
```

## Rate Limiting

- **Limit**: 1000 requests per hour per user
- **Headers**: 
  - `X-RateLimit-Limit`: 1000
  - `X-RateLimit-Remaining`: 999
  - `X-RateLimit-Reset`: 1704110400

## Pagination

All list endpoints support pagination:
- `page`: Page number (default: 1)
- `pageSize`: Items per page (default: 10, max: 100)
- `sort`: Sort field (default: createdAt)
- `order`: Sort order (asc/desc, default: desc)

## Webhooks

Webhooks can be configured to receive notifications for:
- Anonymization job completion
- PII scan completion
- Export completion
- Error events

### Register Webhook
```
POST /webhooks
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "url": "https://example.com/webhook",
  "events": ["anonymization.completed", "pii_scan.completed"],
  "active": true
}
```

---

**Last Updated**: Current date
**Version**: 1.0
**Status**: Production Ready

