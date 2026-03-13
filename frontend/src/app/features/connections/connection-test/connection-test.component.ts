import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';

/**
 * Connection test component for testing database connections.
 * 
 * Features:
 * - Test button with loading state
 * - Display test results (success/failure)
 * - Show connection details on success
 * - Display error message on failure
 * - Real-time feedback
 * 
 * Validates Requirements: 8.3, 1.4, 1.5, 1.6
 */
@Component({
  selector: 'app-connection-test',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-white rounded-lg shadow p-6">
      <h3 class="text-lg font-semibold text-gray-900 mb-4">Test Connection</h3>

      <!-- Test Button -->
      <button
        (click)="testConnection()"
        [disabled]="loading || !connectionId"
        class="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition mb-4"
      >
        <span *ngIf="!loading">Test Connection</span>
        <span *ngIf="loading" class="flex items-center">
          <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          Testing...
        </span>
      </button>

      <!-- Success Result -->
      <div *ngIf="testResult && testResult.success" class="mt-4 p-4 bg-green-50 border border-green-200 rounded-lg">
        <div class="flex items-start">
          <svg class="h-6 w-6 text-green-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <div class="ml-3">
            <h4 class="text-sm font-medium text-green-800">Connection Successful</h4>
            <div class="mt-2 text-sm text-green-700">
              <p><strong>Status:</strong> {{ testResult.status }}</p>
              <p><strong>Database:</strong> {{ testResult.database }}</p>
              <p><strong>Version:</strong> {{ testResult.version }}</p>
              <p *ngIf="testResult.readOnly" class="mt-2 text-green-600">
                ✓ Read-only access verified
              </p>
              <p *ngIf="!testResult.readOnly" class="mt-2 text-yellow-600">
                ⚠ Warning: Connection has write access
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Error Result -->
      <div *ngIf="testResult && !testResult.success" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg">
        <div class="flex items-start">
          <svg class="h-6 w-6 text-red-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4v.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <div class="ml-3">
            <h4 class="text-sm font-medium text-red-800">Connection Failed</h4>
            <div class="mt-2 text-sm text-red-700">
              <p><strong>Error:</strong> {{ testResult.error }}</p>
              <p *ngIf="testResult.suggestion" class="mt-2">
                <strong>Suggestion:</strong> {{ testResult.suggestion }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Test Duration -->
      <div *ngIf="testResult" class="mt-4 text-sm text-gray-600">
        <p>Test completed in {{ testResult.duration }}ms</p>
      </div>
    </div>
  `,
  styles: []
})
export class ConnectionTestComponent implements OnInit {
  @Input() connectionId: string | null = null;
  
  loading = false;
  testResult: any = null;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {}

  testConnection(): void {
    if (!this.connectionId) {
      return;
    }

    this.loading = true;
    this.testResult = null;

    const startTime = Date.now();

    this.apiService.testConnection(this.connectionId).subscribe({
      next: (response) => {
        this.loading = false;
        const duration = Date.now() - startTime;

        if (response.success) {
          this.testResult = {
            success: true,
            status: response.data?.status || 'CONNECTED',
            database: response.data?.database || 'Unknown',
            version: response.data?.version || 'Unknown',
            readOnly: response.data?.readOnly !== false,
            duration
          };
        } else {
          this.testResult = {
            success: false,
            error: response.message || 'Connection test failed',
            suggestion: this.getSuggestion(response.message),
            duration
          };
        }
      },
      error: (error) => {
        this.loading = false;
        const duration = Date.now() - startTime;

        this.testResult = {
          success: false,
          error: error.error?.message || error.message || 'Connection test failed',
          suggestion: this.getSuggestion(error.error?.message || error.message),
          duration
        };
      }
    });
  }

  private getSuggestion(errorMessage: string): string {
    if (!errorMessage) {
      return '';
    }

    const message = errorMessage.toLowerCase();

    if (message.includes('timeout')) {
      return 'Check if the host is reachable and the port is correct. The connection timeout is 5 seconds.';
    }
    if (message.includes('refused')) {
      return 'The database server is not running or not listening on the specified port.';
    }
    if (message.includes('authentication') || message.includes('password')) {
      return 'Check your username and password. Make sure they are correct.';
    }
    if (message.includes('ssl') || message.includes('tls')) {
      return 'Try disabling TLS/SSL or check your certificate configuration.';
    }
    if (message.includes('unknown host')) {
      return 'Check if the hostname is correct and resolvable.';
    }

    return 'Please check your connection settings and try again.';
  }
}
