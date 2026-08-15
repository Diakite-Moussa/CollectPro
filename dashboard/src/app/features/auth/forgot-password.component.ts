import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <div class="forgot-container">
      <mat-card class="forgot-card">
        <mat-card-header class="justify-center">
          <mat-card-title class="brand-title">
            <mat-icon class="brand-icon">lock_reset</mat-icon>
            CollectPro
          </mat-card-title>
          <mat-card-subtitle>Réinitialisation de mot de passe</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content class="mt-4">
          <div *ngIf="submitted" class="success-banner">
            <mat-icon color="primary">mark_email_read</mat-icon>
            <p>Si cette adresse email correspond à un compte actif, un lien de réinitialisation vous a été envoyé par courriel.</p>
          </div>

          <form *ngIf="!submitted" (ngSubmit)="onSubmit()" #forgotForm="ngForm">
            <p class="instruction-text">
              Saisissez l'adresse email associée à votre compte CollectPro pour recevoir un lien de réinitialisation.
            </p>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Adresse Email</mat-label>
              <input matInput type="email" [(ngModel)]="email" name="email" required email #emailInput="ngModel">
              <mat-icon matSuffix>email</mat-icon>
              <mat-error *ngIf="emailInput.invalid">Veuillez saisir une adresse email valide</mat-error>
            </mat-form-field>

            <div *ngIf="errorMessage" class="error-message">
              {{ errorMessage }}
            </div>

            <button mat-flat-button color="primary" class="w-full submit-btn" [disabled]="forgotForm.invalid || loading">
              {{ loading ? 'Envoi en cours...' : 'Envoyer le lien' }}
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions class="justify-center">
          <a mat-button routerLink="/login">
            <mat-icon>arrow_back</mat-icon> Retour à la connexion
          </a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .forgot-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background-color: #0f172a; }
    .forgot-card { width: 100%; max-width: 440px; padding: 1.5rem; border-radius: 16px; }
    .brand-title { display: flex; align-items: center; gap: 0.5rem; font-size: 1.6rem; font-weight: 700; }
    .brand-icon { color: #2563eb; }
    .justify-center { justify-content: center; text-align: center; }
    .w-full { width: 100%; }
    .mt-4 { margin-top: 1rem; }
    .instruction-text { font-size: 0.9rem; color: #475569; margin-bottom: 1rem; }
    .submit-btn { padding: 1.4rem; font-size: 1rem; border-radius: 10px; margin-top: 0.5rem; }
    .error-message { color: #dc2626; font-size: 0.85rem; margin-bottom: 1rem; }
    .success-banner { background: #eff6ff; border: 1px solid #bfdbfe; color: #1e40af; padding: 1rem; border-radius: 10px; text-align: center; font-size: 0.9rem; }
  `]
})
export class ForgotPasswordComponent {
  private authService = inject(AuthService);

  email = '';
  loading = false;
  submitted = false;
  errorMessage = '';

  onSubmit(): void {
    if (!this.email) return;
    this.loading = true;
    this.errorMessage = '';

    this.authService.forgotPassword(this.email).subscribe({
      next: () => {
        this.loading = false;
        this.submitted = true;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Une erreur est survenue';
      }
    });
  }
}
