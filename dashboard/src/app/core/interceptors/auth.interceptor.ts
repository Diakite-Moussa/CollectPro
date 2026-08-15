import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const isAuthRoute = req.url.includes('/login') ||
    req.url.includes('/refresh') ||
    req.url.includes('/activate');

  const token = authService.getToken();
  const authReq = token && !isAuthRoute
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isAuthRoute || req.url.includes('/refresh')) {
        return throwError(() => error);
      }
      if (req.headers.has('X-Retry-After-Refresh')) {
        authService.logout();
        return throwError(() => error);
      }
      return authService.refreshToken().pipe(
        switchMap(() => {
          const newToken = authService.getToken();
          const retryReq = req.clone({
            setHeaders: {
              Authorization: `Bearer ${newToken}`,
              'X-Retry-After-Refresh': 'true'
            }
          });
          return next(retryReq);
        }),
        catchError((refreshErr) => throwError(() => refreshErr))
      );
    })
  );
};
