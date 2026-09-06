import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError, throwError, shareReplay } from 'rxjs';
import { AuthResponse, LoginRequest, UserSummary } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl = environment.apiUrl;
  private readonly userKey = 'collectpro_user';
  private accessTokenInMemory: string | null = null;

  currentUser = signal<UserSummary | null>(null);
  private refreshInFlight: Observable<AuthResponse> | null = null;

  constructor(private http: HttpClient, private router: Router) {
    this.currentUser.set(this.getStoredUser());
  }

  initSession(): Observable<AuthResponse | null> {
    if (this.getStoredUser()) {
      return this.refreshToken().pipe(
        catchError(() => of(null))
      );
    }
    return of(null);
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, request, { withCredentials: true }).pipe(
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
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http
        .post<AuthResponse>(`${this.apiUrl}/refresh`, {}, { withCredentials: true })
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

  logout(redirectToLogin: boolean = true): void {
    this.http.post(`${this.apiUrl}/logout`, {}, { withCredentials: true }).subscribe({ error: () => {} });
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        localStorage.removeItem(this.userKey);
      }
    } catch { }
    this.accessTokenInMemory = null;
    this.currentUser.set(null);
    if (redirectToLogin) {
      this.router.navigate(['/login']);
    }
  }

  getToken(): string | null {
    return this.accessTokenInMemory;
  }

  isAuthenticated(): boolean {
    return !!this.accessTokenInMemory || !!this.getStoredUser();
  }

  private setSession(authResult: AuthResponse): void {
    this.accessTokenInMemory = authResult.accessToken;
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
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
