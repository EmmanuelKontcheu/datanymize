import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, interval } from 'rxjs';
import { takeUntil, switchMap, tap } from 'rxjs/operators';
import { ApiService } from '../../../core/services/api.service';

/**
 * Progress Monitor Component
 * 
 * Real-time progress monitoring for anonymization:
 * - Progress bar with percentage
 * - Processed rows / total rows
 * - Estimated time remaining
 * - Current table being processed
 * - Cancel button with confirmation
 * 
 * Validates Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
@Component({
  selector: 'app-progress-monitor',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-2xl mx-auto">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Anonymization Progress</h1>
          <p class="mt-2 text-gray-600">Job ID: {{ jobId }}</p>
        </div>

        <!-- Status Card -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <!-- Status Badge -->
          <div class="mb-6">
            <span [class]="'inline-block px-3 py-1 rounded-full text-sm font-medium ' + getStatusClass()">
              {{ progress.status }}
            </span>
          </div>

          <!-- Progress Bar -->
          <div class="mb-6">
            <div class="flex justify-between items-center mb-2">
              <span class="text-sm font-medium text-gray-700">Overall Progress</span>
              <span class="text-sm font-bold text-gray-900">{{ progress.progress }}%</span>
            </div>
            <div class="w-full bg-gray-200 rounded-full h-3">
              <div class="bg-blue-500 h-3 rounded-full transition-all duration-300" 
                   [style.width.%]="progress.progress"></div>
            </div>
          </div>

          <!-- Statistics Grid -->
          <div class="grid grid-cols-2 gap-4 mb-6">
            <!-- Rows Processed -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Rows Processed</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatNumber(progress.rowsProcessed) }}</p>
              <p class="text-xs text-gray-500 mt-1">of {{ formatNumber(progress.totalRows) }}</p>
            </div>

            <!-- Processing Speed -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Processing Speed</p>
              <p class="text-2xl font-bold text-gray-900">{{ getProcessingSpeed() }}</p>
              <p class="text-xs text-gray-500 mt-1">rows/second</p>
            </div>

            <!-- Elapsed Time -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Elapsed Time</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatTime(elapsedSeconds) }}</p>
            </div>

            <!-- Estimated Time Remaining -->
            <div class="bg-gray-50 rounded-lg p-4">
              <p class="text-xs text-gray-600 mb-1">Time Remaining</p>
              <p class="text-2xl font-bold text-gray-900">{{ formatTime(progress.estimatedTimeRemaining) }}</p>
            </div>
          </div>

          <!-- Current Table -->
          <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
            <p class="text-sm text-blue-800">
              <strong>Current Table:</strong> {{ progress.currentTable || 'Initializing...' }}
            </p>
          </div>

          <!-- Error Display -->
          <div *ngIf="progress.status === 'FAILED' && progress.errorMessage" 
               class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <p class="text-sm text-red-800">
              <strong>Error:</strong> {{ progress.errorMessage }}
            </p>
          </div>

          <!-- Buttons -->
          <div class="flex gap-4">
            <button *ngIf="progress.status === 'IN_PROGRESS'" 
                    (click)="showCancelConfirmation = true"
                    class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700">
              Cancel Anonymization
            </button>
            <button *ngIf="progress.status === 'COMPLETED' || progress.status === 'FAILED' || progress.status === 'CANCELLED'"
                    (click)="goToResults()"
                    class="flex-1 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600">
              View Results
            </button>
            <button (click)="goToDashboard()"
                    class="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">
              Back to Dashboard
            </button>
          </div>
        </div>

        <!-- Detailed Statistics -->
        <div *ngIf="tableStats && tableStats.length > 0" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-lg font-bold text-gray-900 mb-4">Table Statistics</h2>
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
      </div>

      <!-- Cancel Confirmation Modal -->
      <div *ngIf="showCancelConfirmation" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg shadow-lg p-6 max-w-sm">
          <h2 class="text-lg font-bold text-gray-900 mb-4">Cancel Anonymization?</h2>
          <p class="text-gray-600 mb-6">
            Are you sure you want to cancel this anonymization job? The operation will be rolled back.
          </p>
          <div class="flex gap-4">
            <button (click)="showCancelConfirmation = false"
                    class="flex-1 px-4 py-2 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50">
              Keep Running
            </button>
            <button (click)="cancelAnonymization()"
                    [disabled]="isCancelling"
                    class="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400">
              {{ isCancelling ? 'Cancelling...' : 'Cancel Job' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ProgressMonitorComponent implements OnInit, OnDestroy {
  jobId: string = '';
  
  progress = {
    status: 'IN_PROGRESS',
    progress: 0,
    rowsProcessed: 0,
    totalRows: 0,
    currentTable: '',
    estimatedTimeRemaining: 0,
    errorMessage: ''
  };

  tableStats: any[] = [];
  elapsedSeconds = 0;
  
  showCancelConfirmation = false;
  isCancelling = false;
  
  private destroy$ = new Subject<void>();
  private startTime = Date.now();

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

    // Start polling for progress updates every 2 seconds
    interval(2000)
      .pipe(
        switchMap(() => this.apiService.get(`/anonymizations/${this.jobId}/progress`)),
        tap((response: any) => {
          this.updateProgress(response.data);
          this.elapsedSeconds = Math.floor((Date.now() - this.startTime) / 1000);
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        error: (err: any) => {
          console.error('Failed to fetch progress', err);
        }
      });

    // Initial load
    this.loadProgress();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadProgress(): void {
    this.apiService.get(`/anonymizations/${this.jobId}/progress`)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.updateProgress(response.data);
        },
        error: (err: any) => {
          console.error('Failed to load progress', err);
        }
      });
  }

  private updateProgress(data: any): void {
    this.progress = {
      status: data.status || 'IN_PROGRESS',
      progress: data.progress || 0,
      rowsProcessed: data.rowsProcessed || 0,
      totalRows: data.totalRows || 0,
      currentTable: data.currentTable || '',
      estimatedTimeRemaining: data.estimatedTimeRemaining || 0,
      errorMessage: data.errorMessage || ''
    };

    if (data.tableStats) {
      this.tableStats = data.tableStats;
    }

    // Stop polling if job is complete
    if (this.progress.status !== 'IN_PROGRESS') {
      this.destroy$.next();
    }
  }

  cancelAnonymization(): void {
    this.isCancelling = true;
    
    this.apiService.post(`/anonymizations/${this.jobId}/cancel`, {})
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.showCancelConfirmation = false;
          this.progress.status = 'CANCELLED';
        },
        error: (err: any) => {
          this.isCancelling = false;
          console.error('Failed to cancel anonymization', err);
        }
      });
  }

  goToResults(): void {
    this.router.navigate(['/anonymization', this.jobId, 'results']);
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }

  getStatusClass(): string {
    switch (this.progress.status) {
      case 'IN_PROGRESS':
        return 'bg-blue-100 text-blue-800';
      case 'COMPLETED':
        return 'bg-green-100 text-green-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'CANCELLED':
        return 'bg-yellow-100 text-yellow-800';
      default:
        return 'bg-gray-100 text-gray-800';
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

  getProcessingSpeed(): string {
    if (this.elapsedSeconds === 0 || this.progress.rowsProcessed === 0) {
      return '0';
    }
    const speed = this.progress.rowsProcessed / this.elapsedSeconds;
    return speed.toFixed(2);
  }

  formatNumber(num: number): string {
    return new Intl.NumberFormat('en-US').format(num);
  }

  formatTime(seconds: number): string {
    if (seconds === 0) return '0s';
    
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    const parts = [];
    if (hours > 0) parts.push(`${hours}h`);
    if (minutes > 0) parts.push(`${minutes}m`);
    if (secs > 0 || parts.length === 0) parts.push(`${secs}s`);

    return parts.join(' ');
  }
}
