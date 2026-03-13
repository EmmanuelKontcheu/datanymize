import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from './api.service';

/**
 * Authentication service for managing user authentication state.
 * 
 * Handles:
 * - User login/logout
 * - Token management
 * - Authentication state
 * - User information
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject: BehaviorSubject<any>;
  public currentUser$: Observable<any>;
  private tokenKey = 'auth_token';
  private refreshTokenKey = 'refresh_token';

  constructor(private apiService: ApiService) {
    this.currentUserSubject = new BehaviorSubject<any>(this.getUserFromStorage());
    this.currentUser$ = this.currentUserSubject.asObservable();
  }

  public get currentUserValue(): any {
    return this.currentUserSubject.value;
  }

  login(username: string, password: string): Observable<any> {
    return this.apiService.login(username, password).pipe(
      map(response => {
        if (response.success && response.data) {
          const user = response.data;
          localStorage.setItem(this.tokenKey, user.token);
          if (user.refreshToken) {
            localStorage.setItem(this.refreshTokenKey, user.refreshToken);
          }
          this.currentUserSubject.next(user);
          return user;
        }
        throw new Error('Login failed');
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.refreshTokenKey);
    this.currentUserSubject.next(null);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.refreshTokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  refreshToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    return this.apiService.refreshToken(refreshToken).pipe(
      map(response => {
        if (response.success && response.data) {
          localStorage.setItem(this.tokenKey, response.data.token);
          return response.data;
        }
        throw new Error('Token refresh failed');
      })
    );
  }

  getCurrentUser(): Observable<any> {
    return this.apiService.getCurrentUser().pipe(
      map(response => {
        if (response.success) {
          this.currentUserSubject.next(response.data);
          return response.data;
        }
        throw new Error('Failed to get current user');
      })
    );
  }

  private getUserFromStorage(): any {
    const token = localStorage.getItem(this.tokenKey);
    if (token) {
      // In production, would decode JWT to get user info
      return { token };
    }
    return null;
  }
}
