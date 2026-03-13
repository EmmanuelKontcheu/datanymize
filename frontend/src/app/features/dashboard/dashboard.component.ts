import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

/**
 * Dashboard component for Datanymize.
 * 
 * Displays:
 * - Connection overview with status indicators
 * - Quick action buttons
 * - Recent jobs summary
 * - System health status
 * 
 * Validates Requirements: 8.1
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="min-h-screen bg-gray-50">
      <!-- Header -->
      <header class="bg-white shadow">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <h1 class="text-3xl font-bold text-gray-900">Dashboard</h1>
          <p class="mt-2 text-gray-600">Welcome to Datanymize - Multi-database Anonymization Platform</p>
        </div>
      </header>

      <!-- Main Content -->
      <main class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <!-- Quick Actions -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div class="bg-white rounded-lg shadow p-6 hover:shadow-lg transition">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">New Connection</h3>
            <p class="text-gray-600 mb-4">Create a new database connection</p>
            <a routerLink="/connections/new" class="inline-block bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
              Create Connection
            </a>
          </div>

          <div class="bg-white rounded-lg shadow p-6 hover:shadow-lg transition">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">PII Scan</h3>
            <p class="text-gray-600 mb-4">Scan database for sensitive data</p>
            <a routerLink="/pii-scan" class="inline-block bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700">
              Start Scan
            </a>
          </div>

          <div class="bg-white rounded-lg shadow p-6 hover:shadow-lg transition">
            <h3 class="text-lg font-semibold text-gray-900 mb-4">Anonymize</h3>
            <p class="text-gray-600 mb-4">Start anonymization job</p>
            <a routerLink="/anonymization" class="inline-block bg-purple-600 text-white px-4 py-2 rounded hover:bg-purple-700">
              Start Anonymization
            </a>
          </div>
        </div>

        <!-- Connections Overview -->
        <div class="bg-white rounded-lg shadow p-6 mb-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-4">Connections</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div *ngFor="let connection of connections" class="border rounded-lg p-4">
              <h3 class="font-semibold text-gray-900">{{ connection.database }}</h3>
              <p class="text-sm text-gray-600">{{ connection.databaseType }}</p>
              <p class="text-sm text-gray-600">{{ connection.host }}:{{ connection.port }}</p>
              <div class="mt-2">
                <span [ngClass]="getStatusClass(connection.status)" class="px-2 py-1 rounded text-sm font-medium">
                  {{ connection.status }}
                </span>
              </div>
            </div>
          </div>
          <a routerLink="/connections" class="mt-4 inline-block text-blue-600 hover:text-blue-800">
            View All Connections →
          </a>
        </div>

        <!-- Recent Jobs -->
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-2xl font-bold text-gray-900 mb-4">Recent Jobs</h2>
          <div class="overflow-x-auto">
            <table class="min-w-full">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-900">Date</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-900">Type</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-900">Status</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-900">Rows</th>
                </tr>
              </thead>
              <tbody class="divide-y">
                <tr *ngFor="let job of recentJobs">
                  <td class="px-6 py-4 text-sm text-gray-900">{{ job.date | date:'short' }}</td>
                  <td class="px-6 py-4 text-sm text-gray-900">{{ job.type }}</td>
                  <td class="px-6 py-4 text-sm">
                    <span [ngClass]="getStatusClass(job.status)" class="px-2 py-1 rounded text-sm font-medium">
                      {{ job.status }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-sm text-gray-900">{{ job.rows }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <a routerLink="/job-history" class="mt-4 inline-block text-blue-600 hover:text-blue-800">
            View All Jobs →
          </a>
        </div>
      </main>
    </div>
  `,
  styles: []
})
export class DashboardComponent implements OnInit {
  connections: any[] = [];
  recentJobs: any[] = [];

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadConnections();
    this.loadRecentJobs();
  }

  loadConnections(): void {
    this.apiService.listConnections().subscribe({
      next: (response) => {
        if (response.success) {
          this.connections = response.data || [];
        }
      },
      error: (error) => {
        console.error('Failed to load connections', error);
      }
    });
  }

  loadRecentJobs(): void {
    // Mock data for demonstration
    this.recentJobs = [
      {
        date: new Date(),
        type: 'Anonymization',
        status: 'COMPLETED',
        rows: 100000
      },
      {
        date: new Date(Date.now() - 86400000),
        type: 'PII Scan',
        status: 'COMPLETED',
        rows: 50000
      },
      {
        date: new Date(Date.now() - 172800000),
        type: 'Export',
        status: 'COMPLETED',
        rows: 75000
      }
    ];
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
      case 'CONNECTED':
        return 'bg-green-100 text-green-800';
      case 'RUNNING':
        return 'bg-blue-100 text-blue-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'UNTESTED':
        return 'bg-gray-100 text-gray-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }
}
