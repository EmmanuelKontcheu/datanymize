import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-scan-results',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="container mx-auto px-4 py-8">
      <div class="mb-6">
        <button
          (click)="onBack()"
          class="text-blue-600 hover:text-blue-800 mb-4"
        >
          ← Back to Scans
        </button>
        <h1 class="text-3xl font-bold">PII Scan Results</h1>
      </div>

      <!-- Loading State -->
      <div *ngIf="isLoading" class="text-center py-8">
        <p class="text-gray-600">Loading scan results...</p>
      </div>

      <!-- Error State -->
      <div *ngIf="errorMessage && !isLoading" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <p class="text-red-800">{{ errorMessage }}</p>
      </div>

      <!-- Results -->
      <div *ngIf="!isLoading && !errorMessage" class="space-y-6">
        <!-- Summary -->
        <div class="bg-white rounded-lg shadow p-6">
          <h2 class="text-xl font-semibold mb-4">Summary</h2>
          <div class="grid grid-cols-3 gap-4">
            <div class="text-center">
              <p class="text-gray-600 text-sm">Total Columns</p>
              <p class="text-2xl font-bold">{{ totalColumns }}</p>
            </div>
            <div class="text-center">
              <p class="text-gray-600 text-sm">PII Columns</p>
              <p class="text-2xl font-bold text-red-600">{{ piiColumns }}</p>
            </div>
            <div class="text-center">
              <p class="text-gray-600 text-sm">Scan Time</p>
              <p class="text-lg font-semibold">{{ scanTime }}</p>
            </div>
          </div>
        </div>

        <!-- Classification Table -->
        <div class="bg-white rounded-lg shadow overflow-hidden">
          <div class="p-6 border-b">
            <h2 class="text-xl font-semibold">Classifications</h2>
          </div>
          
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead class="bg-gray-50 border-b">
                <tr>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-700">Table</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-700">Column</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-700">Data Type</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-700">Category</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-700">Confidence</th>
                  <th class="px-6 py-3 text-left text-sm font-semibold text-gray-700">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let classification of classifications" class="border-b hover:bg-gray-50">
                  <td class="px-6 py-4 text-sm text-gray-900">{{ classification.tableName }}</td>
                  <td class="px-6 py-4 text-sm text-gray-900">{{ classification.columnName }}</td>
                  <td class="px-6 py-4 text-sm text-gray-600">{{ classification.dataType }}</td>
                  <td class="px-6 py-4 text-sm">
                    <span class="px-2 py-1 rounded text-white text-xs font-semibold"
                          [ngClass]="getCategoryClass(classification.category)">
                      {{ classification.category }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-sm">
                    <div class="flex items-center gap-2">
                      <div class="w-16 bg-gray-200 rounded-full h-2">
                        <div
                          class="h-2 rounded-full"
                          [style.width.%]="classification.confidence"
                          [ngClass]="getConfidenceColor(classification.confidence)"
                        ></div>
                      </div>
                      <span class="text-xs font-semibold">{{ classification.confidence }}%</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 text-sm">
                    <button
                      (click)="onViewSamples(classification)"
                      class="text-blue-600 hover:text-blue-800 mr-3"
                    >
                      View
                    </button>
                    <button
                      (click)="onOverride(classification)"
                      class="text-orange-600 hover:text-orange-800"
                    >
                      Override
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex gap-4">
          <button
            (click)="onExport()"
            class="flex-1 bg-green-600 text-white py-2 px-4 rounded-md hover:bg-green-700"
          >
            Export Results
          </button>
          <button
            (click)="onProceedToConfiguration()"
            class="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700"
          >
            Proceed to Configuration
          </button>
        </div>
      </div>

      <!-- Sample Data Modal -->
      <div *ngIf="showSampleModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg shadow-lg max-w-2xl w-full mx-4 max-h-96 overflow-y-auto">
          <div class="p-6 border-b flex justify-between items-center">
            <h3 class="text-lg font-semibold">Sample Data: {{ selectedClassification?.columnName }}</h3>
            <button
              (click)="showSampleModal = false"
              class="text-gray-500 hover:text-gray-700"
            >
              ✕
            </button>
          </div>
          <div class="p-6">
            <div class="space-y-2">
              <div *ngFor="let sample of selectedSamples" class="p-2 bg-gray-50 rounded text-sm font-mono">
                {{ sample }}
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Override Modal -->
      <div *ngIf="showOverrideModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div class="bg-white rounded-lg shadow-lg max-w-md w-full mx-4">
          <div class="p-6 border-b flex justify-between items-center">
            <h3 class="text-lg font-semibold">Override Classification</h3>
            <button
              (click)="showOverrideModal = false"
              class="text-gray-500 hover:text-gray-700"
            >
              ✕
            </button>
          </div>
          <div class="p-6">
            <p class="text-sm text-gray-600 mb-4">
              Column: <strong>{{ selectedClassification?.columnName }}</strong>
            </p>
            <div class="mb-4">
              <label class="block text-sm font-medium text-gray-700 mb-2">
                New Category
              </label>
              <select
                [(ngModel)]="overrideCategory"
                class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="EMAIL">Email</option>
                <option value="PHONE">Phone</option>
                <option value="SSN">SSN</option>
                <option value="CREDIT_CARD">Credit Card</option>
                <option value="NAME">Name</option>
                <option value="ADDRESS">Address</option>
                <option value="IDENTIFIER">Identifier</option>
                <option value="FINANCIAL">Financial</option>
                <option value="MEDICAL">Medical</option>
                <option value="BIOMETRIC">Biometric</option>
                <option value="NONE">None</option>
              </select>
            </div>
            <div class="flex gap-3">
              <button
                (click)="showOverrideModal = false"
                class="flex-1 bg-gray-300 text-gray-800 py-2 px-4 rounded-md hover:bg-gray-400"
              >
                Cancel
              </button>
              <button
                (click)="onConfirmOverride()"
                class="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700"
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ScanResultsComponent implements OnInit, OnDestroy {
  scanId: string = '';
  classifications: any[] = [];
  isLoading = true;
  errorMessage = '';
  totalColumns = 0;
  piiColumns = 0;
  scanTime = '';
  
  showSampleModal = false;
  showOverrideModal = false;
  selectedClassification: any = null;
  selectedSamples: string[] = [];
  overrideCategory = '';
  
  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit(): void {
    this.route.params.pipe(takeUntil(this.destroy$)).subscribe(params => {
      this.scanId = params['id'];
      this.loadScanResults();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadScanResults(): void {
    this.apiService.getPiiScanResults(this.scanId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          const data = response.data;
          this.classifications = data.classifications || [];
          this.totalColumns = this.classifications.length;
          this.piiColumns = this.classifications.filter(c => c.category !== 'NONE').length;
          this.scanTime = new Date(data.scanTime).toLocaleString();
          this.isLoading = false;
        },
        error: (error) => {
          this.errorMessage = error.error?.message || 'Failed to load scan results';
          this.isLoading = false;
        }
      });
  }

  getCategoryClass(category: string): string {
    const classes: { [key: string]: string } = {
      'EMAIL': 'bg-red-600',
      'PHONE': 'bg-red-600',
      'SSN': 'bg-red-600',
      'CREDIT_CARD': 'bg-red-600',
      'NAME': 'bg-orange-600',
      'ADDRESS': 'bg-orange-600',
      'IDENTIFIER': 'bg-yellow-600',
      'FINANCIAL': 'bg-red-600',
      'MEDICAL': 'bg-red-600',
      'BIOMETRIC': 'bg-red-600',
      'NONE': 'bg-green-600'
    };
    return classes[category] || 'bg-gray-600';
  }

  getConfidenceColor(confidence: number): string {
    if (confidence >= 80) return 'bg-green-600';
    if (confidence >= 60) return 'bg-yellow-600';
    return 'bg-red-600';
  }

  onViewSamples(classification: any): void {
    this.selectedClassification = classification;
    this.selectedSamples = classification.evidence || [];
    this.showSampleModal = true;
  }

  onOverride(classification: any): void {
    this.selectedClassification = classification;
    this.overrideCategory = classification.category;
    this.showOverrideModal = true;
  }

  onConfirmOverride(): void {
    if (this.selectedClassification && this.overrideCategory) {
      this.apiService.overridePiiClassification(this.scanId, {
        tableName: this.selectedClassification.tableName,
        columnName: this.selectedClassification.columnName,
        newCategory: this.overrideCategory
      }).pipe(takeUntil(this.destroy$)).subscribe({
        next: () => {
          this.selectedClassification.category = this.overrideCategory;
          this.showOverrideModal = false;
        },
        error: (error) => {
          console.error('Failed to override classification:', error);
        }
      });
    }
  }

  onViewSamples2(classification: any): void {
    this.onViewSamples(classification);
  }

  onExport(): void {
    // Export functionality can be implemented later
    console.log('Export results');
  }

  onProceedToConfiguration(): void {
    this.router.navigate(['/configuration']);
  }

  onBack(): void {
    this.router.navigate(['/pii-scan']);
  }
}
