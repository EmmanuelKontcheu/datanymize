import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService } from '../../../core/services/api.service';

/**
 * Result Summary Component
 * 
 * Displays anonymization completion results:
 * - Completion status
 * - Statistics (rows processed, duration, success rate)
 * - Errors or warnings
 * - Export options
 * 
 * Validates Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
@Component({
  selector: 'app-result-summary',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-2xl mx-auto">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Anonymization Results</h1>
          <p class="mt-2 text-gray-600">Job ID: {{ jobId }}</p>
        </div>

        <!-- Status Card -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <!-- Status Badge -->
          <div class="mb-6 flex items-center gap-4">
            <div [class]="'w-16 h-16 rounded-full flex items-center justify-center ' + getStatusBgClass()">
              <span [class]="'text-2xl ' + getStatusIconClass()">
                {{ getStatusIcon() }}
              </span>
            </div>
            <div>
              <h2 class="text-2xl font-bold text-gray-900">{{ result.status }}</h2>
              <p class="text-gray-600">{{ getStatusMessage() }}</p>
            </div>
          </div>

          <!-- Statistics Grid -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <!-- Rows Processed -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Rows Processed</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatNumber(result.rowsProcessed) }}</p>
            </div>

            <!-- Duration -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Duration</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatDuration(result.duration) }}</p>
            </div>

            <!-- Success Rate -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Success Rate</p>
              <p class="text-2xl font-bold text-gray-900">{{ getSuccessRate() }}%</p>
            </div>

            <!-- Processing Speed -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Processing Speed</p>
              <p class="text-2xl font-bold text-gray-900">{{ getProcessingSpeed() }}</p>
              <p class="text-xs text-gray-500 mt-1">rows/sec</p>
            </div>
          </div>

          <!-- Error Display -->
          <div *ngIf="result.status === 'FAILED' && result.errorMessage" 
               class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <h3 class="font-medium text-red-900 mb-2">Error Details</h3>
            <p class="text-sm text-red-800">{{ result.errorMessage }}</p>
          </div>

          <!-- Warnings -->
          <div *ngIf="warnings && warnings.length > 0" 
               class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
            <h3 class="font-medium text-yellow-900 mb-2">Warnings</h3>
            <ul class="text-sm text-yellow-800 space-y-1">
              <li *ngFor="let warning of warnings">• {{ warning }}</li>
            </ul>
          </div>

          <!-- Table Statistics -->
          <div *ngIf="tableStats && tableStats.length > 0" class="mb-6">
            <h3 class="font-medium text-gray-900 mb-3">Table Statistics</h3>
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
                  <tr *ngFor="let stat of tableStats" class="border-t">
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

          <!-- Buttons -->
          <div class="flex gap-4">
            <button *ngIf="result.status === 'COMPLETED'"
                    (click)="exportResults()"
                    class="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
              Export Results
            </button>
            <button (click)="goToDashboard()"
                    class="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">
              Back to Dashboard
            </button>
          </div>
        </div>

        <!-- Next Steps -->
        <div class="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 class="font-medium text-blue-900 mb-3">Next Steps</h3>
          <ul class="text-sm text-blue-800 space-y-2">
            <li *ngIf="result.status === 'COMPLETED'">
              ✓ Anonymization completed successfully
            </li>
            <li *ngIf="result.status === 'COMPLETED'">
              ✓ You can now export the anonymized data
            </li>
            <li *ngIf="result.status === 'COMPLETED'">
              ✓ View job history to track all anonymization jobs
            </li>
            <li *ngIf="result.status === 'FAILED'">
              ✗ Anonymization failed - please review the error details
            </li>
            <li *ngIf="result.status === 'FAILED'">
              ✗ Check your configuration and try again
            </li>
            <li *ngIf="result.status === 'CANCELLED'">
              ⊘ Anonymization was cancelled
            </li>
            <li *ngIf="result.status === 'CANCELLED'">
              ⊘ The target database has been rolled back
            </li>
          </ul>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ResultSummaryComponent implements OnInit, OnDestroy {
  jobId: string = '';
  
  result = {
    status: 'COMPLETED',
    rowsProcessed: 0,
    duration: 0,
    errorMessage: ''
  };

  tableStats: any[] = [];
  warnings: string[] = [];
  
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    this.jobId = this.route.snapshot.paramMap.get('id') || '';
    
    if (!this.jobId) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.loadResults();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadResults(): void {
    this.apiService.get(`/anonymizations/${this.jobId}/results`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.result = response.data || this.result;
          if (response.data?.tableStats) {
            this.tableStats = response.data.tableStats;
          }
          if (response.data?.warnings) {
            this.warnings = response.data.warnings;
          }
        },
        error: (err) => {
          console.error('Failed to load results', err);
        }
      });
  }

  exportResults(): void {
    // TODO: Implement export functionality
    alert('Export functionality coming soon');
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  getStatusIcon(): string {
    switch (this.result.status) {
      case 'COMPLETED':
        return '✓';
      case 'FAILED':
        return '✕';
      case 'CANCELLED':
        return '⊘';
      default:
        return '?';
    }
  }

  getStatusBgClass(): string {
    switch (this.result.status) {
      case 'COMPLETED':
        return 'bg-green-100';
      case 'FAILED':
        return 'bg-red-100';
      case 'CANCELLED':
        return 'bg-yellow-100';
      default:
        return 'bg-gray-100';
    }
  }

  getStatusIconClass(): string {
    switch (this.result.status) {
      case 'COMPLETED':
        return 'text-green-600';
      case 'FAILED':
        return 'text-red-600';
      case 'CANCELLED':
        return 'text-yellow-600';
      default:
        return 'text-gray-600';
    }
  }

  getStatusMessage(): string {
    switch (this.result.status) {
      case 'COMPLETED':
        return 'Anonymization completed successfully';
      case 'FAILED':
        return 'Anonymization failed';
      case 'CANCELLED':
        return 'Anonymization was cancelled';
      default:
        return 'Unknown status';
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
    // This would be calculated based on actual data
    return this.result.status === 'COMPLETED' ? 100 : 0;
  }

  getProcessingSpeed(): string {
    if (this.result.duration === 0 || this.result.rowsProcessed === 0) {
      return '0';
    }
    const seconds = this.result.duration / 1000;
    const speed = this.result.rowsProcessed / seconds;
    return speed.toFixed(2);
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
