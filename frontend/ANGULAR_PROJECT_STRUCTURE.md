# Datanymize Angular Web UI - Project Structure

## Overview

This document describes the Angular 17+ project structure for the Datanymize Web UI.

## Directory Structure

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
│   │   │   │   └── auth.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── auth.interceptor.ts
│   │   │   │   └── error.interceptor.ts
│   │   │   └── models/
│   │   │       ├── connection.model.ts
│   │   │       ├── pii-scan.model.ts
│   │   │       ├── configuration.model.ts
│   │   │       └── anonymization.model.ts
│   │   │
│   │   ├── shared/
│   │   │   ├── components/
│   │   │   │   ├── progress-bar/
│   │   │   │   ├── error-alert/
│   │   │   │   ├── confirmation-dialog/
│   │   │   │   └── loading-spinner/
│   │   │   ├── pipes/
│   │   │   │   ├── safe-html.pipe.ts
│   │   │   │   └── format-bytes.pipe.ts
│   │   │   └── directives/
│   │   │       └── highlight.directive.ts
│   │   │
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   │   ├── auth.routes.ts
│   │   │   │   ├── login/
│   │   │   │   │   └── login.component.ts
│   │   │   │   └── logout/
│   │   │   │       └── logout.component.ts
│   │   │   │
│   │   │   ├── dashboard/
│   │   │   │   ├── dashboard.routes.ts
│   │   │   │   └── dashboard.component.ts
│   │   │   │
│   │   │   ├── connections/
│   │   │   │   ├── connections.routes.ts
│   │   │   │   ├── connection-list/
│   │   │   │   │   └── connection-list.component.ts
│   │   │   │   ├── connection-form/
│   │   │   │   │   └── connection-form.component.ts
│   │   │   │   ├── connection-test/
│   │   │   │   │   └── connection-test.component.ts
│   │   │   │   └── connection-detail/
│   │   │   │       └── connection-detail.component.ts
│   │   │   │
│   │   │   ├── pii-scan/
│   │   │   │   ├── pii-scan.routes.ts
│   │   │   │   ├── scan-initiation/
│   │   │   │   │   └── scan-initiation.component.ts
│   │   │   │   ├── scan-results/
│   │   │   │   │   └── scan-results.component.ts
│   │   │   │   ├── classification-table/
│   │   │   │   │   └── classification-table.component.ts
│   │   │   │   └── sample-viewer/
│   │   │   │       └── sample-viewer.component.ts
│   │   │   │
│   │   │   ├── configuration/
│   │   │   │   ├── configuration.routes.ts
│   │   │   │   ├── config-editor/
│   │   │   │   │   └── config-editor.component.ts
│   │   │   │   ├── transformer-selector/
│   │   │   │   │   └── transformer-selector.component.ts
│   │   │   │   ├── config-validator/
│   │   │   │   │   └── config-validator.component.ts
│   │   │   │   └── config-history/
│   │   │   │       └── config-history.component.ts
│   │   │   │
│   │   │   ├── anonymization/
│   │   │   │   ├── anonymization.routes.ts
│   │   │   │   ├── anonymization-wizard/
│   │   │   │   │   └── anonymization-wizard.component.ts
│   │   │   │   ├── progress-monitor/
│   │   │   │   │   └── progress-monitor.component.ts
│   │   │   │   └── result-summary/
│   │   │   │       └── result-summary.component.ts
│   │   │   │
│   │   │   ├── job-history/
│   │   │   │   ├── job-history.routes.ts
│   │   │   │   ├── job-list/
│   │   │   │   │   └── job-list.component.ts
│   │   │   │   ├── job-detail/
│   │   │   │   │   └── job-detail.component.ts
│   │   │   │   └── job-retry/
│   │   │   │       └── job-retry.component.ts
│   │   │   │
│   │   │   └── audit-logs/
│   │   │       ├── audit-logs.routes.ts
│   │   │       ├── audit-log-viewer/
│   │   │       │   └── audit-log-viewer.component.ts
│   │   │       └── audit-log-export/
│   │   │           └── audit-log-export.component.ts
│   │   │
│   │   ├── app.component.ts
│   │   └── app.routes.ts
│   │
│   ├── styles.scss
│   ├── index.html
│   └── main.ts
│
├── angular.json
├── package.json
├── tsconfig.json
├── tailwind.config.js
└── ANGULAR_PROJECT_STRUCTURE.md
```

## Component Descriptions

### Core Services

#### ApiService
Central HTTP service for all backend API calls.
- Connection management
- Schema operations
- PII scanning
- Configuration management
- Anonymization
- Export
- Audit logging
- Authentication

#### AuthService
Manages user authentication state.
- Login/logout
- Token management
- Current user tracking
- Authentication state

#### ConnectionService
Manages database connections.
- Connection CRUD operations
- Connection testing
- Connection pooling

#### TenantService
Manages tenant context.
- Tenant selection
- Tenant isolation
- Tenant metadata

### Shared Components

#### ProgressBar
Displays progress of long-running operations.
- Percentage display
- Animated progress
- Time remaining estimation

#### ErrorAlert
Displays error messages.
- Error classification
- Error suggestions
- Dismissible alerts

#### ConfirmationDialog
Displays confirmation dialogs.
- Yes/No options
- Custom messages
- Callback handling

#### LoadingSpinner
Displays loading state.
- Animated spinner
- Loading message
- Overlay support

### Feature Components

#### Dashboard
Main dashboard page.
- Connection overview
- Quick actions
- Recent jobs
- System health

#### Connections
Connection management.
- List connections
- Create connection
- Edit connection
- Test connection
- Delete connection

#### PII Scan
PII detection and classification.
- Start scan
- View results
- Override classifications
- View sample data

#### Configuration
Anonymization configuration.
- Create configuration
- Edit configuration
- Validate configuration
- View version history
- Restore versions

#### Anonymization
Anonymization execution.
- Anonymization wizard
- Progress monitoring
- Real-time updates
- Cancellation support

#### Job History
Job history and management.
- List jobs
- View job details
- Retry jobs
- Export job logs

#### Audit Logs
Audit log viewing and export.
- List audit logs
- Filter logs
- View log details
- Export logs

## Styling

### Tailwind CSS
- Utility-first CSS framework
- Responsive design
- Dark mode support
- Custom theme configuration

### Global Styles
- `styles.scss` - Global styles
- Component-scoped styles
- Responsive breakpoints

## State Management

### BehaviorSubject
- Current user state
- Authentication state
- Connection state
- Job state

### RxJS Operators
- `map` - Transform data
- `catchError` - Error handling
- `switchMap` - Async operations
- `debounceTime` - Debouncing
- `distinctUntilChanged` - Deduplication

## HTTP Interceptors

### AuthInterceptor
- Adds JWT token to requests
- Handles token refresh

### ErrorInterceptor
- Handles HTTP errors
- Redirects on 401
- Shows error messages

## Guards

### AuthGuard
- Protects routes
- Redirects to login if not authenticated
- Preserves return URL

## Models

### Connection
```typescript
interface Connection {
  id: string;
  host: string;
  port: number;
  database: string;
  username: string;
  databaseType: 'postgresql' | 'mysql' | 'mongodb';
  useTLS: boolean;
  verifyCertificate: boolean;
  status: 'UNTESTED' | 'CONNECTED' | 'FAILED';
}
```

### PIIScan
```typescript
interface PIIScan {
  id: string;
  connectionId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  progress: number;
  classifications: ColumnClassification[];
}
```

### Configuration
```typescript
interface Configuration {
  id: string;
  version: number;
  content: string;
  format: 'yaml' | 'json';
  createdAt: Date;
}
```

### Anonymization
```typescript
interface Anonymization {
  id: string;
  sourceConnectionId: string;
  targetConnectionId: string;
  configurationId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  progress: number;
  rowsProcessed: number;
  totalRows: number;
}
```

## Routing

### Route Structure
- `/auth/login` - Login page
- `/dashboard` - Main dashboard
- `/connections` - Connection management
- `/pii-scan` - PII scanning
- `/configuration` - Configuration management
- `/anonymization` - Anonymization execution
- `/job-history` - Job history
- `/audit-logs` - Audit logs

### Lazy Loading
All feature modules are lazy-loaded for better performance.

## Build and Deployment

### Development
```bash
npm install
npm start
```

### Production Build
```bash
npm run build:prod
```

### Testing
```bash
npm test
```

## Dependencies

### Core
- `@angular/core` - Angular framework
- `@angular/common` - Common utilities
- `@angular/forms` - Form handling
- `@angular/router` - Routing
- `rxjs` - Reactive programming

### UI
- `tailwindcss` - CSS framework
- `@tailwindcss/forms` - Form styling

### HTTP
- `axios` - HTTP client (optional)

### Development
- `typescript` - TypeScript compiler
- `@angular/cli` - Angular CLI
- `tailwindcss` - Tailwind CSS

## Best Practices

1. **Standalone Components**: Use Angular 17+ standalone components
2. **Lazy Loading**: Lazy-load feature modules
3. **OnPush Change Detection**: Use OnPush strategy for performance
4. **Unsubscribe**: Use `takeUntilDestroyed` or `async` pipe
5. **Type Safety**: Use TypeScript interfaces
6. **Error Handling**: Implement comprehensive error handling
7. **Accessibility**: Follow WCAG guidelines
8. **Responsive Design**: Mobile-first approach
9. **Performance**: Optimize bundle size and load time
10. **Testing**: Write unit and integration tests

## Future Enhancements

1. **State Management**: Implement NgRx for complex state
2. **Caching**: Implement HTTP caching strategy
3. **Offline Support**: Add service worker for offline support
4. **Real-time Updates**: Implement WebSocket for real-time progress
5. **Dark Mode**: Add dark mode support
6. **Internationalization**: Add i18n support
7. **Analytics**: Add analytics tracking
8. **Performance Monitoring**: Add performance monitoring
