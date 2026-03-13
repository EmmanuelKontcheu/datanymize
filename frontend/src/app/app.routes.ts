import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    path: 'connections',
    canActivate: [authGuard],
    loadChildren: () => import('./features/connections/connections.routes').then(m => m.CONNECTIONS_ROUTES)
  },
  {
    path: 'pii-scan',
    canActivate: [authGuard],
    loadChildren: () => import('./features/pii-scan/pii-scan.routes').then(m => m.PII_SCAN_ROUTES)
  },
  {
    path: 'configuration',
    canActivate: [authGuard],
    loadChildren: () => import('./features/configuration/configuration.routes').then(m => m.CONFIGURATION_ROUTES)
  },
  {
    path: 'anonymization',
    canActivate: [authGuard],
    loadChildren: () => import('./features/anonymization/anonymization.routes').then(m => m.ANONYMIZATION_ROUTES)
  },
  {
    path: 'job-history',
    canActivate: [authGuard],
    loadChildren: () => import('./features/job-history/job-history.routes').then(m => m.JOB_HISTORY_ROUTES)
  },
  {
    path: 'audit-logs',
    canActivate: [authGuard],
    loadChildren: () => import('./features/audit-logs/audit-logs.routes').then(m => m.AUDIT_LOGS_ROUTES)
  },
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  }
];
