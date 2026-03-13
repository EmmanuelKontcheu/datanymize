import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';

/**
 * Connection form component for creating and editing database connections.
 * 
 * Features:
 * - Form with host, port, username, password fields
 * - Database type selector (PostgreSQL, MySQL, MongoDB)
 * - TLS/SSL configuration options
 * - Form validation with error messages
 * - Create and edit modes
 * 
 * Validates Requirements: 8.2, 1.1, 1.2, 1.3
 */
@Component({
  selector: 'app-connection-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="min-h-screen bg-gray-50">
      <!-- Header -->
      <header class="bg-white shadow">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div class="flex items-center">
            <a routerLink="/connections" class="text-blue-600 hover:text-blue-800 mr-4">← Back</a>
            <h1 class="text-3xl font-bold text-gray-900">
              {{ isEditMode ? 'Edit Connection' : 'New Connection' }}
            </h1>
          </div>
        </div>
      </header>

      <!-- Main Content -->
      <main class="max-w-2xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <!-- Error Alert -->
        <div *ngIf="error" class="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
          <p class="text-red-800">{{ error }}</p>
        </div>

        <!-- Form Card -->
        <div class="bg-white rounded-lg shadow p-8">
          <form [formGroup]="connectionForm" (ngSubmit)="onSubmit()">
            <!-- Database Type -->
            <div class="mb-6">
              <label for="databaseType" class="block text-sm font-medium text-gray-700 mb-2">
                Database Type <span class="text-red-600">*</span>
              </label>
              <select
                id="databaseType"
                formControlName="databaseType"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                [disabled]="isEditMode || loading"
              >
                <option value="">Select a database type</option>
                <option value="postgresql">PostgreSQL</option>
                <option value="mysql">MySQL</option>
                <option value="mongodb">MongoDB</option>
              </select>
              <p *ngIf="isFieldInvalid('databaseType')" class="mt-1 text-sm text-red-600">
                Database type is required
              </p>
            </div>

            <!-- Connection Name -->
            <div class="mb-6">
              <label for="database" class="block text-sm font-medium text-gray-700 mb-2">
                Database Name <span class="text-red-600">*</span>
              </label>
              <input
                id="database"
                type="text"
                formControlName="database"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                placeholder="e.g., production_db"
                [disabled]="loading"
              />
              <p *ngIf="isFieldInvalid('database')" class="mt-1 text-sm text-red-600">
                Database name is required
              </p>
            </div>

            <!-- Host -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label for="host" class="block text-sm font-medium text-gray-700 mb-2">
                  Host <span class="text-red-600">*</span>
                </label>
                <input
                  id="host"
                  type="text"
                  formControlName="host"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  placeholder="e.g., localhost or db.example.com"
                  [disabled]="loading"
                />
                <p *ngIf="isFieldInvalid('host')" class="mt-1 text-sm text-red-600">
                  Host is required
                </p>
              </div>

              <div>
                <label for="port" class="block text-sm font-medium text-gray-700 mb-2">
                  Port <span class="text-red-600">*</span>
                </label>
                <input
                  id="port"
                  type="number"
                  formControlName="port"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  placeholder="e.g., 5432"
                  [disabled]="loading"
                />
                <p *ngIf="isFieldInvalid('port')" class="mt-1 text-sm text-red-600">
                  Port is required and must be a number
                </p>
              </div>
            </div>

            <!-- Username and Password -->
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
              <div>
                <label for="username" class="block text-sm font-medium text-gray-700 mb-2">
                  Username <span class="text-red-600">*</span>
                </label>
                <input
                  id="username"
                  type="text"
                  formControlName="username"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  placeholder="e.g., postgres"
                  [disabled]="loading"
                />
                <p *ngIf="isFieldInvalid('username')" class="mt-1 text-sm text-red-600">
                  Username is required
                </p>
              </div>

              <div>
                <label for="password" class="block text-sm font-medium text-gray-700 mb-2">
                  Password <span class="text-red-600">*</span>
                </label>
                <input
                  id="password"
                  type="password"
                  formControlName="password"
                  class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none"
                  placeholder="Enter password"
                  [disabled]="loading"
                />
                <p *ngIf="isFieldInvalid('password')" class="mt-1 text-sm text-red-600">
                  Password is required
                </p>
              </div>
            </div>

            <!-- TLS/SSL Options -->
            <div class="mb-6 p-4 bg-gray-50 rounded-lg border border-gray-200">
              <h3 class="text-sm font-semibold text-gray-900 mb-4">Security Options</h3>
              
              <div class="mb-4">
                <label class="flex items-center">
                  <input
                    type="checkbox"
                    formControlName="useTLS"
                    class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    [disabled]="loading"
                  />
                  <span class="ml-2 text-sm text-gray-700">Use TLS/SSL Encryption</span>
                </label>
                <p class="mt-1 ml-6 text-xs text-gray-600">
                  Recommended for production connections
                </p>
              </div>

              <div *ngIf="connectionForm.get('useTLS')?.value">
                <label class="flex items-center">
                  <input
                    type="checkbox"
                    formControlName="verifyCertificate"
                    class="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    [disabled]="loading"
                  />
                  <span class="ml-2 text-sm text-gray-700">Verify SSL Certificate</span>
                </label>
                <p class="mt-1 ml-6 text-xs text-gray-600">
                  Uncheck to allow self-signed certificates
                </p>
              </div>
            </div>

            <!-- Form Actions -->
            <div class="flex gap-4">
              <button
                type="submit"
                [disabled]="loading || connectionForm.invalid"
                class="flex-1 bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
              >
                <span *ngIf="!loading">{{ isEditMode ? 'Update Connection' : 'Create Connection' }}</span>
                <span *ngIf="loading" class="flex items-center justify-center">
                  <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Saving...
                </span>
              </button>
              <a routerLink="/connections" class="flex-1 bg-gray-200 text-gray-900 py-2 rounded-lg font-medium hover:bg-gray-300 transition text-center">
                Cancel
              </a>
            </div>
          </form>
        </div>

        <!-- Info Box -->
        <div class="mt-6 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <h3 class="text-sm font-semibold text-blue-900 mb-2">Connection Requirements</h3>
          <ul class="text-sm text-blue-800 space-y-1">
            <li>• Connection must have READ-ONLY access to the source database</li>
            <li>• TLS/SSL encryption is recommended for all connections</li>
            <li>• Connection will be tested before saving</li>
            <li>• Passwords are encrypted and never stored in plain text</li>
          </ul>
        </div>
      </main>
    </div>
  `,
  styles: []
})
export class ConnectionFormComponent implements OnInit {
  connectionForm!: FormGroup;
  loading = false;
  error = '';
  isEditMode = false;
  connectionId: string | null = null;

  constructor(
    private formBuilder: FormBuilder,
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.checkEditMode();
  }

  initializeForm(): void {
    this.connectionForm = this.formBuilder.group({
      databaseType: ['', Validators.required],
      database: ['', Validators.required],
      host: ['', Validators.required],
      port: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
      username: ['', Validators.required],
      password: ['', Validators.required],
      useTLS: [true],
      verifyCertificate: [true]
    });
  }

  checkEditMode(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.connectionId = params['id'];
        this.loadConnection();
      }
    });
  }

  loadConnection(): void {
    if (!this.connectionId) return;

    this.loading = true;
    this.apiService.getConnection(this.connectionId).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.success) {
          const conn = response.data;
          this.connectionForm.patchValue({
            databaseType: conn.databaseType,
            database: conn.database,
            host: conn.host,
            port: conn.port,
            username: conn.username,
            useTLS: conn.useTLS,
            verifyCertificate: conn.verifyCertificate
          });
          // Disable database type in edit mode
          this.connectionForm.get('databaseType')?.disable();
        }
      },
      error: (error) => {
        this.loading = false;
        this.error = error.error?.message || 'Failed to load connection';
      }
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.connectionForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onSubmit(): void {
    if (this.connectionForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = '';

    const formValue = this.connectionForm.getRawValue();

    const request = this.isEditMode && this.connectionId
      ? this.apiService.updateConnection(this.connectionId, formValue)
      : this.apiService.createConnection(formValue);

    request.subscribe({
      next: () => {
        this.router.navigate(['/connections']);
      },
      error: (error) => {
        this.loading = false;
        this.error = error.error?.message || 'Failed to save connection';
      }
    });
  }

  updateConnection(id: string, data: any): any {
    // This would be added to ApiService
    return this.apiService.createConnection(data);
  }
}
