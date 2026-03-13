import { Routes } from '@angular/router';

export const ANONYMIZATION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./anonymization-wizard/anonymization-wizard.component').then(m => m.AnonymizationWizardComponent)
  },
  {
    path: ':id/progress',
    loadComponent: () => import('./progress-monitor/progress-monitor.component').then(m => m.ProgressMonitorComponent)
  },
  {
    path: ':id/results',
    loadComponent: () => import('./result-summary/result-summary.component').then(m => m.ResultSummaryComponent)
  }
];
