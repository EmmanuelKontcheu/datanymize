import { Routes } from '@angular/router';

export const CONFIGURATION_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./config-editor/config-editor.component').then(m => m.ConfigEditorComponent)
  },
  {
    path: ':id/history',
    loadComponent: () => import('./config-history/config-history.component').then(m => m.ConfigHistoryComponent)
  }
];
