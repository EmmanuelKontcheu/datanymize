import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-config-history',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="container mx-auto px-4 py-8">
      <div class="mb-6">
        <button
          (click)="onBack()"
          class="text-blue-600 hover:text-blue-800 mb-4"
        >
          ← Back to Configuration
        </button>
        <h1 class="text-3xl font-bold">Configuration Version History</h1>
      </div>

      <!-- Loading State -->
      <div *ngIf="isLoading" class="text-center py-8">
        <p class="text-gray-600">Loading version history...</p>
      </div>

      <!-- Error State -->
      <div *ngIf="errorMessage && !isLoading" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <p class="text-red-800">{{ errorMessage }}</p>
      </div>

      <!-- Version History -->
      <div *ngIf="!isLoading && !errorMessage" class="grid grid-cols-3 gap-6">
        <!-- Version List -->
        <div class="col-span-1">
          <div class="bg-white rounded-lg shadow overflow-hidden">
            <div class="p-6 border-b">
              <h2 class="text-lg font-semibold">Versions</h2>
            </div>
            <div class="divide-y max-h-96 overflow-y-auto">
              <button
                *ngFor="let version of versions"
                (click)="onSelectVersion(version)"
                [class.bg-blue-50]="selectedVersion?.version === version.version"
                class="w-full text-left p-4 hover:bg-gray-50 transition"
              >
                <div class="font-semibold">v{{ version.version }}</div>
                <div class="text-gray-600 text-xs">{{ version.createdAt | date:'short' }}</div>
                <div class="text-gray-500 text-xs mt-1">{{ version.changes }}</div>
              </button>
            </div>
          </div>
        </div>

        <!-- Version Details -->
        <div class="col-span-2">
          <div *ngIf="selectedVersion" class="bg-white rounded-lg shadow p-6">
            <div class="mb-6">
              <h2 class="text-xl font-semibold mb-2">Version {{ selectedVersion.version }}</h2>
              <p class="text-gray-600">{{ selectedVersion.createdAt | date:'medium' }}</p>
            </div>

            <!-- Content Preview -->
            <div class="mb-6">
              <h3 class="text-lg font-semibold mb-2">Content</h3>
              <div class="bg-gray-50 p-4 rounded text-sm font-mono max-h-64 overflow-y-auto">
                <pre>{{ selectedVersion.content }}</pre>
              </div>
            </div>

            <!-- Action Buttons -->
            <div class="flex gap-4">
              <button
                (click)="onRestore()"
                class="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700"
              >
                Restore This Version
              </button>
              <button
                (click)="onCompare()"
                class="flex-1 bg-gray-600 text-white py-2 px-4 rounded-md hover:bg-gray-700"
              >
                Compare with Current
              </button>
            </div>
          </div>

          <!-- No Version Selected -->
          <div *ngIf="!selectedVersion" class="bg-white rounded-lg shadow p-6 text-center">
            <p class="text-gray-600">Select a version to view details</p>
          </div>
        </div>
      </div>

      <!-- Comparison Modal -->
      <div *ngIf="showComparisonModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg shadow-lg max-w-4xl w-full mx-4 max-h-96 overflow-y-auto">
          <div class="p-6 border-b flex justify-between items-center">
            <h3 class="text-lg font-semibold">Version Comparison</h3>
            <button
              (click)="showComparisonModal = false"
              class="text-gray-500 hover:text-gray-700"
            >
              ✕
            </button>
          </div>
          <div class="p-6">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <h4 class="font-semibold mb-2">Version {{ selectedVersion?.version }}</h4>
                <div class="bg-gray-50 p-4 rounded text-sm font-mono max-h-64 overflow-y-auto">
                  <pre>{{ selectedVersion?.content }}</pre>
                </div>
              </div>
              <div>
                <h4 class="font-semibold mb-2">Current Version</h4>
                <div class="bg-gray-50 p-4 rounded text-sm font-mono max-h-64 overflow-y-auto">
                  <pre>{{ currentContent }}</pre>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ConfigHistoryComponent implements OnInit, OnDestroy {
  configId: string = '';
  versions: any[] = [];
  selectedVersion: any = null;
  currentContent = '';
  isLoading = true;
  errorMessage = '';
  showComparisonModal = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.configId = params['id'];
      this.loadVersionHistory();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadVersionHistory(): void {
    this.apiService.getConfigurationVersions(this.configId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.versions = response.data || [];
          if (this.versions.length > 0) {
            this.selectedVersion = this.versions[0];
          }
          this.isLoading = false;
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Failed to load version history';
          this.isLoading = false;
        }
      });
  }

  onSelectVersion(version: any): void {
    this.selectedVersion = version;
  }

  onRestore(): void {
    if (this.selectedVersion) {
      this.apiService.restoreConfigurationVersion(this.configId, this.selectedVersion.version)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            console.log('Configuration restored successfully');
            this.router.navigate(['/configuration']);
          },
          error: (error) => {
            this.errorMessage = error.error?.message || 'Failed to restore configuration';
          }
        });
    }
  }

  onCompare(): void {
    this.apiService.getConfiguration(this.configId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.currentContent = response.data.content;
          this.showComparisonModal = true;
        },
        error: (error) => {
          console.error('Failed to load current configuration:', error);
        }
      });
  }

  onBack(): void {
    this.router.navigate(['/configuration']);
  }
}
