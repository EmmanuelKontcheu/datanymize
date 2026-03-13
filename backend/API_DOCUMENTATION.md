# Datanymize REST API Documentation

## Overview

The Datanymize REST API provides comprehensive endpoints for managing database connections, extracting schemas, detecting PII, configuring anonymization rules, executing anonymization jobs, and exporting data.

**Base URL**: `http://localhost:8080/api`

**API Version**: 1.0.0

**Authentication**: JWT Bearer Token

## Authentication

All endpoints (except `/api/auth/login`) require JWT Bearer token authentication.

### Login

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password"
}

Response:
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "userId": "user-123",
    "username": "user@example.com",
    "roles": ["USER", "ADMIN"]
  }
}
```

### Using Token

Include the token in the Authorization header:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## API Endpoints

### Connection Management

#### Create Connection
```
POST /api/connections
Content-Type: application/json

{
  "host": "localhost",
  "port": 5432,
  "database": "production",
  "username": "user",
  "password": "password",
  "databaseType": "postgresql",
  "useTLS": true,
  "verifyCertificate": true
}

Response: 201 Created
{
  "success": true,
  "data": {
    "id": "conn-123",
    "host": "localhost",
    "port": 5432,
    "database": "production",
    "databaseType": "postgresql",
    "status": "UNTESTED"
  }
}
```

#### List Connections
```
GET /api/connections

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "id": "conn-123",
      "host": "localhost",
      "port": 5432,
      "database": "production",
      "databaseType": "postgresql",
      "status": "CONNECTED"
    }
  ]
}
```

#### Get Connection Details
```
GET /api/connections/{id}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "conn-123",
    "host": "localhost",
    "port": 5432,
    "database": "production",
    "databaseType": "postgresql",
    "status": "CONNECTED"
  }
}
```

#### Test Connection
```
POST /api/connections/{id}/test

Response: 200 OK
{
  "success": true,
  "data": {
    "success": true,
    "message": "Connection successful"
  }
}
```

#### Delete Connection
```
DELETE /api/connections/{id}

Response: 200 OK
{
  "success": true,
  "message": "Connection deleted successfully"
}
```

### Schema Management

#### Extract Schema
```
POST /api/schemas/extract
Content-Type: application/json

{
  "connectionId": "conn-123"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "schema-123",
    "databaseName": "production",
    "tables": [
      {
        "name": "users",
        "columns": [
          {
            "name": "id",
            "dataType": "INTEGER",
            "nullable": false,
            "isPrimaryKey": true
          }
        ]
      }
    ]
  }
}
```

#### Synchronize Schema
```
POST /api/schemas/sync
Content-Type: application/json

{
  "sourceConnectionId": "conn-123",
  "targetConnectionId": "conn-456"
}

Response: 200 OK
{
  "success": true,
  "message": "Schema synchronized successfully"
}
```

#### Get Schema Details
```
GET /api/schemas/{id}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "schema-123",
    "databaseName": "production",
    "tables": [...]
  }
}
```

#### Compare Schemas
```
POST /api/schemas/compare
Content-Type: application/json

{
  "sourceSchemaId": "schema-123",
  "targetSchemaId": "schema-456"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "differences": [
      {
        "type": "MISSING_TABLE",
        "table": "users"
      }
    ]
  }
}
```

### PII Detection

#### Start PII Scan
```
POST /api/pii-scans
Content-Type: application/json

{
  "connectionId": "conn-123"
}

Response: 201 Created
{
  "success": true,
  "data": {
    "id": "scan-123",
    "status": "RUNNING",
    "progress": 0
  }
}
```

#### Get Scan Status
```
GET /api/pii-scans/{id}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "scan-123",
    "status": "COMPLETED",
    "progress": 100
  }
}
```

#### Get Scan Results
```
GET /api/pii-scans/{id}/results

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "scan-123",
    "classifications": [
      {
        "tableName": "users",
        "columnName": "email",
        "category": "EMAIL",
        "confidence": 95,
        "detectionMethod": "pattern"
      }
    ]
  }
}
```

#### Override PII Classification
```
POST /api/pii-scans/{id}/override
Content-Type: application/json

