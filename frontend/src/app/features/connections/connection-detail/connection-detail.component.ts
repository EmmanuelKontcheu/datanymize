import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ConnectionTestComponent } from '../connection-test/connection-test.component';

/**
 * Connection detail component for displaying connection information.
 * 
 * Features:
 * - Display connection information
 * - Show available actions (Schema Sync, PII Scan, Anonymize)
 * - Display connection history
 * - Test connection functionality
 * - Edit and delete options
 * 
 * Validates Requirements: 8.4, 8.5
 */
@Component({
  selector: 'app-connection-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ConnectionTestComponent],
  template: `
    <div class="min-h-screen bg-gray-50">
      <!-- Header -->
      <header class="bg-white shadow">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div class="flex justify-between items-center">
            <div class="flex items-center">
              <a routerLink="/connections" class="text-blue-600 hover:text-blue-800 mr-4">← Back</a>
              <div>
                <h1 class="text-3xl font-bold text-gray-900">{{ connection?.database }}</h1>
                <p class="mt-2 text-gray-600">{{ connection?.host }}:{{ connection?.port }}</p>
              </div>
            </div>
            <div class="flex gap-2">
              <a [routerLink]="['/connections', connectionId, 'edit']" class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
                Edit
              </a>
              <button (click)="deleteConnection()" class="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700">
                Delete
              </button>
            </div>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <!-- Loading State -->
        <div *ngIf="loading" class="text-center py-12">
          <svg class="animate-spin h-12 w-12 text-blue-600 mx-auto" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p class="mt-4 text-gray-600">Loading connection details...</p>
        </div>

        <!-- Error State -->
        <div *ngIf="error && !loading" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p class="text-red-800">{{ error }}</p>
        </div>

        <!-- Connection Details -->
        <div *ngIf="!loading && connection" class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- Left Column: Connection Info -->
          <div class="lg:col-span-2 space-y-6">
            <!-- Connection Information -->
            <div class="bg-white rounded-lg shadow p-6">
              <h2 class="text-xl font-bold text-gray-900 mb-4">Connection Information</h2>
              <div class="grid grid-cols-2 gap-4">
                <div>
                  <p class="text-sm text-gray-600">Database Type</p>
                  <p class="text-lg font-semibold text-gray-900">{{ connection.databaseType | uppercase }}</p>
                </div>
                <div>
                  <p class="text-sm text-gray-600">Status</p>
                  <span [ngClass]="getStatusClass(connection.status)" class="px-3 py-1 inline-flex text-sm leading-5 font-semibold rounded-full">
                    {{ connection.status }}
                  </span>
                </div>
                <div>
                  <p class="text-sm text-gray-600">Host</p>
                  <p class="text-lg font-semibold text-gray-900">{{ connection.host }}</p>
                </div>
                <div>
                  <p class="text-sm text-gray-600">Port</p>
                  <p class="text-lg font-semibold text-gray-900">{{ connection.port }}</p>
                </div>
                <div>
                  <p class="text-sm text-gray-600">Username</p>
                  <p class="text-lg font-semibold text-gray-900">{{ connection.username }}</p>
                </div>
                <div>
                  <p class="text-sm text-gray-600">Database</p>
                  <p class="text-lg font-semibold text-gray-900">{{ connection.database }}</p>
                </div>
                <div>
                  <p class="text-sm text-gray-600">TLS/SSL</p>
                  <p class="text-lg font-semibold text-gray-900">
                    {{ connection.useTLS ? '✓ Enabled' : '✗ Disabled' }}
                  </p>
                </div>
                <div>
                  <p class="text-sm text-gray-600">Certificate Verification</p>
                  <p class="text-lg font-semibold text-gray-900">
                    {{ connection.verifyCertificate ? '✓ Enabled' : '✗ Disabled' }}
                  </p>
                </div>
              </div>
            </div>

            <!-- Connection Test -->
            <app-connection-test [connectionId]="connectionId"></app-connection-test>

            <!-- Available Actions -->
            <div class="bg-white rounded-lg shadow p-6">
              <h2 class="text-xl font-bold text-gray-900 mb-4">Available Actions</h2>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <a routerLink="/pii-scan" [queryParams]="{ connectionId: connectionId }" class="p-4 border border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition">
                  <h3 class="font-semibold text-gray-900">PII Scan</h3>
                  <p class="text-sm text-gray-600 mt-1">Scan for sensitive data</p>
                </a>
                <a routerLink="/configuration" [queryParams]="{ connectionId: connectionId }" class="p-4 border border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition">
                  <h3 class="font-semibold text-gray-900">Configuration</h3>
                  <p class="text-sm text-gray-600 mt-1">Create anonymization rules</p>
                </a>
                <a routerLink="/anonymization" [queryParams]="{ sourceConnectionId: connectionId }" class="p-4 border border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition">
                  <h3 class="font-semibold text-gray-900">Anonymize</h3>
                  <p class="text-sm text-gray-600 mt-1">Start anonymization job</p>
                </a>
                <a routerLink="/job-history" [queryParams]="{ connectionId: connectionId }" class="p-4 border border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition">
                  <h3 class="font-semibold text-gray-900">Job History</h3>
                  <p class="text-sm text-gray-600 mt-1">View past operations</p>
                </a>
              </div>
            </div>
          </div>

          <!-- Right Column: Quick Stats -->
          <div class="space-y-6">
            <!-- Connection Stats -->
            <div class="bg-white rounded-lg shadow p-6">
              <h3 class="text-lg font-bold text-gray-900 mb-4">Statistics</h3>
              <div class="space-y-4">
                <div class="border-b pb-4">
                  <p class="text-sm text-gray-600">Created</p>
                  <p class="text-lg font-semibold text-gray-900">
                    {{ connection.createdAt | date:'short' }}
                  </p>
                </div>
                <div class="border-b pb-4">
                  <p class="text-sm text-gray-600">Last Tested</p>
                  <p class="text-lg font-semibold text-gray-900">
                    {{ connection.lastTestedAt | date:'short' || 'Never' }}
                  </p>
                </div>
                <div>
                  <p class="text-sm text-gray-600">Connection ID</p>
                  <p class="text-xs font-mono text-gray-900 break-all">{{ connection.id }}</p>
                </div>
              </div>
            </div>

            <!-- Security Info -->
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-6">
              <h3 class="text-lg font-bold text-blue-900 mb-4">Security</h3>
              <ul class="text-sm text-blue-800 space-y-2">
                <li class="flex items-start">
                  <span class="mr-2">✓</span>
                  <span>Passwords encrypted at rest</span>
                </li>
                <li class="flex items-start">
                  <span class="mr-2">✓</span>
                  <span>Read-only access enforced</span>
                </li>
                <li class="flex items-start">
                  <span class="mr-2">✓</span>
                  <span>All access logged</span>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </main>
    </div>
  `,
  styles: []
})
export class ConnectionDetailComponent implements OnInit {
  connection: any = null;
  connectionId: string | null = null;
  loading = false;
  error = '';

  constructor(
    private apiService: ApiService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.connectionId = params['id'];
        this.loadConnection();
      }
    });
  }

  loadConnection(): void {
    if (!this.connectionId) return;

    this.loading = true;
    this.error = '';

    this.apiService.getConnection(this.connectionId).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          this.connection = response.data;
        }
      },
      error: (error) => {
        this.loading = false;
        this.error = error.error?.message || 'Failed to load connection';
      }
    });
  }

  deleteConnection(): void {
    if (!this.connectionId) return;

    if (confirm('Are you sure you want to delete this connection? This action cannot be undone.')) {
      this.apiService.deleteConnection(this.connectionId).subscribe({
        next: () => {
          this.router.navigate(['/connections']);
        },
        error: (error) => {
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
}
