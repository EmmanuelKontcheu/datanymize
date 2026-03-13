import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ApiService } from '../../../core/services/api.service';

/**
 * Anonymization Wizard Component
 * 
 * Multi-step wizard for anonymization:
 * Step 1: Select source and target databases
 * Step 2: Select configuration
 * Step 3: Review and confirm
 * 
 * Validates Requirements: 11.1, 11.2, 11.3, 11.4, 11.5
 */
@Component({
  selector: 'app-anonymization-wizard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="min-h-screen bg-gray-50 py-8 px-4">
      <div class="max-w-2xl mx-auto">
        <!-- Header -->
        <div class="mb-8">
          <h1 class="text-3xl font-bold text-gray-900">Anonymization Wizard</h1>
          <p class="mt-2 text-gray-600">Configure and execute data anonymization</p>
        </div>

        <!-- Progress Indicator -->
        <div class="mb-8">
          <div class="flex items-center justify-between">
            <div *ngFor="let step of steps; let i = index" class="flex items-center">
              <div [class]="'w-10 h-10 rounded-full flex items-center justify-center font-bold ' + 
                   (currentStep > i ? 'bg-green-500 text-white' : 
                    currentStep === i ? 'bg-blue-500 text-white' : 'bg-gray-300 text-gray-600')">
                {{ i + 1 }}
              </div>
              <span class="ml-2 text-sm font-medium text-gray-700">{{ step }}</span>
              <div *ngIf="i < steps.length - 1" class="w-12 h-1 mx-4" 
                   [class.bg-green-500]="currentStep > i" 
                   [class.bg-gray-300]="currentStep <= i"></div>
            </div>
          </div>
        </div>

        <!-- Step 1: Select Databases -->
        <div *ngIf="currentStep === 0" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-xl font-bold text-gray-900 mb-6">Select Source and Target Databases</h2>
          
          <form [formGroup]="step1Form" class="space-y-6">
            <!-- Source Connection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Source Database</label>
              <select formControlName="sourceConnectionId" 
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                <option value="">Select source database...</option>
                <option *ngFor="let conn of connections" [value]="conn.id">
                  {{ conn.name }} ({{ conn.type }})
                </option>
              </select>
              <p *ngIf="step1Form.get('sourceConnectionId')?.hasError('required') && step1Form.get('sourceConnectionId')?.touched" 
                 class="mt-1 text-sm text-red-600">Source database is required</p>
            </div>

            <!-- Target Connection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Target Database</label>
              <select formControlName="targetConnectionId" 
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                <option value="">Select target database...</option>
                <option *ngFor="let conn of connections" [value]="conn.id">
                  {{ conn.name }} ({{ conn.type }})
                </option>
              </select>
              <p *ngIf="step1Form.get('targetConnectionId')?.hasError('required') && step1Form.get('targetConnectionId')?.touched" 
                 class="mt-1 text-sm text-red-600">Target database is required</p>
            </div>

            <!-- Warning -->
            <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
              <p class="text-sm text-yellow-800">
                <strong>Note:</strong> The target database will be overwritten with anonymized data. Ensure you have a backup.
              </p>
            </div>
          </form>

          <!-- Buttons -->
          <div class="mt-8 flex justify-between">
            <button (click)="cancel()" 
                    class="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
              Cancel
            </button>
            <button (click)="nextStep()" 
                    [disabled]="!step1Form.valid"
                    class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-400">
              Next
            </button>
          </div>
        </div>

        <!-- Step 2: Select Configuration -->
        <div *ngIf="currentStep === 1" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-xl font-bold text-gray-900 mb-6">Select Configuration</h2>
          
          <form [formGroup]="step2Form" class="space-y-6">
            <!-- Configuration Selection -->
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-2">Anonymization Configuration</label>
              <select formControlName="configurationId" 
                      class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                <option value="">Select configuration...</option>
                <option *ngFor="let config of configurations" [value]="config.id">
                  {{ config.name }} (v{{ config.version }})
                </option>
              </select>
              <p *ngIf="step2Form.get('configurationId')?.hasError('required') && step2Form.get('configurationId')?.touched" 
                 class="mt-1 text-sm text-red-600">Configuration is required</p>
            </div>

            <!-- Configuration Preview -->
            <div *ngIf="selectedConfig" class="bg-gray-50 rounded-lg p-4">
              <h3 class="font-medium text-gray-900 mb-3">Configuration Preview</h3>
              <pre class="text-xs text-gray-700 overflow-auto max-h-48">{{ selectedConfig | json }}</pre>
            </div>
          </form>

          <!-- Buttons -->
          <div class="mt-8 flex justify-between">
            <button (click)="previousStep()" 
                    class="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
              Back
            </button>
            <button (click)="nextStep()" 
                    [disabled]="!step2Form.valid"
                    class="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:bg-gray-400">
              Next
            </button>
          </div>
        </div>

        <!-- Step 3: Review and Confirm -->
        <div *ngIf="currentStep === 2" class="bg-white rounded-lg shadow p-6">
          <h2 class="text-xl font-bold text-gray-900 mb-6">Review and Confirm</h2>
          
          <div class="space-y-6">
            <!-- Source Database -->
            <div class="border-b pb-4">
              <h3 class="font-medium text-gray-900 mb-2">Source Database</h3>
              <p class="text-gray-600">{{ getConnectionName(step1Form.get('sourceConnectionId')?.value) }}</p>
            </div>

            <!-- Target Database -->
            <div class="border-b pb-4">
              <h3 class="font-medium text-gray-900 mb-2">Target Database</h3>
              <p class="text-gray-600">{{ getConnectionName(step1Form.get('targetConnectionId')?.value) }}</p>
            </div>

            <!-- Configuration -->
            <div class="border-b pb-4">
              <h3 class="font-medium text-gray-900 mb-2">Configuration</h3>
              <p class="text-gray-600">{{ getConfigurationName(step2Form.get('configurationId')?.value) }}</p>
            </div>

            <!-- Warning -->
            <div class="bg-red-50 border border-red-200 rounded-lg p-4">
              <p class="text-sm text-red-800">
                <strong>Warning:</strong> This operation will overwrite the target database with anonymized data. This action cannot be undone.
              </p>
            </div>

            <!-- Confirmation Checkbox -->
            <div class="flex items-center">
              <input type="checkbox" formControlName="confirmed" 
                     class="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500">
              <label class="ml-2 text-sm text-gray-700">
                I understand and confirm this operation
              </label>
            </div>
          </div>

          <!-- Buttons -->
          <div class="mt-8 flex justify-between">
            <button (click)="previousStep()" 
                    class="px-6 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50">
              Back
            </button>
            <button (click)="startAnonymization()" 
                    [disabled]="!step3Form.valid || isStarting"
                    class="px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:bg-gray-400">
              {{ isStarting ? 'Starting...' : 'Start Anonymization' }}
            </button>
          </div>
        </div>

        <!-- Error Alert -->
        <div *ngIf="error" class="mt-6 bg-red-50 border border-red-200 rounded-lg p-4">
          <p class="text-sm text-red-800">{{ error }}</p>
          <button (click)="error = null" class="mt-2 text-sm text-red-600 hover:text-red-800">Dismiss</button>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class AnonymizationWizardComponent implements OnInit, OnDestroy {
  currentStep = 0;
  steps = ['Select Databases', 'Select Configuration', 'Review & Confirm'];
  
  step1Form!: FormGroup;
  step2Form!: FormGroup;
  step3Form!: FormGroup;
  
  connections: any[] = [];
  configurations: any[] = [];
  selectedConfig: any = null;
  
  isStarting = false;
  error: string | null = null;
  
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private router: Router
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadConnections();
    this.loadConfigurations();
    this.setupConfigurationWatch();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeForms(): void {
    this.step1Form = this.fb.group({
      sourceConnectionId: ['', Validators.required],
      targetConnectionId: ['', Validators.required]
    });

    this.step2Form = this.fb.group({
      configurationId: ['', Validators.required]
    });

    this.step3Form = this.fb.group({
      confirmed: [false, Validators.requiredTrue]
    });
  }

  private loadConnections(): void {
    this.apiService.get('/connections')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.connections = response.data || [];
        },
        error: (err: any) => {
          this.error = 'Failed to load connections';
          console.error(err);
        }
      });
  }

  private loadConfigurations(): void {
    this.apiService.get('/configurations')
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response: any) => {
          this.configurations = response.data || [];
        },
        error: (err: any) => {
          this.error = 'Failed to load configurations';
          console.error(err);
        }
      });
  }

  private setupConfigurationWatch(): void {
    this.step2Form.get('configurationId')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(configId => {
        this.selectedConfig = this.configurations.find(c => c.id === configId);
      });
  }

  nextStep(): void {
    if (this.currentStep < this.steps.length - 1) {
      this.currentStep++;
    }
  }

  previousStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  startAnonymization(): void {
    if (!this.step1Form.valid || !this.step2Form.valid || !this.step3Form.valid) {
      return;
    }

    this.isStarting = true;
    this.error = null;

    const request = {
      sourceConnectionId: this.step1Form.get('sourceConnectionId')?.value,
      targetConnectionId: this.step1Form.get('targetConnectionId')?.value,
      configurationId: this.step2Form.get('configurationId')?.value
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
        error: (err: any) => {
          this.isStarting = false;
          this.error = err.error?.message || 'Failed to start anonymization';
          console.error(err);
        }
      });
  }

  cancel(): void {
    this.router.navigate(['/dashboard']);
  }

  getConnectionName(connectionId: string): string {
    const conn = this.connections.find(c => c.id === connectionId);
    return conn ? `${conn.name} (${conn.type})` : 'Unknown';
  }

  getConfigurationName(configId: string): string {
    const config = this.configurations.find(c => c.id === configId);
    return config ? `${config.name} (v${config.version})` : 'Unknown';
  }
}
