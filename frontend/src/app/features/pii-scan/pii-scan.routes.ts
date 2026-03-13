import { Routes } from '@angular/router';

export const PII_SCAN_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./scan-initiation/scan-initiation.component').then(m => m.ScanInitiationComponent)
  },
  {
    path: ':id/results',
    loadComponent: () => import('./scan-results/scan-results.component').then(m => m.ScanResultsComponent)
  }
];
