import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';

/**
 * Connection list component for displaying all database connections.
 * 
 * Features:
 * - Display all connections in a table
 * - Show connection status (connected, failed, untested)
 * - Edit and delete actions
 * - Connection filtering and sorting
 * - Create new connection button
 * 
 * Validates Requirements: 8.1, 8.4, 8.5
 */
@Component({
  selector: 'app-connection-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gray-50">
      <!-- Header -->
      <header class="bg-white shadow">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div class="flex justify-between items-center">
            <div>
              <h1 class="text-3xl font-bold text-gray-900">Connections</h1>
              <p class="mt-2 text-gray-600">Manage your database connections</p>
            </div>
            <a routerLink="/connections/new" class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition">
              + New Connection
            </a>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <!-- Filters -->
        <div class="bg-white rounded-lg shadow p-4 mb-6">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Database Type</label>
              <select
                [(ngModel)]="filterType"
                (change)="applyFilters()"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              >
                <option value="">All Types</option>
                <option value="postgresql">PostgreSQL</option>
                <option value="mysql">MySQL</option>
                <option value="mongodb">MongoDB</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Status</label>
              <select
                [(ngModel)]="filterStatus"
                (change)="applyFilters()"
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              >
                <option value="">All Status</option>
                <option value="CONNECTED">Connected</option>
                <option value="FAILED">Failed</option>
                <option value="UNTESTED">Untested</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Search</label>
              <input
                type="text"
                [(ngModel)]="searchTerm"
                (keyup)="applyFilters()"
                placeholder="Search by name or host..."
                class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 outline-none"
              />
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div *ngIf="loading" class="text-center py-12">
          <div class="inline-block">
            <svg class="animate-spin h-12 w-12 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
          <p class="mt-4 text-gray-600">Loading connections...</p>
        </div>

        <!-- Error State -->
        <div *ngIf="error && !loading" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p class="text-red-800">{{ error }}</p>
        </div>

        <!-- Empty State -->
        <div *ngIf="!loading && filteredConnections.length === 0" class="bg-white rounded-lg shadow p-12 text-center">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
          </svg>
          <h3 class="mt-4 text-lg font-medium text-gray-900">No connections found</h3>
          <p class="mt-2 text-gray-600">Get started by creating your first database connection.</p>
          <a routerLink="/connections/new" class="mt-4 inline-block bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
            Create Connection
          </a>
        </div>

        <!-- Connections Table -->
        <div *ngIf="!loading && filteredConnections.length > 0" class="bg-white rounded-lg shadow overflow-hidden">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Type</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Host</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr *ngFor="let connection of filteredConnections" class="hover:bg-gray-50 transition">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-gray-900">{{ connection.database }}</div>
                  <div class="text-sm text-gray-500">{{ connection.username }}&#64;{{ connection.host }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full" [ngClass]="getDatabaseTypeClass(connection.databaseType)">
                    {{ connection.databaseType | uppercase }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                  {{ connection.host }}:{{ connection.port }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full" [ngClass]="getStatusClass(connection.status)">
                    {{ connection.status }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <a [routerLink]="['/connections', connection.id]" class="text-blue-600 hover:text-blue-900 mr-4">
                    View
                  </a>
                  <a [routerLink]="['/connections', connection.id, 'edit']" class="text-blue-600 hover:text-blue-900 mr-4">
                    Edit
                  </a>
                  <button (click)="deleteConnection(connection.id)" class="text-red-600 hover:text-red-900">
                    Delete
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </div>
  `,
  styles: []
})
export class ConnectionListComponent implements OnInit {
  connections: any[] = [];
  filteredConnections: any[] = [];
  loading = false;
  error = '';
  filterType = '';
  filterStatus = '';
  searchTerm = '';

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadConnections();
  }

  loadConnections(): void {
    this.loading = true;
    this.error = '';

    this.apiService.listConnections().subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.connections = response.data || [];
          this.applyFilters();
        }
      },
      error: (error: any) => {
        this.loading = false;
        this.error = error.error?.message || 'Failed to load connections';
      }
    });
  }

  applyFilters(): void {
    this.filteredConnections = this.connections.filter(conn => {
      const typeMatch = !this.filterType || conn.databaseType === this.filterType;
      const statusMatch = !this.filterStatus || conn.status === this.filterStatus;
      const searchMatch = !this.searchTerm || 
        conn.database.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        conn.host.toLowerCase().includes(this.searchTerm.toLowerCase());
      
      return typeMatch && statusMatch && searchMatch;
    });
  }

  deleteConnection(id: string): void {
    if (confirm('Are you sure you want to delete this connection?')) {
      this.apiService.deleteConnection(id).subscribe({
        next: () => {
          this.loadConnections();
        },
        error: (error: any) => {
          this.error = error.error?.message || 'Failed to delete connection';
        }
      });
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'CONNECTED':
        return 'bg-green-100 text-green-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'UNTESTED':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  getDatabaseTypeClass(type: string): string {
    switch (type) {
      case 'postgresql':
        return 'bg-blue-100 text-blue-800';
      case 'mysql':
        return 'bg-orange-100 text-orange-800';
      case 'mongodb':
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }
}
