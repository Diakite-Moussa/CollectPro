import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ROUTE_ROLES } from '../constants/route-roles';

export const roleGuard: CanActivateFn = (route) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const routeKey = route.routeConfig?.path ?? '';
  const allowedRoles = ROUTE_ROLES[routeKey];

  // Pas de restriction définie pour cette route => accessible à tout utilisateur connecté
  if (!allowedRoles) return true;

  const userRole = authService.currentUser()?.role;

  if (userRole && allowedRoles.includes(userRole as any)) {
    return true;
  }

  router.navigate(['/dashboard']);
  return false;
};
