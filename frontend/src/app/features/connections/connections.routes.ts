import { Routes } from '@angular/router';

export const CONNECTIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./connection-list/connection-list.component').then(m => m.ConnectionListComponent)
  },
  {
    path: 'new',
    loadComponent: () => import('./connection-form/connection-form.component').then(m => m.ConnectionFormComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () => import('./connection-form/connection-form.component').then(m => m.ConnectionFormComponent)
  },
  {
    path: ':id',
    loadComponent: () => import('./connection-detail/connection-detail.component').then(m => m.ConnectionDetailComponent)
  }
];
