import { Injectable } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Authentication guard to protect routes.
 * 
 * Redirects to login if user is not authenticated.
 */
export const authGuard: CanActivateFn = (route, state) => {
  const authService = new AuthService(null as any);
  const router = new Router();

  if (authService.isAuthenticated()) {
    return true;
  }

  router.navigate(['/auth/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
