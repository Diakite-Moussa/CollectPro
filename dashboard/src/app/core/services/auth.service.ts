import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, throwError, switchMap, shareReplay } from 'rxjs';
import { AuthResponse, LoginRequest, UserSummary } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = environment.apiUrl;
  private readonly tokenKey = 'collectpro_access_token';
  private readonly refreshTokenKey = 'collectpro_refresh_token';
  private readonly userKey = 'collectpro_user';

  currentUser = signal<UserSummary | null>(null);
  private refreshInFlight: Observable<AuthResponse> | null = null;

  constructor(private http: HttpClient) {
    this.currentUser.set(this.getStoredUser());
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request).pipe(
      tap((res) => this.setSession(res))
    );
  }

  activateAccount(token: string, password: string): Observable<UserSummary> {
    return this.http.post<UserSummary>(`${this.apiUrl}/activate`, { token, password });
  }

  forgotPassword(email: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/forgot-password`, { email });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  refreshToken(): Observable<AuthResponse> {
    const storedRefresh = this.getRefreshToken();
    if (!storedRefresh) {
      return throwError(() => new Error('No refresh token'));
    }
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http
        .post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken: storedRefresh })
        .pipe(
          tap((res) => this.setSession(res)),
          catchError((err) => {
            this.logout();
            return throwError(() => err);
          }),
          shareReplay(1)
        );
      this.refreshInFlight.subscribe({
        complete: () => { this.refreshInFlight = null; },
        error: () => { this.refreshInFlight = null; }
      });
    }
    return this.refreshInFlight;
  }

  logout(): void {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.refreshTokenKey);
        localStorage.removeItem(this.userKey);
      }
    } catch { }
    this.currentUser.set(null);
  }

  getToken(): string | null {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        return localStorage.getItem(this.tokenKey);
      }
    } catch { }
    return null;
  }

  getRefreshToken(): string | null {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        return localStorage.getItem(this.refreshTokenKey);
      }
    } catch { }
    return null;
  }

  isAuthenticated(): boolean {
    return !!this.getToken() || !!this.getRefreshToken();
  }

  private setSession(authResult: AuthResponse): void {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.setItem(this.tokenKey, authResult.accessToken);
        localStorage.setItem(this.refreshTokenKey, authResult.refreshToken);
        localStorage.setItem(this.userKey, JSON.stringify(authResult.user));
      }
    } catch { }
    this.currentUser.set(authResult.user);
  }

  private getStoredUser(): UserSummary | null {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        const raw = localStorage.getItem(this.userKey);
        if (!raw) return null;
        return JSON.parse(raw);
      }
    } catch { }
    return null;
  }
}
