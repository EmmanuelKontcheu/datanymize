import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';

/**
 * HTTP interceptor for handling errors.
 * 
 * Handles:
 * - 401 Unauthorized - redirects to login
 * - 403 Forbidden - shows error message
 * - 404 Not Found - shows error message
 * - 500 Server Error - shows error message
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError(error => {
      if (error.status === 401) {
        // Unauthorized - redirect to login
        router.navigate(['/auth/login']);
      } else if (error.status === 403) {
        // Forbidden
        console.error('Access denied', error);
      } else if (error.status === 404) {
        // Not found
        console.error('Resource not found', error);
      } else if (error.status === 500) {
        // Server error
        console.error('Server error', error);
      }

      return throwError(() => error);
    })
  );
};
