import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card">
        <mat-card-content>
          <div class="logo-header">
            <mat-icon class="brand-icon">assessment</mat-icon>
            <h2>CollectPro Admin</h2>
            <p class="subtitle">Plateforme de collecte de données terrain</p>
          </div>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            @if (errorMessage) {
              <div class="error-banner">
                <mat-icon>error_outline</mat-icon>
                <span>{{ errorMessage }}</span>
              </div>
            }

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Adresse Email</mat-label>
              <input matInput formControlName="email" type="email" placeholder="admin@org.com" required>
              <mat-icon matSuffix>email</mat-icon>
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Mot de passe</mat-label>
              <input matInput formControlName="password" [type]="hidePassword ? 'password' : 'text'" required>
              <button mat-icon-button matSuffix (click)="hidePassword = !hidePassword" type="button">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <div class="forgot-link-wrapper">
              <a routerLink="/forgot-password" class="forgot-link">Mot de passe oublié ?</a>
            </div>

            <button mat-raised-button color="primary" class="w-full submit-btn" type="submit" [disabled]="loginForm.invalid || loading">
              @if (!loading) {
                <span>Se connecter</span>
              } @else {
                <mat-spinner diameter="24"></mat-spinner>
              }
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
      padding: 1rem;
      box-sizing: border-box;
    }
    .login-card {
      width: 100%;
      max-width: 420px;
      padding: 1.5rem;
      border-radius: 16px;
      background: #ffffff;
    }
    .logo-header {
      width: 100%;
      text-align: center;
      margin-bottom: 1.5rem;
      .brand-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: #2563eb;
      }
      h2 {
        margin: 0.5rem 0 0.2rem 0;
        font-weight: 700;
        color: #0f172a;
      }
      .subtitle {
        color: #64748b;
        font-size: 0.9rem;
        margin: 0;
      }
    }
    .w-full {
      width: 100%;
      margin-bottom: 1rem;
    }
    .submit-btn {
      height: 48px;
      font-size: 1rem;
      font-weight: 600;
      border-radius: 8px;
      display: flex;
      justify-content: center;
      align-items: center;
    }
    .error-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background-color: #fef2f2;
      color: #dc2626;
      padding: 0.75rem;
      border-radius: 8px;
      margin-bottom: 1rem;
      font-size: 0.875rem;
    }
  `]
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = false;
  hidePassword = true;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading = true;
    this.errorMessage = null;

    this.authService.login(this.loginForm.value).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Échec de connexion. Vérifiez vos identifiants.';
      }
    });
  }
}
