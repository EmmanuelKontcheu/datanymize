import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil, interval } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-scan-initiation',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="container mx-auto px-4 py-8">
      <div class="max-w-2xl mx-auto">
        <h1 class="text-3xl font-bold mb-6">PII Scan</h1>

        <!-- Connection Selection -->
        <div class="bg-white rounded-lg shadow p-6 mb-6">
          <h2 class="text-xl font-semibold mb-4">Select Database Connection</h2>
          <form [formGroup]="scanForm" (ngSubmit)="onStartScan()">
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                Connection
              </label>
              <select
                formControlName="connectionId"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="">Select a connection...</option>
                <option *ngFor="let conn of connections" [value]="conn.id">
                  {{ conn.host }}:{{ conn.port }} ({{ conn.database }})
                </option>
              </select>
              <p *ngIf="scanForm.get('connectionId')?.hasError('required') && scanForm.get('connectionId')?.touched"
                 class="text-red-500 text-sm mt-1">
                Connection is required
              </p>
            </div>

            <button
              type="submit"
              [disabled]="scanForm.invalid || isScanning"
              class="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              {{ isScanning ? 'Scanning...' : 'Start PII Scan' }}
            </button>
          </form>
        </div>

        <!-- Scan Progress -->
        <div *ngIf="isScanning && currentScanId" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-xl font-semibold mb-4">Scan Progress</h2>
          
          <div class="mb-4">
            <div class="flex justify-between mb-2">
              <span class="text-sm font-medium text-gray-700">Progress</span>
              <span class="text-sm font-medium text-gray-700">{{ scanProgress }}%</span>
            </div>
            <div class="w-full bg-gray-200 rounded-full h-2">
              <div
                class="bg-blue-600 h-2 rounded-full transition-all duration-300"
                [style.width.%]="scanProgress"
              ></div>
            </div>
          </div>

          <div class="text-center">
            <p class="text-gray-600 mb-4">Scanning database for PII...</p>
            <button
              (click)="onCancelScan()"
              class="bg-red-600 text-white py-2 px-4 rounded-md hover:bg-red-700"
            >
              Cancel Scan
            </button>
          </div>
        </div>

        <!-- Error Message -->
        <div *ngIf="errorMessage" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <p class="text-red-800">{{ errorMessage }}</p>
        </div>

        <!-- Success Message -->
        <div *ngIf="successMessage" class="bg-green-50 border border-green-200 rounded-lg p-4 mb-6">
          <p class="text-green-800">{{ successMessage }}</p>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ScanInitiationComponent implements OnInit, OnDestroy {
  scanForm!: FormGroup;
  connections: any[] = [];
  isScanning = false;
  currentScanId: string | null = null;
  scanProgress = 0;
  errorMessage = '';
  successMessage = '';
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private router: Router
  ) {
    this.scanForm = this.fb.group({
      connectionId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadConnections();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadConnections(): void {
    this.apiService.listConnections()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.connections = response.data || [];
        },
        error: (error) => {
          this.errorMessage = 'Failed to load connections';
          console.error(error);
        }
      });
  }

  onStartScan(): void {
    if (this.scanForm.invalid) return;

    const connectionId = this.scanForm.get('connectionId')?.value;
    this.isScanning = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.scanProgress = 0;

    this.apiService.startPiiScan(connectionId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.currentScanId = response.data.id;
          this.monitorScanProgress();
        },
        error: (error) => {
          this.isScanning = false;
          this.errorMessage = error.error?.message || 'Failed to start PII scan';
          console.error(error);
        }
      });
  }

  monitorScanProgress(): void {
    interval(2000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.currentScanId) {
          this.apiService.getPiiScanStatus(this.currentScanId)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: (response) => {
                const status = response.data;
                this.scanProgress = status.progress || 0;

                if (status.status === 'COMPLETED') {
                  this.isScanning = false;
                  this.successMessage = 'PII scan completed successfully!';
                  setTimeout(() => {
                    this.router.navigate(['/pii-scan', this.currentScanId, 'results']);
                  }, 1500);
                } else if (status.status === 'FAILED') {
                  this.isScanning = false;
                  this.errorMessage = status.errorMessage || 'PII scan failed';
                }
              },
              error: (error) => {
                console.error('Error monitoring scan progress:', error);
              }
            });
        }
      });
  }

  onCancelScan(): void {
    if (this.currentScanId) {
      // Cancel scan via API if available
      this.isScanning = false;
      this.currentScanId = null;
      this.scanProgress = 0;
    }
  }
}
