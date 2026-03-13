# Datanymize

Multi-database anonymization SaaS platform that enables developers to safely synchronize realistic test data from production databases into local development and test environments while maintaining GDPR compliance.

## Project Structure

```
datanymize/
├── backend/                 # Spring Boot 3.x REST API
│   ├── src/
│   ├── pom.xml
│   ├── mvnw.cmd
│   ├── .mvn/
│   └── README.md
├── frontend/                # Angular 17+ Web UI
│   ├── src/
│   ├── package.json
│   ├── angular.json
│   └── README.md
└── README.md               # This file
```

## Technology Stack

### Backend
- **Java**: 21
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven
- **Databases**: PostgreSQL, MySQL, MongoDB
- **Testing**: JUnit 5, jqwik (Property-Based Testing)

### Frontend
- **Framework**: Angular 17+
- **Styling**: Tailwind CSS
- **Language**: TypeScript
- **Build Tool**: npm/yarn

## Getting Started

### Backend Setup

```bash
cd backend
mvn clean install
```

**Run Development Profile** (with PostgreSQL):
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Run Test Profile** (with H2 in-memory database):
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"
```

See `backend/README.md` for detailed backend documentation.

### Frontend Setup

```bash
cd frontend
npm install
ng serve
```

Frontend will be available at `http://localhost:4200`

See `frontend/README.md` for detailed frontend documentation.

## API Documentation

The backend API is available at `http://localhost:8080/api` with Swagger documentation at `/api/swagger-ui.html`

## Development Guidelines

### Code Style
- Backend: Follow Google Java Style Guide
- Frontend: Follow Angular style guide

### Testing
- Backend: Unit tests + property-based tests (jqwik)
- Frontend: Unit tests + E2E tests

### Commit Messages
- Use conventional commits format
- Example: `feat: add connection manager interface`

## Security

- All database connections use TLS/SSL
- Credentials encrypted at rest (AES-256)
- Read-only access to source databases
- Comprehensive audit logging
- Multi-tenant data isolation

## License

Proprietary - Datanymize SaaS Platform
