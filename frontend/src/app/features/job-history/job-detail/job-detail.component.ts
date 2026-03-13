import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService } from '../../../core/services/api.service';

/**
 * Job Detail Component
 * 
 * Displays full job information:
 * - Job details
 * - Configuration used
 * - Detailed statistics
 * - Errors or warnings
 * - Retry button
 * 
 * Validates Requirements: 12.1, 12.2, 12.3, 12.4, 12.5
 */
@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-4xl mx-auto">
        <!-- Header -->
        <div class="mb-8 flex justify-between items-center">
          <div>
            <h1 class="text-3xl font-bold text-gray-900">Job Details</h1>
            <p class="mt-2 text-gray-600">Job ID: {{ jobId }}</p>
          </div>
          <button (click)="goBack()"
                  class="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
            Back to History
          </button>
        </div>

        <!-- Status Card -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <div class="flex items-center justify-between mb-6">
            <div class="flex items-center gap-4">
              <div [class]="'w-12 h-12 rounded-full flex items-center justify-center ' + getStatusBgClass()">
                <span [class]="'text-xl ' + getStatusIconClass()">{{ getStatusIcon() }}</span>
              </div>
              <div>
                <h2 class="text-2xl font-bold text-gray-900">{{ job.status }}</h2>
                <p class="text-gray-600">{{ formatDate(job.createdAt) }}</p>
              </div>
            </div>
            <button *ngIf="job.status === 'FAILED'" (click)="retryJob()"
                    class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
              Retry Job
            </button>
          </div>

          <!-- Statistics Grid -->
          <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Rows Processed</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatNumber(job.rowsProcessed) }}</p>
            </div>

            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Duration</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatDuration(job.duration) }}</p>
            </div>

            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Processing Speed</p>
              <p class="text-2xl font-bold text-gray-900">{{ getProcessingSpeed() }}</p>
              <p class="text-xs text-gray-500 mt-1">rows/sec</p>
            </div>

            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Success Rate</p>
              <p class="text-2xl font-bold text-gray-900">{{ getSuccessRate() }}%</p>
            </div>
          </div>
        </div>

        <!-- Job Information -->
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
          <!-- Source Database -->
          <div class="bg-white rounded-lg shadow p-6">
            <h3 class="text-lg font-bold text-gray-900 mb-4">Source Database</h3>
            <div class="space-y-3">
              <div>
                <p class="text-xs text-gray-600">Database Name</p>
                <p class="text-sm font-medium text-gray-900">{{ job.sourceDatabase }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-600">Connection ID</p>
                <p class="text-sm font-medium text-gray-900">{{ job.sourceConnectionId }}</p>
              </div>
            </div>
          </div>

          <!-- Target Database -->
          <div class="bg-white rounded-lg shadow p-6">
            <h3 class="text-lg font-bold text-gray-900 mb-4">Target Database</h3>
            <div class="space-y-3">
              <div>
                <p class="text-xs text-gray-600">Database Name</p>
                <p class="text-sm font-medium text-gray-900">{{ job.targetDatabase }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-600">Connection ID</p>
                <p class="text-sm font-medium text-gray-900">{{ job.targetConnectionId }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Configuration -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <h3 class="text-lg font-bold text-gray-900 mb-4">Configuration</h3>
          <div class="bg-gray-50 rounded-lg p-4">
            <pre class="text-xs text-gray-700 overflow-auto max-h-48">{{ job.configuration | json }}</pre>
          </div>
        </div>

        <!-- Error Details -->
        <div *ngIf="job.status === 'FAILED' && job.errorMessage" 
             class="bg-red-50 border border-red-200 rounded-lg p-6 mb-6">
          <h3 class="text-lg font-bold text-red-900 mb-4">Error Details</h3>
          <p class="text-sm text-red-800">{{ job.errorMessage }}</p>
        </div>

        <!-- Table Statistics -->
        <div *ngIf="job.tableStats && job.tableStats.length > 0" 
             class="bg-white rounded-lg shadow p-6 mb-6">
          <h3 class="text-lg font-bold text-gray-900 mb-4">Table Statistics</h3>
          <div class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-gray-50">
                <tr>
                  <th class="px-4 py-2 text-left text-gray-700 font-medium">Table Name</th>
                  <th class="px-4 py-2 text-right text-gray-700 font-medium">Rows Processed</th>
                  <th class="px-4 py-2 text-right text-gray-700 font-medium">Status</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let stat of job.tableStats" class="border-t">
                  <td class="px-4 py-2 text-gray-900">{{ stat.tableName }}</td>
                  <td class="px-4 py-2 text-right text-gray-600">{{ formatNumber(stat.rowsProcessed) }}</td>
                  <td class="px-4 py-2 text-right">
                    <span [class]="'inline-block px-2 py-1 rounded text-xs font-medium ' + getTableStatusClass(stat.status)">
                      {{ stat.status }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Warnings -->
        <div *ngIf="job.warnings && job.warnings.length > 0" 
             class="bg-yellow-50 border border-yellow-200 rounded-lg p-6">
          <h3 class="text-lg font-bold text-yellow-900 mb-4">Warnings</h3>
          <ul class="text-sm text-yellow-800 space-y-2">
            <li *ngFor="let warning of job.warnings">• {{ warning }}</li>
          </ul>
        </div>
      </div>

      <!-- Retry Confirmation Modal -->
      <div *ngIf="showRetryConfirmation" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg shadow-lg p-6 max-w-sm">
          <h2 class="text-lg font-bold text-gray-900 mb-4">Retry Job?</h2>
          <p class="text-gray-600 mb-6">
            This will re-execute the anonymization job with the same configuration.
          </p>
          <div class="flex gap-4">
            <button (click)="showRetryConfirmation = false"
                    class="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">
              Cancel
            </button>
            <button (click)="confirmRetry()"
                    [disabled]="isRetrying"
                    class="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400">
              {{ isRetrying ? 'Retrying...' : 'Retry' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class JobDetailComponent implements OnInit, OnDestroy {
  jobId: string = '';
  
  job = {
    id: '',
    status: 'COMPLETED',
    createdAt: '',
    sourceDatabase: '',
    targetDatabase: '',
    sourceConnectionId: '',
    targetConnectionId: '',
    rowsProcessed: 0,
    duration: 0,
    configuration: {},
    errorMessage: '',
    tableStats: [],
    warnings: []
  };

  showRetryConfirmation = false;
  isRetrying = false;

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    this.jobId = this.route.snapshot.paramMap.get('id') || '';
    
    if (!this.jobId) {
      this.router.navigate(['/job-history']);
      return;
    }

    this.loadJobDetails();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadJobDetails(): void {
    this.apiService.get(`/anonymizations/${this.jobId}`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.job = response.data || this.job;
        },
        error: (err) => {
          console.error('Failed to load job details', err);
        }
      });
  }

  retryJob(): void {
    this.showRetryConfirmation = true;
  }

  confirmRetry(): void {
    this.isRetrying = true;

    const request = {
      sourceConnectionId: this.job.sourceConnectionId,
      targetConnectionId: this.job.targetConnectionId,
      configurationId: this.job.configuration['id'] || ''
    };

    this.apiService.post('/anonymizations', request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          const jobId = response.data?.jobId;
          if (jobId) {
            this.router.navigate(['/anonymization', jobId, 'progress']);
          }
        },
        error: (err) => {
          this.isRetrying = false;
          console.error('Failed to retry job', err);
        }
      });
  }

  goBack(): void {
    this.router.navigate(['/job-history']);
  }

  getStatusIcon(): string {
    switch (this.job.status) {
      case 'COMPLETED':
        return '✓';
      case 'FAILED':
        return '✕';
      case 'CANCELLED':
        return '⊘';
      case 'IN_PROGRESS':
        return '⟳';
      default:
        return '?';
    }
  }

  getStatusBgClass(): string {
    switch (this.job.status) {
      case 'COMPLETED':
        return 'bg-green-100';
      case 'FAILED':
        return 'bg-red-100';
      case 'CANCELLED':
        return 'bg-yellow-100';
      case 'IN_PROGRESS':
        return 'bg-blue-100';
      default:
        return 'bg-gray-100';
    }
  }

  getStatusIconClass(): string {
    switch (this.job.status) {
      case 'COMPLETED':
        return 'text-green-600';
      case 'FAILED':
        return 'text-red-600';
      case 'CANCELLED':
        return 'text-yellow-600';
      case 'IN_PROGRESS':
        return 'text-blue-600';
      default:
        return 'text-gray-600';
    }
  }

  getTableStatusClass(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return 'bg-green-100 text-green-800';
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  getSuccessRate(): number {
    return this.job.status === 'COMPLETED' ? 100 : 0;
  }

  getProcessingSpeed(): string {
    if (this.job.duration === 0 || this.job.rowsProcessed === 0) {
      return '0';
    }
    const seconds = this.job.duration / 1000;
    const speed = this.job.rowsProcessed / seconds;
    return speed.toFixed(2);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }

  formatNumber(num: number): string {
    return new Intl.NumberFormat('en-US').format(num);
  }

  formatDuration(ms: number): string {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
      return `${hours}h ${minutes % 60}m`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds % 60}s`;
    } else {
      return `${seconds}s`;
    }
  }
}
