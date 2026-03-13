# Datanymize Deployment Guide

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Local Development Setup](#local-development-setup)
4. [Docker Deployment](#docker-deployment)
5. [Kubernetes Deployment](#kubernetes-deployment)
6. [Environment Configuration](#environment-configuration)
7. [Database Setup](#database-setup)
8. [Security Configuration](#security-configuration)
9. [Monitoring and Logging](#monitoring-and-logging)
10. [Backup and Recovery](#backup-and-recovery)
11. [Troubleshooting](#troubleshooting)

## Overview

Datanymize is a multi-database anonymization SaaS platform. This guide covers deployment options from local development to production Kubernetes clusters.

### Supported Deployment Environments
- Local development (Docker Compose)
- Docker containers
- Kubernetes clusters
- Cloud platforms (AWS, Azure, GCP)

### System Requirements
- **CPU**: 2+ cores
- **Memory**: 4GB+ RAM
- **Storage**: 20GB+ disk space
- **Network**: Internet connectivity for AI providers

## Prerequisites

### Required Software
- Docker 20.10+
- Docker Compose 2.0+
- Kubernetes 1.24+ (for K8s deployment)
- kubectl 1.24+ (for K8s deployment)
- Git 2.30+

### Optional Software
- Helm 3.0+ (for K8s deployment)
- PostgreSQL 14+ client tools
- MySQL 8.0+ client tools
- MongoDB 5.0+ client tools

### API Keys Required
- OpenAI API key (for AI-based PII detection)
- Anthropic API key (optional, for Claude-based detection)

## Local Development Setup

### 1. Clone Repository
```bash
git clone https://github.com/datanymize/datanymize.git
cd datanymize
```

### 2. Build Backend
```bash
cd backend
mvn clean package -DskipTests
cd ..
```

### 3. Build Frontend
```bash
cd frontend
npm install
npm run build
cd ..
```

### 4. Start with Docker Compose
```bash
docker-compose up -d
```

### 5. Access Application
- **Frontend**: http://localhost:4200
- **Backend API**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html

### 6. Default Credentials
- **Username**: admin@datanymize.com
- **Password**: admin123

## Docker Deployment

### 1. Build Docker Images

#### Backend Image
```bash
cd backend
docker build -t datanymize-backend:latest .
cd ..
```

#### Frontend Image
```bash
cd frontend
docker build -t datanymize-frontend:latest .
cd ..
```

### 2. Run Containers

#### Backend Container
```bash
docker run -d \
  --name datanymize-backend \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/datanymize \
  -e SPRING_DATASOURCE_USERNAME=datanymize \
  -e SPRING_DATASOURCE_PASSWORD=secure_password \
  -e OPENAI_API_KEY=your_api_key \
  datanymize-backend:latest
```

#### Frontend Container
```bash
docker run -d \
  --name datanymize-frontend \
  -p 4200:80 \
  -e API_URL=http://localhost:8080/api \
  datanymize-frontend:latest
```

### 3. Docker Compose Production Setup

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: datanymize
      POSTGRES_USER: datanymize
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    image: datanymize-backend:latest
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/datanymize
      SPRING_DATASOURCE_USERNAME: datanymize
      SPRING_DATASOURCE_PASSWORD: secure_password
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      SPRING_JPA_HIBERNATE_DDL_AUTO: validate
    ports:
      - "8080:8080"
    depends_on:
      - postgres

  frontend:
    image: datanymize-frontend:latest
    environment:
      API_URL: http://backend:8080/api
    ports:
      - "4200:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

## Kubernetes Deployment

### 1. Create Namespace
```bash
kubectl create namespace datanymize
```

### 2. Create Secrets
```bash
kubectl create secret generic datanymize-secrets \
  --from-literal=db-password=secure_password \
  --from-literal=openai-api-key=your_api_key \
  -n datanymize
```

### 3. Create ConfigMap
```bash
kubectl create configmap datanymize-config \
  --from-literal=spring.jpa.hibernate.ddl-auto=validate \
  --from-literal=api.url=http://backend:8080/api \
  -n datanymize
```

### 4. Deploy PostgreSQL
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: datanymize
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: datanymize
        - name: POSTGRES_USER
          value: datanymize
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: datanymize-secrets
              key: db-password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
```

### 5. Deploy Backend
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: datanymize
spec:
  replicas: 3
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
      - name: backend
        image: datanymize-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres:5432/datanymize
        - name: SPRING_DATASOURCE_USERNAME
          value: datanymize
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: datanymize-secrets
              key: db-password
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: datanymize-secrets
              key: openai-api-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
```

### 6. Deploy Frontend
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
  namespace: datanymize
spec:
  replicas: 2
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: datanymize-frontend:latest
        ports:
        - containerPort: 80
        env:
        - name: API_URL
          value: http://backend:8080/api
        resources:
          requests:
            memory: "256Mi"
            cpu: "100m"
          limits:
            memory: "512Mi"
            cpu: "250m"
```

### 7. Create Services
```yaml
apiVersion: v1
kind: Service
metadata:
  name: backend
  namespace: datanymize
spec:
  selector:
    app: backend
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: ClusterIP

---
apiVersion: v1
kind: Service
metadata:
  name: frontend
  namespace: datanymize
spec:
  selector:
    app: frontend
  ports:
  - protocol: TCP
    port: 80
    targetPort: 80
  type: LoadBalancer
```

### 8. Create Ingress
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: datanymize-ingress
  namespace: datanymize
spec:
  ingressClassName: nginx
  rules:
  - host: datanymize.example.com
    http:
      paths:
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: backend
            port:
              number: 8080
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
  tls:
  - hosts:
    - datanymize.example.com
    secretName: datanymize-tls
```

## Environment Configuration

### Backend Configuration (application.yml)

```yaml
spring:
  application:
    name: datanymize
  datasource:
    url: jdbc:postgresql://localhost:5432/datanymize
    username: datanymize
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        jdbc:
          batch_size: 20
          fetch_size: 50

server:
  port: 8080
  servlet:
    context-path: /api

datanymize:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 86400000
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      model: claude-3-opus
  encryption:
    key: ${ENCRYPTION_KEY}
    algorithm: AES
```

### Frontend Configuration (environment.ts)

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  apiTimeout: 30000,
  pollInterval: 2000,
  pageSize: 10
};
```

## Database Setup

### PostgreSQL Setup

```bash
# Create database
createdb -U postgres datanymize

# Create user
createuser -U postgres -P datanymize

# Grant privileges
psql -U postgres -d datanymize -c "GRANT ALL PRIVILEGES ON DATABASE datanymize TO datanymize;"

# Run migrations
psql -U datanymize -d datanymize -f schema.sql
```

### MySQL Setup

```bash
# Create database
mysql -u root -p -e "CREATE DATABASE datanymize;"

# Create user
mysql -u root -p -e "CREATE USER 'datanymize'@'localhost' IDENTIFIED BY 'password';"

# Grant privileges
mysql -u root -p -e "GRANT ALL PRIVILEGES ON datanymize.* TO 'datanymize'@'localhost';"

# Run migrations
mysql -u datanymize -p datanymize < schema.sql
```

### MongoDB Setup

```bash
# Create database
mongo admin --eval "db.createUser({user: 'datanymize', pwd: 'password', roles: ['root']})"

# Create collections
mongo datanymize --eval "db.createCollection('users')"
```

## Security Configuration

### TLS/SSL Setup

```bash
# Generate self-signed certificate
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 365

# Create PKCS12 keystore
openssl pkcs12 -export -in cert.pem -inkey key.pem -out keystore.p12 -name datanymize
```

### Spring Boot TLS Configuration

```yaml
server:
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: datanymize
```

### Firewall Rules

```bash
# Allow HTTP
sudo ufw allow 80/tcp

# Allow HTTPS
sudo ufw allow 443/tcp

# Allow API
sudo ufw allow 8080/tcp

# Allow PostgreSQL (internal only)
sudo ufw allow from 10.0.0.0/8 to any port 5432
```

## Monitoring and Logging

### Prometheus Metrics

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### ELK Stack Integration

```yaml
logging:
  level:
    root: INFO
    com.datanymize: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/datanymize.log
    max-size: 10MB
    max-history: 30
```

## Backup and Recovery

### Database Backup

```bash
# PostgreSQL backup
pg_dump -U datanymize datanymize > backup.sql

# MySQL backup
mysqldump -u datanymize -p datanymize > backup.sql

# MongoDB backup
mongodump --uri="mongodb://datanymize:password@localhost:27017/datanymize"
```

### Restore from Backup

```bash
# PostgreSQL restore
psql -U datanymize datanymize < backup.sql

# MySQL restore
mysql -u datanymize -p datanymize < backup.sql

# MongoDB restore
mongorestore --uri="mongodb://datanymize:password@localhost:27017" dump/
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed
```bash
# Check database is running
docker ps | grep postgres

# Check connection string
echo $SPRING_DATASOURCE_URL

# Test connection
psql -h localhost -U datanymize -d datanymize -c "SELECT 1"
```

#### 2. API Not Responding
```bash
# Check backend logs
docker logs datanymize-backend

# Check port is open
netstat -tlnp | grep 8080

# Restart backend
docker restart datanymize-backend
```

#### 3. Frontend Not Loading
```bash
# Check frontend logs
docker logs datanymize-frontend

# Check API URL configuration
curl http://localhost:8080/api/health

# Clear browser cache
# Ctrl+Shift+Delete (Chrome/Firefox)
```

#### 4. High Memory Usage
```bash
# Check memory usage
docker stats datanymize-backend

# Increase JVM heap
export JAVA_OPTS="-Xmx2g -Xms1g"

# Restart backend
docker restart datanymize-backend
```

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8080/actuator/health/db

# API readiness
curl http://localhost:8080/actuator/health/readiness
```

## Performance Tuning

### Database Optimization

```sql
-- Create indices
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_anonymization_jobs_status ON anonymization_jobs(status);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);

-- Analyze tables
ANALYZE users;
ANALYZE anonymization_jobs;
ANALYZE audit_logs;
```

### Connection Pool Tuning

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### JVM Tuning

```bash
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## Maintenance

### Regular Tasks

- **Daily**: Monitor logs and metrics
- **Weekly**: Review performance metrics
- **Monthly**: Update dependencies
- **Quarterly**: Security audit
- **Annually**: Capacity planning

### Backup Schedule

- **Hourly**: Incremental backups
- **Daily**: Full backups
- **Weekly**: Off-site backups
- **Monthly**: Archive backups

---

**Last Updated**: Current date
**Version**: 1.0
**Status**: Production Ready