{
  "tableName": "users",
  "columnName": "email",
  "newCategory": "IDENTIFIER"
}

Response: 200 OK
{
  "success": true,
  "message": "Classification overridden successfully"
}
```

### Configuration Management

#### Create Configuration
```
POST /api/configurations
Content-Type: application/json

{
  "content": "version: '1.0'\ntables:\n  users:\n    columns:\n      email:\n        transformer: fake_email",
  "format": "yaml"
}

Response: 201 Created
{
  "success": true,
  "data": {
    "id": "config-123",
    "version": 1,
    "format": "yaml",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

#### Get Configuration
```
GET /api/configurations/{id}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "config-123",
    "version": 1,
    "content": "version: '1.0'\n...",
    "createdAt": "2024-01-15T10:30:00"
  }
}
```

#### Update Configuration
```
PUT /api/configurations/{id}
Content-Type: application/json

{
  "content": "version: '1.0'\ntables:\n  users:\n    columns:\n      email:\n        transformer: fake_email",
  "format": "yaml"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "config-123",
    "version": 2,
    "format": "yaml",
    "createdAt": "2024-01-15T10:35:00"
  }
}
```

#### Get Version History
```
GET /api/configurations/{id}/versions

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "versionNumber": 1,
      "timestamp": "2024-01-15T10:30:00",
      "changes": "Initial configuration"
    },
    {
      "versionNumber": 2,
      "timestamp": "2024-01-15T10:35:00",
      "changes": "Updated email transformer"
    }
  ]
}
```

#### Restore Version
```
POST /api/configurations/{id}/restore
Content-Type: application/json

{
  "versionNumber": 1
}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "config-123",
    "version": 3,
    "format": "yaml",
    "createdAt": "2024-01-15T10:40:00"
  }
}
```

### Anonymization

#### Start Anonymization
```
POST /api/anonymizations
Content-Type: application/json

{
  "sourceConnectionId": "conn-123",
  "targetConnectionId": "conn-456",
  "configurationId": "config-123"
}

Response: 201 Created
{
  "success": true,
  "data": {
    "id": "anon-123",
    "status": "RUNNING",
    "progress": 0
  }
}
```

#### Get Anonymization Status
```
GET /api/anonymizations/{id}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "anon-123",
    "status": "RUNNING",
    "progress": 50
  }
}
```

#### Get Real-Time Progress
```
GET /api/anonymizations/{id}/progress

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "anon-123",
    "status": "RUNNING",
    "progress": 50,
    "rowsProcessed": 50000,
    "totalRows": 100000,
    "estimatedTimeRemaining": 300
  }
}
```

#### Cancel Anonymization
```
POST /api/anonymizations/{id}/cancel

Response: 200 OK
{
  "success": true,
  "message": "Anonymization cancelled successfully"
}
```

#### Get Results
```
GET /api/anonymizations/{id}/results

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "anon-123",
    "status": "COMPLETED",
    "rowsProcessed": 100000,
    "duration": 600,
    "success": true
  }
}
```

### Export

#### Start Export
```
POST /api/exports
Content-Type: application/json

{
  "sourceConnectionId": "conn-456",
  "format": "POSTGRESQL_DUMP"
}

Response: 201 Created
{
  "success": true,
  "data": {
    "id": "export-123",
    "status": "RUNNING",
    "format": "POSTGRESQL_DUMP",
    "progress": 0
  }
}
```

#### Get Export Status
```
GET /api/exports/{id}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "export-123",
    "status": "COMPLETED",
    "format": "POSTGRESQL_DUMP",
    "progress": 100
  }
}
```

#### Download Export
```
GET /api/exports/{id}/download

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "export-123",
    "downloadUrl": "/api/exports/export-123/file",
    "format": "POSTGRESQL_DUMP",
    "size": 1048576
  }
}
```

#### Get Export Progress
```
GET /api/exports/{id}/progress

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "export-123",
    "status": "RUNNING",
    "progress": 75,
    "rowsProcessed": 75000,
    "totalRows": 100000,
    "estimatedTimeRemaining": 60
  }
}
```

### Audit Logs

#### List Audit Logs
```
GET /api/audit-logs?action=ANONYMIZATION_STARTED&userId=user-123&page=0&pageSize=50

Response: 200 OK
{
  "success": true,
  "data": [
    {
      "id": "log-1",
      "timestamp": "2024-01-15T10:30:00",
      "userId": "user-123",
      "action": "ANONYMIZATION_STARTED",
      "resource": "database-1",
      "result": "SUCCESS",
      "rowsProcessed": 100000
    }
  ]
}
```

#### Get Audit Log Details
```
GET /api/audit-logs/{id}

Response: 200 OK
{
  "success": true,
  "data": {
    "id": "log-1",
    "timestamp": "2024-01-15T10:30:00",
    "tenantId": "tenant-123",
    "userId": "user-123",
    "action": "ANONYMIZATION_STARTED",
    "resource": "database-1",
    "result": "SUCCESS",
    "rowsProcessed": 100000,
    "duration": 600,
    "metadata": {}
  }
}
```

#### Export Audit Logs
```
POST /api/audit-logs/export
Content-Type: application/json

{
  "format": "CSV",
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-01-31T23:59:59"
}

Response: 200 OK
{
  "success": true,
  "data": {
    "exportId": "export-123",
    "format": "CSV",
    "status": "COMPLETED",
    "downloadUrl": "/api/audit-logs/export/export-123/download"
  }
}
```

### Authentication

#### Logout
```
POST /api/auth/logout
Authorization: Bearer <token>

Response: 200 OK
{
  "success": true,
  "message": "Logout successful"
}
```

#### Refresh Token
```
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

Response: 200 OK
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

#### Get Current User
```
GET /api/auth/me
Authorization: Bearer <token>

Response: 200 OK
{
  "success": true,
  "data": {
    "userId": "user-123",
    "username": "user@example.com",
    "email": "user@example.com",
    "roles": ["USER", "ADMIN"],
    "tenantId": "tenant-123"
  }
}
```

## Error Responses

All error responses follow this format:

```json
{
  "success": false,
  "error": {
    "message": "Error description",
    "code": "ERROR_CODE",
    "status": 400
  }
}
```

### Common Error Codes

- `CONNECTION_NOT_FOUND` (404): Connection not found
- `CONFIG_NOT_FOUND` (404): Configuration not found
- `EXPORT_NOT_FOUND` (404): Export job not found
- `AUTH_FAILED` (401): Authentication failed
- `ACCESS_DENIED` (403): Access denied
- `VALIDATION_FAILED` (400): Request validation failed
- `INTERNAL_ERROR` (500): Internal server error

## Rate Limiting

API endpoints are rate-limited to 1000 requests per hour per user.

## Pagination

List endpoints support pagination with `page` and `pageSize` parameters:

```
GET /api/audit-logs?page=0&pageSize=50
```

## Swagger/OpenAPI Documentation

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui.html
```

OpenAPI specification is available at:

```
http://localhost:8080/v3/api-docs
```

## Examples

### Complete Anonymization Workflow

1. **Create connection to source database**
   ```
   POST /api/connections
   ```

2. **Create connection to target database**
   ```
   POST /api/connections
   ```

3. **Extract schema from source**
   ```
   POST /api/schemas/extract
   ```

4. **Synchronize schema to target**
   ```
   POST /api/schemas/sync
   ```

5. **Run PII scan**
   ```
   POST /api/pii-scans
   ```

6. **Review and override PII classifications**
   ```
   POST /api/pii-scans/{id}/override
   ```

7. **Create anonymization configuration**
   ```
   POST /api/configurations
   ```

8. **Start anonymization**
   ```
   POST /api/anonymizations
   ```

9. **Monitor progress**
   ```
   GET /api/anonymizations/{id}/progress
   ```

10. **Export anonymized data**
    ```
    POST /api/exports
    ```

## Support

For issues or questions, contact support@datanymize.com
