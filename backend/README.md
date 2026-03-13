# Datanymize Backend

Spring Boot 3.x REST API for the Datanymize multi-database anonymization SaaS platform.

## Technology Stack

- **Java**: 21
- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven
- **Databases**: PostgreSQL, MySQL, MongoDB
- **Testing**: JUnit 5, jqwik (Property-Based Testing)

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/datanymize/
│   │   │   ├── controller/          # REST API controllers
│   │   │   ├── service/             # Business logic services
│   │   │   ├── repository/          # Data access layer
│   │   │   ├── model/               # Domain models
│   │   │   ├── config/              # Spring configuration
│   │   │   └── DatanymizeApplication.java
│   │   └── resources/
│   │       ├── application.yml      # Application configuration
│   │       └── application-{profile}.yml
│   └── test/
│       ├── java/com/datanymize/
│       │   ├── unit/                # Unit tests
│       │   ├── integration/         # Integration tests
│       │   └── properties/          # Property-based tests
│       └── resources/
├── pom.xml                          # Maven configuration
├── mvnw.cmd                         # Maven wrapper (Windows)
├── mvnw                             # Maven wrapper (Linux/Mac)
└── .mvn/                            # Maven wrapper configuration
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8.0 or higher
- PostgreSQL 12+ (for dev profile)
- MySQL 8.0+ (optional)
- MongoDB 5.0+ (optional)

### Build

```bash
mvn clean install
```

### Run

**Development Profile** (with PostgreSQL):
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

**Test Profile** (with H2 in-memory database):
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"
```

**Production Profile**:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

### Test

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=DatanymizeApplicationTests
```

Run property-based tests:
```bash
mvn test -Dtest=*Properties
```

## Configuration Profiles

### Development (dev)
- PostgreSQL database
- DDL auto: create-drop
- SQL logging enabled
- Debug logging level

### Test (test)
- H2 in-memory database
- DDL auto: create-drop
- SQL logging disabled
- Warning logging level

### Production (prod)
- PostgreSQL database (configurable)
- DDL auto: validate
- SSL/TLS enabled
- Warning logging level
- File-based logging with rotation

## API Endpoints

### Health Check
- `GET /api/health` - Service health status

## Development Guidelines

### Code Style
- Follow Google Java Style Guide
- Use Lombok for boilerplate reduction
- Add meaningful JavaDoc for public APIs

### Testing
- Write unit tests for all business logic
- Write property-based tests for core algorithms
- Aim for >80% code coverage

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
