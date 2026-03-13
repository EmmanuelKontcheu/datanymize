import { Routes } from '@angular/router';

export const AUDIT_LOGS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./audit-log-viewer/audit-log-viewer.component').then(m => m.AuditLogViewerComponent)
  }
];
