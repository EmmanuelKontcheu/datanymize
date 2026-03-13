import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-config-editor',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="container mx-auto px-4 py-8">
      <div class="grid grid-cols-3 gap-6">
        <!-- Main Editor -->
        <div class="col-span-2">
          <h1 class="text-3xl font-bold mb-6">Configuration Editor</h1>

          <!-- Format Selector -->
          <div class="bg-white rounded-lg shadow p-6 mb-6">
            <div class="flex gap-4 mb-4">
              <label class="flex items-center">
                <input
                  type="radio"
                  [(ngModel)]="configFormat"
                  value="yaml"
                  class="mr-2"
                />
                <span>YAML</span>
              </label>
              <label class="flex items-center">
                <input
                  type="radio"
                  [(ngModel)]="configFormat"
                  value="json"
                  class="mr-2"
                />
                <span>JSON</span>
              </label>
            </div>
          </div>

          <!-- Editor -->
          <div class="bg-white rounded-lg shadow p-6 mb-6">
            <textarea
              [(ngModel)]="configContent"
              class="w-full h-96 p-4 border border-gray-300 rounded-md font-mono text-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              placeholder="Enter your configuration here..."
            ></textarea>
          </div>

          <!-- Validation Errors -->
          <div *ngIf="validationErrors.length > 0" class="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <h3 class="font-semibold text-red-800 mb-2">Validation Errors:</h3>
            <ul class="list-disc list-inside space-y-1">
              <li *ngFor="let error of validationErrors" class="text-red-700 text-sm">
                {{ error }}
              </li>
            </ul>
          </div>

          <!-- Action Buttons -->
          <div class="flex gap-4">
            <button
              (click)="onValidate()"
              class="flex-1 bg-yellow-600 text-white py-2 px-4 rounded-md hover:bg-yellow-700"
            >
              Validate
            </button>
            <button
              (click)="onSave()"
              [disabled]="validationErrors.length > 0"
              class="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
            >
              Save Configuration
            </button>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="col-span-1">
          <!-- Preview -->
          <div class="bg-white rounded-lg shadow p-6 mb-6">
            <h2 class="text-lg font-semibold mb-4">Preview</h2>
            <div class="bg-gray-50 p-4 rounded text-sm font-mono max-h-64 overflow-y-auto">
              <pre>{{ configContent }}</pre>
            </div>
          </div>

          <!-- Version History -->
          <div class="bg-white rounded-lg shadow p-6">
            <h2 class="text-lg font-semibold mb-4">Version History</h2>
            <div class="space-y-2">
              <button
                *ngFor="let version of versions"
                (click)="onSelectVersion(version)"
                class="w-full text-left p-2 hover:bg-gray-100 rounded text-sm"
              >
                <div class="font-semibold">v{{ version.version }}</div>
                <div class="text-gray-600 text-xs">{{ version.createdAt | date:'short' }}</div>
              </button>
            </div>
            <button
              (click)="onViewHistory()"
              class="w-full mt-4 text-blue-600 hover:text-blue-800 text-sm font-semibold"
            >
              View Full History
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class ConfigEditorComponent implements OnInit, OnDestroy {
  configContent = '';
  configFormat: 'yaml' | 'json' = 'yaml';
  validationErrors: string[] = [];
  versions: any[] = [];
  configId: string | null = null;
  
  private destroy$ = new Subject<void>();

  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadVersionHistory();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadVersionHistory(): void {
    // Load version history if configId exists
    // For now, we'll just initialize with empty versions
    this.versions = [];
  }

  onValidate(): void {
    this.validationErrors = [];
    
    try {
      if (this.configFormat === 'json') {
        JSON.parse(this.configContent);
      } else {
        // Basic YAML validation - just check for common issues
        if (!this.configContent.trim()) {
          this.validationErrors.push('Configuration cannot be empty');
        }
      }
      
      if (this.validationErrors.length === 0) {
        // Additional validation logic can be added here
        console.log('Configuration is valid');
      }
    } catch (error: any) {
      this.validationErrors.push(error.message || 'Invalid configuration format');
    }
  }

  onSave(): void {
    this.onValidate();
    
    if (this.validationErrors.length > 0) {
      return;
    }

    const configData = {
      content: this.configContent,
      format: this.configFormat
    };

    if (this.configId) {
      this.apiService.updateConfiguration(this.configId, configData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            console.log('Configuration saved successfully');
            this.configId = response.data.id;
            this.loadVersionHistory();
          },
          error: (error) => {
            this.validationErrors.push(error.error?.message || 'Failed to save configuration');
          }
        });
    } else {
      this.apiService.createConfiguration(configData)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: (response) => {
            console.log('Configuration created successfully');
            this.configId = response.data.id;
            this.loadVersionHistory();
          },
          error: (error) => {
            this.validationErrors.push(error.error?.message || 'Failed to create configuration');
          }
        });
    }
  }

  onSelectVersion(version: any): void {
    this.configContent = version.content;
    this.configFormat = version.format;
  }

  onViewHistory(): void {
    if (this.configId) {
      this.router.navigate(['/configuration', this.configId, 'history']);
    }
  }
}
