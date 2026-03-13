# Datanymize Frontend

Angular 17+ web UI for the Datanymize multi-database anonymization SaaS platform.

## Technology Stack

- **Framework**: Angular 17+
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Build Tool**: npm/yarn
- **Testing**: Jasmine, Karma, Cypress (E2E)

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/
│   │   │   ├── services/
│   │   │   │   ├── api.service.ts
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── connection.service.ts
│   │   │   │   └── tenant.service.ts
│   │   │   ├── guards/
│   │   │   │   ├── auth.guard.ts
│   │   │   │   └── tenant.guard.ts
│   │   │   └── interceptors/
│   │   │       ├── auth.interceptor.ts
│   │   │       └── error.interceptor.ts
│   │   │
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   │   ├── progress-bar/
│   │   │   │   ├── error-alert/
│   │   │   │   └── confirmation-dialog/
│   │   │   └── pipes/
│   │   │       ├── safe-html.pipe.ts
│   │   │       └── format-bytes.pipe.ts
│   │   │
│   │   ├── features/
│   │   │   ├── dashboard/
│   │   │   ├── connections/
│   │   │   ├── pii-scan/
│   │   │   ├── configuration/
│   │   │   ├── anonymization/
│   │   │   ├── job-history/
│   │   │   └── audit-logs/
│   │   │
│   │   └── app.component.ts
│   ├── assets/
│   ├── styles/
│   │   └── tailwind.css
│   ├── index.html
│   ├── main.ts
│   └── styles.css
├── angular.json
├── package.json
├── tsconfig.json
├── tailwind.config.js
└── README.md
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm 9+
- Angular CLI 17+

### Installation

```bash
npm install
```

### Development Server

```bash
ng serve
```

Navigate to `http://localhost:4200/`. The application will automatically reload if you change any of the source files.

### Build

```bash
ng build
```

The build artifacts will be stored in the `dist/` directory.

### Running Tests

**Unit Tests:**
```bash
ng test
```

**E2E Tests:**
```bash
ng e2e
```

## Configuration

### API Base URL

Update the API base URL in `src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### Tailwind CSS

Tailwind CSS is configured in `tailwind.config.js`. Customize theme colors, spacing, and other utilities as needed.

## Features

### Dashboard
- Connection overview with status indicators
- Quick action buttons
- Recent jobs summary
- System health status

### Connection Management
- Create, edit, delete database connections
- Test connections with real-time feedback
- TLS/SSL configuration

### PII Scan Visualization
- Table with column classifications
- Color-coded confidence levels
- Sample data viewer
- Classification override

### Configuration Editor
- Visual transformer selector
- Parameter configuration
- YAML/JSON preview
- Version history

### Progress Monitor
- Real-time progress tracking
- Estimated time remaining
- Current operation details
- Cancel button

### Job History
- View all anonymization jobs
- Detailed job information
- Retry functionality
- Export logs

## Development Guidelines

### Code Style
- Follow Angular style guide
- Use TypeScript strict mode
- Add meaningful comments for complex logic

### Component Structure
- One component per file
- Use smart/dumb component pattern
- Implement OnDestroy for cleanup

### Services
- Use dependency injection
- Implement error handling
- Use RxJS observables

### Testing
- Write unit tests for all services
- Write component tests for UI logic
- Aim for >80% code coverage

### Commit Messages
- Use conventional commits format
- Example: `feat: add connection form component`

## Security

- JWT token-based authentication
- HTTPS for all API calls
- XSS protection with Angular sanitization
- CSRF protection with token validation
- Secure credential storage (no localStorage for sensitive data)

## Performance

- Lazy loading for feature modules
- OnPush change detection strategy
- Tree-shaking for unused code
- Production build optimization

## License

Proprietary - Datanymize SaaS Platform
