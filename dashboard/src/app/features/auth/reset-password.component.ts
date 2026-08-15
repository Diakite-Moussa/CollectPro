import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
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
    <div class="reset-container">
      <mat-card class="reset-card">
        <mat-card-header class="justify-center">
          <mat-card-title class="brand-title">
            <mat-icon class="brand-icon">key</mat-icon>
            CollectPro
          </mat-card-title>
          <mat-card-subtitle>Création du nouveau mot de passe</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content class="mt-4">
          <div *ngIf="success" class="success-banner">
            <mat-icon color="primary">check_circle</mat-icon>
            <p>Votre mot de passe a été réinitialisé avec succès !</p>
            <button mat-flat-button color="primary" class="w-full mt-3" routerLink="/login">
              Se connecter
            </button>
          </div>

          <form *ngIf="!success" (ngSubmit)="onSubmit()" #resetForm="ngForm">
            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Nouveau mot de passe</mat-label>
              <input matInput [type]="hidePassword ? 'password' : 'text'" [(ngModel)]="newPassword" name="newPassword" required minlength="8" #pwdInput="ngModel">
              <button mat-icon-button matSuffix (click)="hidePassword = !hidePassword" type="button">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="pwdInput.invalid">Le mot de passe doit comporter au moins 8 caractères</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="w-full">
              <mat-label>Confirmer le mot de passe</mat-label>
              <input matInput [type]="hideConfirm ? 'password' : 'text'" [(ngModel)]="confirmPassword" name="confirmPassword" required minlength="8">
              <button mat-icon-button matSuffix (click)="hideConfirm = !hideConfirm" type="button">
                <mat-icon>{{ hideConfirm ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <div *ngIf="newPassword && confirmPassword && newPassword !== confirmPassword" class="error-message">
              Les deux mots de passe ne correspondent pas.
            </div>

            <div *ngIf="errorMessage" class="error-message">
              {{ errorMessage }}
            </div>

            <button mat-flat-button color="primary" class="w-full submit-btn"
                    [disabled]="resetForm.invalid || newPassword !== confirmPassword || loading || !token">
              {{ loading ? 'Réinitialisation...' : 'Valider le mot de passe' }}
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions class="justify-center" *ngIf="!success">
          <a mat-button routerLink="/login">
            <mat-icon>arrow_back</mat-icon> Retour à la connexion
          </a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .reset-container { display: flex; justify-content: center; align-items: center; min-height: 100vh; background-color: #0f172a; }
    .reset-card { width: 100%; max-width: 440px; padding: 1.5rem; border-radius: 16px; }
    .brand-title { display: flex; align-items: center; gap: 0.5rem; font-size: 1.6rem; font-weight: 700; }
    .brand-icon { color: #2563eb; }
    .justify-center { justify-content: center; text-align: center; }
    .w-full { width: 100%; }
    .mt-4 { margin-top: 1rem; }
    .mt-3 { margin-top: 0.75rem; }
    .submit-btn { padding: 1.4rem; font-size: 1rem; border-radius: 10px; margin-top: 0.5rem; }
    .error-message { color: #dc2626; font-size: 0.85rem; margin-bottom: 1rem; }
    .success-banner { background: #f0fdf4; border: 1px solid #bbf7d0; color: #166534; padding: 1rem; border-radius: 10px; text-align: center; font-size: 0.9rem; }
  `]
})
export class ResetPasswordComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  token = '';
  newPassword = '';
  confirmPassword = '';
  hidePassword = true;
  hideConfirm = true;
  loading = false;
  success = false;
  errorMessage = '';

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.errorMessage = 'Token de réinitialisation invalide ou absent de l\'URL.';
    }
  }

  onSubmit(): void {
    if (!this.token || !this.newPassword || this.newPassword !== this.confirmPassword) return;
    this.loading = true;
    this.errorMessage = '';

    this.authService.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Token invalide ou expiré';
      }
    });
  }
}
