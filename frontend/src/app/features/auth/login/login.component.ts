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
    <div style="min-height: 100vh; background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); display: flex; align-items: center; justify-content: center; padding: 1rem;">
      <div style="width: 100%; max-width: 420px;">
        <!-- Logo/Title -->
        <div style="text-align: center; margin-bottom: 3rem;">
          <div style="display: inline-flex; align-items: center; justify-content: center; width: 64px; height: 64px; background: linear-gradient(135deg, #3b82f6 0%, #7c3aed 100%); border-radius: 12px; margin-bottom: 1rem; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);">
            <svg style="width: 32px; height: 32px; color: white;" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
            </svg>
          </div>
          <h1 style="font-size: 2rem; font-weight: bold; color: white; margin: 0.5rem 0;">Datanymize</h1>
          <p style="color: #94a3b8; font-size: 1.125rem; margin: 0;">Multi-Database Anonymization Platform</p>
        </div>

        <!-- Login Card -->
        <div style="background: #1e293b; border-radius: 16px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); padding: 2rem; border: 1px solid #334155;">
          <h2 style="font-size: 1.5rem; font-weight: bold; color: white; margin-bottom: 2rem;">Welcome Back</h2>

          <!-- Error Alert -->
          <div *ngIf="error" style="margin-bottom: 1.5rem; padding: 1rem; background: rgba(239, 68, 68, 0.1); border: 1px solid rgba(239, 68, 68, 0.3); border-radius: 8px;">
            <div style="display: flex; align-items: center; gap: 0.75rem;">
              <svg style="width: 20px; height: 20px; color: #fca5a5;" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"></path>
              </svg>
              <p style="color: #fca5a5; font-size: 0.875rem;">{{ error }}</p>
            </div>
          </div>

          <!-- Login Form -->
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" style="display: flex; flex-direction: column; gap: 1.25rem;">
            <!-- Email Field -->
            <div>
              <label for="username" style="display: block; font-size: 0.875rem; font-weight: 500; color: #cbd5e1; margin-bottom: 0.5rem;">
                Email or Username
              </label>
              <input
                id="username"
                type="text"
                formControlName="username"
                style="width: 100%; padding: 0.75rem 1rem; background: #334155; border: 1px solid #475569; border-radius: 8px; color: white; font-size: 1rem; outline: none; transition: all 0.2s;"
                (focus)="$event.target.style.borderColor='#3b82f6'; $event.target.style.boxShadow='0 0 0 3px rgba(59, 130, 246, 0.1)'"
                (blur)="$event.target.style.borderColor='#475569'; $event.target.style.boxShadow='none'"
                placeholder="Enter your email or username"
                [disabled]="loading"
              />
              <p *ngIf="isFieldInvalid('username')" style="margin-top: 0.5rem; font-size: 0.875rem; color: #f87171;">
                Username is required
              </p>
            </div>

            <!-- Password Field -->
            <div>
              <label for="password" style="display: block; font-size: 0.875rem; font-weight: 500; color: #cbd5e1; margin-bottom: 0.5rem;">
                Password
              </label>
              <input
                id="password"
                type="password"
                formControlName="password"
                style="width: 100%; padding: 0.75rem 1rem; background: #334155; border: 1px solid #475569; border-radius: 8px; color: white; font-size: 1rem; outline: none; transition: all 0.2s;"
                (focus)="$event.target.style.borderColor='#3b82f6'; $event.target.style.boxShadow='0 0 0 3px rgba(59, 130, 246, 0.1)'"
                (blur)="$event.target.style.borderColor='#475569'; $event.target.style.boxShadow='none'"
                placeholder="Enter your password"
                [disabled]="loading"
              />
              <p *ngIf="isFieldInvalid('password')" style="margin-top: 0.5rem; font-size: 0.875rem; color: #f87171;">
                Password is required
              </p>
            </div>

            <!-- Submit Button -->
            <button
              type="submit"
              [disabled]="loading || loginForm.invalid"
              style="margin-top: 2rem; padding: 0.75rem 1rem; background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%); color: white; border: none; border-radius: 8px; font-weight: 600; font-size: 1rem; cursor: pointer; transition: all 0.2s; box-shadow: 0 10px 15px -3px rgba(37, 99, 235, 0.3);"
              [style.opacity]="loading || loginForm.invalid ? '0.5' : '1'"
              [style.cursor]="loading || loginForm.invalid ? 'not-allowed' : 'pointer'"
              (mouseenter)="!loading && loginForm.valid ? $event.target.style.boxShadow='0 20px 25px -5px rgba(37, 99, 235, 0.4)' : null"
              (mouseleave)="$event.target.style.boxShadow='0 10px 15px -3px rgba(37, 99, 235, 0.3)'"
            >
              <span *ngIf="!loading" style="display: flex; align-items: center; justify-content: center;">
                Sign In
              </span>
              <span *ngIf="loading" style="display: flex; align-items: center; justify-content: center; gap: 0.5rem;">
                <svg style="width: 20px; height: 20px; color: white; animation: spin 1s linear infinite;" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle style="opacity: 0.25;" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path style="opacity: 0.75;" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Signing in...
              </span>
            </button>
          </form>

          <!-- Divider -->
          <div style="position: relative; margin: 2rem 0;">
            <div style="position: absolute; inset: 0; display: flex; align-items: center;">
              <div style="width: 100%; border-top: 1px solid #475569;"></div>
            </div>
            <div style="position: relative; display: flex; justify-content: center; font-size: 0.875rem;">
              <span style="padding: 0 0.5rem; background: #1e293b; color: #64748b;">Demo Credentials</span>
            </div>
          </div>

          <!-- Demo Credentials -->
          <div style="display: flex; flex-direction: column; gap: 0.75rem; padding: 1rem; background: rgba(51, 65, 85, 0.5); border-radius: 8px; border: 1px solid #475569;">
            <div>
              <p style="font-size: 0.75rem; color: #64748b; margin-bottom: 0.25rem;">Email</p>
              <code style="font-size: 0.875rem; color: #93c5fd; font-family: monospace;">demo&#64;datanymize.com</code>
            </div>
            <div>
              <p style="font-size: 0.75rem; color: #64748b; margin-bottom: 0.25rem;">Password</p>
              <code style="font-size: 0.875rem; color: #93c5fd; font-family: monospace;">demo123</code>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div style="text-align: center; margin-top: 2rem;">
          <p style="color: #64748b; font-size: 0.875rem;">© 2024 Datanymize. All rights reserved.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
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
