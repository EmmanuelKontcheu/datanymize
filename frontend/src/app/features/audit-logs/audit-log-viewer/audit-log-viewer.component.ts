import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService } from '../../../core/services/api.service';

/**
 * Audit Log Viewer Component
 * 
 * Displays audit log entries with:
 * - Table with columns: Timestamp, User, Action, Resource, Result
 * - Filtering by action, user, date range
 * - Pagination
 * - Export functionality
 * 
 * Validates Requirements: 16.1, 16.2, 16.3, 16.4
 */
@Component({
  selector: 'app-audit-log-viewer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-6xl mx-auto">
        <!-- Header -->
        <div class="mb-8 flex justify-between items-center">
          <div>
            <h1 class="text-3xl font-bold text-gray-900">Audit Logs</h1>
            <p class="mt-2 text-gray-600">View system activity and compliance records</p>
          </div>
          <button (click)="exportLogs()"
                  class="px-6 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
            Export Logs
          </button>
        </div>

        <!-- Filters -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <div class="grid grid-cols-1 md:grid-cols-5 gap-4">
            <!-- Action Filter -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Action</label>
              <select [(ngModel)]="filterAction" (change)="applyFilters()"
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
                <option value="">All Actions</option>
                <option value="LOGIN">Login</option>
                <option value="LOGOUT">Logout</option>
                <option value="CREATE_CONNECTION">Create Connection</option>
                <option value="DELETE_CONNECTION">Delete Connection</option>
                <option value="START_ANONYMIZATION">Start Anonymization</option>
                <option value="CANCEL_ANONYMIZATION">Cancel Anonymization</option>
                <option value="EXPORT_DATA">Export Data</option>
              </select>
            </div>

            <!-- User Filter -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">User</label>
              <input type="text" [(ngModel)]="filterUser" (change)="applyFilters()" placeholder="Username"
                     class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
            </div>

            <!-- From Date -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">From Date</label>
              <input type="date" [(ngModel)]="filterFromDate" (change)="applyFilters()"
                     class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
            </div>

            <!-- To Date -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">To Date</label>
              <input type="date" [(ngModel)]="filterToDate" (change)="applyFilters()"
                     class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
            </div>

            <!-- Result Filter -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Result</label>
              <select [(ngModel)]="filterResult" (change)="applyFilters()"
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
                <option value="">All Results</option>
                <option value="SUCCESS">Success</option>
                <option value="FAILURE">Failure</option>
              </select>
            </div>
          </div>

          <!-- Clear Filters -->
          <div class="mt-4">
            <button (click)="clearFilters()"
                    class="text-sm text-blue-600 hover:text-blue-800">
              Clear Filters
            </button>
          </div>
        </div>

        <!-- Audit Logs Table -->
        <div class="bg-white rounded-lg shadow overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead class="bg-gray-50 border-b">
                <tr>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700">Timestamp</th>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700">User</th>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700">Action</th>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700">Resource</th>
                  <th class="px-6 py-3 text-center text-sm font-medium text-gray-700">Result</th>
                  <th class="px-6 py-3 text-center text-sm font-medium text-gray-700">Details</th>
                </tr>
              </thead>
              <tbody class="divide-y">
                <tr *ngFor="let log of paginatedLogs" class="hover:bg-gray-50">
                  <td class="px-6 py-4 text-sm text-gray-900">{{ formatDate(log.timestamp) }}</td>
                  <td class="px-6 py-4 text-sm text-gray-600">{{ log.userId }}</td>
                  <td class="px-6 py-4 text-sm text-gray-600">{{ log.action }}</td>
                  <td class="px-6 py-4 text-sm text-gray-600">{{ log.resource }}</td>
                  <td class="px-6 py-4 text-center">
                    <span [class]="'inline-block px-3 py-1 rounded-full text-xs font-medium ' + getResultClass(log.result)">
                      {{ log.result }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-center">
                    <button (click)="viewDetails(log)"
                            class="text-blue-600 hover:text-blue-800 text-sm font-medium">
                      View
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Empty State -->
          <div *ngIf="paginatedLogs.length === 0" class="text-center py-12">
            <p class="text-gray-600">No audit logs found</p>
          </div>
        </div>

        <!-- Pagination -->
        <div *ngIf="totalPages > 1" class="mt-6 flex justify-between items-center">
          <p class="text-sm text-gray-600">
            Showing {{ (currentPage - 1) * pageSize + 1 }} to {{ Math.min(currentPage * pageSize, filteredLogs.length) }} of {{ filteredLogs.length }} logs
          </p>
          <div class="flex gap-2">
            <button (click)="previousPage()" [disabled]="currentPage === 1"
                    class="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-50">
              Previous
            </button>
            <div class="flex gap-1">
              <button *ngFor="let page of getPageNumbers()" 
                      (click)="goToPage(page)"
                      [class]="'px-3 py-2 rounded-lg ' + (page === currentPage ? 'bg-blue-500 text-white' : 'border border-gray-300 text-gray-700 hover:bg-gray-50')">
                {{ page }}
              </button>
            </div>
            <button (click)="nextPage()" [disabled]="currentPage === totalPages"
                    class="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 disabled:opacity-50">
              Next
            </button>
          </div>
        </div>
      </div>

      <!-- Details Modal -->
      <div *ngIf="selectedLog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg shadow-lg p-6 max-w-2xl w-full mx-4 max-h-96 overflow-y-auto">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-lg font-bold text-gray-900">Audit Log Details</h2>
            <button (click)="selectedLog = null"
                    class="text-gray-500 hover:text-gray-700 text-2xl">×</button>
          </div>

          <div class="space-y-4">
            <div>
              <p class="text-xs text-gray-600">Timestamp</p>
              <p class="text-sm font-medium text-gray-900">{{ formatDate(selectedLog.timestamp) }}</p>
            </div>

            <div>
              <p class="text-xs text-gray-600">User</p>
              <p class="text-sm font-medium text-gray-900">{{ selectedLog.userId }}</p>
            </div>

            <div>
              <p class="text-xs text-gray-600">Action</p>
              <p class="text-sm font-medium text-gray-900">{{ selectedLog.action }}</p>
            </div>

            <div>
              <p class="text-xs text-gray-600">Resource</p>
              <p class="text-sm font-medium text-gray-900">{{ selectedLog.resource }}</p>
            </div>

            <div>
              <p class="text-xs text-gray-600">Result</p>
              <p class="text-sm font-medium text-gray-900">{{ selectedLog.result }}</p>
            </div>

            <div *ngIf="selectedLog.metadata">
              <p class="text-xs text-gray-600">Metadata</p>
              <pre class="text-xs text-gray-700 bg-gray-50 p-3 rounded overflow-auto max-h-32">{{ selectedLog.metadata | json }}</pre>
            </div>

            <div *ngIf="selectedLog.ipAddress">
              <p class="text-xs text-gray-600">IP Address</p>
              <p class="text-sm font-medium text-gray-900">{{ selectedLog.ipAddress }}</p>
            </div>
          </div>

          <div class="mt-6 flex justify-end">
            <button (click)="selectedLog = null"
                    class="px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class AuditLogViewerComponent implements OnInit, OnDestroy {
  logs: any[] = [];
  filteredLogs: any[] = [];
  paginatedLogs: any[] = [];

  // Filters
  filterAction = '';
  filterUser = '';
  filterFromDate = '';
  filterToDate = '';
  filterResult = '';

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;

  selectedLog: any = null;

  private destroy$ = new Subject<void>();

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadAuditLogs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadAuditLogs(): void {
    this.apiService.get('/audit-logs')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.logs = response.data || [];
          this.applyFilters();
        },
        error: (err) => {
          console.error('Failed to load audit logs', err);
        }
      });
  }

  applyFilters(): void {
    this.filteredLogs = this.logs.filter(log => {
      // Action filter
      if (this.filterAction && log.action !== this.filterAction) {
        return false;
      }

      // User filter
      if (this.filterUser && !log.userId.toLowerCase().includes(this.filterUser.toLowerCase())) {
        return false;
      }

      // Date range filter
      if (this.filterFromDate) {
        const logDate = new Date(log.timestamp);
        const fromDate = new Date(this.filterFromDate);
        if (logDate < fromDate) {
          return false;
        }
      }

      if (this.filterToDate) {
        const logDate = new Date(log.timestamp);
        const toDate = new Date(this.filterToDate);
        toDate.setHours(23, 59, 59, 999);
        if (logDate > toDate) {
          return false;
        }
      }

      // Result filter
      if (this.filterResult && log.result !== this.filterResult) {
        return false;
      }

      return true;
    });

    // Update pagination
    this.currentPage = 1;
    this.totalPages = Math.ceil(this.filteredLogs.length / this.pageSize);
    this.updatePaginatedLogs();
  }

  clearFilters(): void {
    this.filterAction = '';
    this.filterUser = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterResult = '';
    this.applyFilters();
  }

  private updatePaginatedLogs(): void {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedLogs = this.filteredLogs.slice(start, end);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePaginatedLogs();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePaginatedLogs();
    }
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.updatePaginatedLogs();
  }

  getPageNumbers(): number[] {
    const pages = [];
    const maxPages = Math.min(5, this.totalPages);
    let startPage = Math.max(1, this.currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxPages - 1);

    if (endPage - startPage + 1 < maxPages) {
      startPage = Math.max(1, endPage - maxPages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    return pages;
  }

  viewDetails(log: any): void {
    this.selectedLog = log;
  }

  exportLogs(): void {
    // TODO: Implement export functionality
    alert('Export functionality coming soon');
  }

  getResultClass(result: string): string {
    switch (result) {
      case 'SUCCESS':
        return 'bg-green-100 text-green-800';
      case 'FAILURE':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }

  protected readonly Math = Math;
}
