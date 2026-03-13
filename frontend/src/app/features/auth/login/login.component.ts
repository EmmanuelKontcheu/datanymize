import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Login component for user authentication.
 * 
 * Features:
 * - Email/password form
 * - Form validation
 * - Error handling
 * - Loading state
 * - Redirect to dashboard on success
 * 
 * Validates Requirements: 8.1, 8.2, 8.3, 8.4, 8.5
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-blue-600 to-blue-800 flex items-center justify-center px-4">
      <div class="w-full max-w-md">
        <!-- Logo/Title -->
        <div class="text-center mb-8">
          <h1 class="text-4xl font-bold text-white mb-2">Datanymize</h1>
          <p class="text-blue-100">Multi-Database Anonymization Platform</p>
        </div>

        <!-- Login Card -->
        <div class="bg-white rounded-lg shadow-xl p-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-6">Sign In</h2>

          <!-- Error Alert -->
          <div *ngIf="error" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-red-800 text-sm">{{ error }}</p>
          </div>

          <!-- Login Form -->
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <!-- Email Field -->
            <div class="mb-4">
              <label for="username" class="block text-sm font-medium text-gray-700 mb-2">
                Email or Username
              </label>
              <input
                id="username"
                type="text"
                formControlName="username"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
                placeholder="Enter your email or username"
                [disabled]="loading"
              />
              <p *ngIf="isFieldInvalid('username')" class="mt-1 text-sm text-red-600">
                Username is required
              </p>
            </div>

            <!-- Password Field -->
            <div class="mb-6">
              <label for="password" class="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <input
                id="password"
                type="password"
                formControlName="password"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none transition"
                placeholder="Enter your password"
                [disabled]="loading"
              />
              <p *ngIf="isFieldInvalid('password')" class="mt-1 text-sm text-red-600">
                Password is required
              </p>
            </div>

            <!-- Submit Button -->
            <button
              type="submit"
              [disabled]="loading || loginForm.invalid"
              class="w-full bg-blue-600 text-white py-2 rounded-lg font-medium hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
            >
              <span *ngIf="!loading">Sign In</span>
              <span *ngIf="loading" class="flex items-center justify-center">
                <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Signing in...
              </span>
            </button>
          </form>

          <!-- Demo Credentials -->
          <div class="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
            <p class="text-sm text-gray-700 mb-2">
              <strong>Demo Credentials:</strong>
            </p>
            <p class="text-sm text-gray-600">
              Email: <code class="bg-white px-2 py-1 rounded">demo@datanymize.com</code>
            </p>
            <p class="text-sm text-gray-600">
              Password: <code class="bg-white px-2 py-1 rounded">demo123</code>
            </p>
          </div>
        </div>

        <!-- Footer -->
        <div class="text-center mt-6 text-blue-100">
          <p class="text-sm">© 2024 Datanymize. All rights reserved.</p>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  loading = false;
  error = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  initializeForm(): void {
    this.loginForm = this.formBuilder.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = '';

    const { username, password } = this.loginForm.value;

    this.authService.login(username, password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading = false;
        this.error = error.error?.message || 'Login failed. Please check your credentials.';
      }
    });
  }
}
