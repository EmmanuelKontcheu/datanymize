import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService } from '../../../core/services/api.service';

/**
 * Job History List Component
 * 
 * Displays job history with:
 * - Table with columns: Date, Time, Source DB, Target DB, Status, Rows Processed
 * - Sorting and filtering
 * - Pagination for large result sets
 * - Status indicators
 * 
 * Validates Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
@Component({
  selector: 'app-job-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-6xl mx-auto">
        <!-- Header -->
        <div class="mb-8 flex justify-between items-center">
          <div>
            <h1 class="text-3xl font-bold text-gray-900">Job History</h1>
            <p class="mt-2 text-gray-600">View all anonymization jobs</p>
          </div>
          <button (click)="goToNewJob()"
                  class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
            New Anonymization
          </button>
        </div>

        <!-- Filters -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <!-- Status Filter -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Status</label>
              <select [(ngModel)]="filterStatus" (change)="applyFilters()"
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
                <option value="">All Statuses</option>
                <option value="COMPLETED">Completed</option>
                <option value="FAILED">Failed</option>
                <option value="IN_PROGRESS">In Progress</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>

            <!-- Date Range -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">From Date</label>
              <input type="date" [(ngModel)]="filterFromDate" (change)="applyFilters()"
                     class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">To Date</label>
              <input type="date" [(ngModel)]="filterToDate" (change)="applyFilters()"
                     class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
            </div>

            <!-- Search -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Search</label>
              <input type="text" [(ngModel)]="filterSearch" (change)="applyFilters()" placeholder="Job ID or DB name"
                     class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500">
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

        <!-- Jobs Table -->
        <div class="bg-white rounded-lg shadow overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead class="bg-gray-50 border-b">
                <tr>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700 cursor-pointer hover:bg-gray-100"
                      (click)="sortBy('date')">
                    Date & Time
                    <span *ngIf="sortField === 'date'" class="ml-1">{{ sortDirection === 'asc' ? '↑' : '↓' }}</span>
                  </th>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700 cursor-pointer hover:bg-gray-100"
                      (click)="sortBy('sourceDb')">
                    Source DB
                    <span *ngIf="sortField === 'sourceDb'" class="ml-1">{{ sortDirection === 'asc' ? '↑' : '↓' }}</span>
                  </th>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700 cursor-pointer hover:bg-gray-100"
                      (click)="sortBy('targetDb')">
                    Target DB
                    <span *ngIf="sortField === 'targetDb'" class="ml-1">{{ sortDirection === 'asc' ? '↑' : '↓' }}</span>
                  </th>
                  <th class="px-6 py-3 text-left text-sm font-medium text-gray-700 cursor-pointer hover:bg-gray-100"
                      (click)="sortBy('status')">
                    Status
                    <span *ngIf="sortField === 'status'" class="ml-1">{{ sortDirection === 'asc' ? '↑' : '↓' }}</span>
                  </th>
                  <th class="px-6 py-3 text-right text-sm font-medium text-gray-700 cursor-pointer hover:bg-gray-100"
                      (click)="sortBy('rowsProcessed')">
                    Rows Processed
                    <span *ngIf="sortField === 'rowsProcessed'" class="ml-1">{{ sortDirection === 'asc' ? '↑' : '↓' }}</span>
                  </th>
                  <th class="px-6 py-3 text-center text-sm font-medium text-gray-700">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y">
                <tr *ngFor="let job of paginatedJobs" class="hover:bg-gray-50">
                  <td class="px-6 py-4 text-sm text-gray-900">
                    {{ formatDate(job.createdAt) }}
                  </td>
                  <td class="px-6 py-4 text-sm text-gray-600">{{ job.sourceDatabase }}</td>
                  <td class="px-6 py-4 text-sm text-gray-600">{{ job.targetDatabase }}</td>
                  <td class="px-6 py-4 text-sm">
                    <span [class]="'inline-block px-3 py-1 rounded-full text-xs font-medium ' + getStatusClass(job.status)">
                      {{ job.status }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-sm text-right text-gray-900">{{ formatNumber(job.rowsProcessed) }}</td>
                  <td class="px-6 py-4 text-center">
                    <button (click)="viewJob(job.id)"
                            class="text-blue-600 hover:text-blue-800 text-sm font-medium">
                      View
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- Empty State -->
          <div *ngIf="paginatedJobs.length === 0" class="text-center py-12">
            <p class="text-gray-600">No jobs found</p>
          </div>
        </div>

        <!-- Pagination -->
        <div *ngIf="totalPages > 1" class="mt-6 flex justify-between items-center">
          <p class="text-sm text-gray-600">
            Showing {{ (currentPage - 1) * pageSize + 1 }} to {{ Math.min(currentPage * pageSize, filteredJobs.length) }} of {{ filteredJobs.length }} jobs
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
    </div>
  `,
  styles: []
})
export class JobListComponent implements OnInit, OnDestroy {
  jobs: any[] = [];
  filteredJobs: any[] = [];
  paginatedJobs: any[] = [];

  // Filters
  filterStatus = '';
  filterFromDate = '';
  filterToDate = '';
  filterSearch = '';

  // Sorting
  sortField = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;

  private destroy$ = new Subject<void>();

  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadJobs(): void {
    this.apiService.get('/anonymizations')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.jobs = response.data || [];
          this.applyFilters();
        },
        error: (err: any) => {
          console.error('Failed to load jobs', err);
        }
      });
  }

  applyFilters(): void {
    this.filteredJobs = this.jobs.filter(job => {
      // Status filter
      if (this.filterStatus && job.status !== this.filterStatus) {
        return false;
      }

      // Date range filter
      if (this.filterFromDate) {
        const jobDate = new Date(job.createdAt);
        const fromDate = new Date(this.filterFromDate);
        if (jobDate < fromDate) {
          return false;
        }
      }

      if (this.filterToDate) {
        const jobDate = new Date(job.createdAt);
        const toDate = new Date(this.filterToDate);
        toDate.setHours(23, 59, 59, 999);
        if (jobDate > toDate) {
          return false;
        }
      }

      // Search filter
      if (this.filterSearch) {
        const search = this.filterSearch.toLowerCase();
        if (!job.id.toLowerCase().includes(search) &&
            !job.sourceDatabase.toLowerCase().includes(search) &&
            !job.targetDatabase.toLowerCase().includes(search)) {
          return false;
        }
      }

      return true;
    });

    // Apply sorting
    this.filteredJobs.sort((a, b) => {
      let aVal = a[this.sortField];
      let bVal = b[this.sortField];

      if (typeof aVal === 'string') {
        aVal = aVal.toLowerCase();
        bVal = bVal.toLowerCase();
      }

      const comparison = aVal < bVal ? -1 : aVal > bVal ? 1 : 0;
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });

    // Update pagination
    this.currentPage = 1;
    this.totalPages = Math.ceil(this.filteredJobs.length / this.pageSize);
    this.updatePaginatedJobs();
  }

  clearFilters(): void {
    this.filterStatus = '';
    this.filterFromDate = '';
    this.filterToDate = '';
    this.filterSearch = '';
    this.applyFilters();
  }

  sortBy(field: string): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.applyFilters();
  }

  private updatePaginatedJobs(): void {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.paginatedJobs = this.filteredJobs.slice(start, end);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePaginatedJobs();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePaginatedJobs();
    }
  }

  goToPage(page: number): void {
    this.currentPage = page;
    this.updatePaginatedJobs();
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

  viewJob(jobId: string): void {
    this.router.navigate(['/job-history', jobId]);
  }

  goToNewJob(): void {
    this.router.navigate(['/anonymization']);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'bg-green-100 text-green-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800';
      case 'CANCELLED':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }

  formatNumber(num: number): string {
    return new Intl.NumberFormat('en-US').format(num);
  }

  protected readonly Math = Math;
}
