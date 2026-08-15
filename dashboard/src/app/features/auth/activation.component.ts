import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/services/auth.service';

function passwordsMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('password')?.value;
    const confirm = control.get('confirmPassword')?.value;
    return password === confirm ? null : { passwordsMismatch: true };
}

@Component({
    selector: 'app-activation',
    standalone: true,
    imports: [
        CommonModule,
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
    <div class="activation-container">
      <mat-card class="activation-card">
        <mat-card-content>
          <div class="logo-header">
            <mat-icon class="brand-icon">how_to_reg</mat-icon>
            <h2>Activation du compte</h2>
            <p class="subtitle">Définissez votre mot de passe pour activer votre accès</p>
          </div>

          @if (!token) {
            <div class="error-banner">
              <mat-icon>error_outline</mat-icon>
              <span>Lien d'activation invalide ou incomplet (token manquant).</span>
            </div>
          }

          @if (successMessage) {
            <div class="success-banner">
              <mat-icon>check_circle</mat-icon>
              <span>{{ successMessage }}</span>
            </div>
            <button mat-raised-button color="primary" class="w-full" routerLink="/login">
              Aller à la connexion
            </button>
          } @else if (token) {
            <form [formGroup]="activationForm" (ngSubmit)="onSubmit()">
              @if (errorMessage) {
                <div class="error-banner">
                  <mat-icon>error_outline</mat-icon>
                  <span>{{ errorMessage }}</span>
                </div>
              }

              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Nouveau mot de passe</mat-label>
                <input matInput formControlName="password" type="password" required>
                <mat-hint>8 caractères minimum</mat-hint>
              </mat-form-field>

              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Confirmer le mot de passe</mat-label>
                <input matInput formControlName="confirmPassword" type="password" required>
              </mat-form-field>

              @if (activationForm.errors?.['passwordsMismatch'] && activationForm.get('confirmPassword')?.touched) {
                <p class="field-error">Les mots de passe ne correspondent pas.</p>
              }

              <button mat-raised-button color="primary" class="w-full submit-btn" type="submit"
                      [disabled]="activationForm.invalid || loading">
                @if (!loading) {
                  <span>Activer mon compte</span>
                } @else {
                  <mat-spinner diameter="24"></mat-spinner>
                }
              </button>
            </form>
          }
        </mat-card-content>
      </mat-card>
    </div>
  `,
    styles: [`
    .activation-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
      padding: 1rem;
      box-sizing: border-box;
    }
    .activation-card {
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
    .success-banner {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background-color: #f0fdf4;
      color: #166534;
      padding: 0.75rem;
      border-radius: 8px;
      margin-bottom: 1rem;
      font-size: 0.875rem;
    }
    .field-error {
      color: #dc2626;
      font-size: 0.8rem;
      margin: -0.5rem 0 1rem 0;
    }
  `]
})
export class ActivationComponent implements OnInit {
    private route = inject(ActivatedRoute);
    private authService = inject(AuthService);
    private fb = inject(FormBuilder);

    token: string | null = null;
    loading = false;
    errorMessage: string | null = null;
    successMessage: string | null = null;

    activationForm: FormGroup = this.fb.group({
        password: ['', [Validators.required, Validators.minLength(8)]],
        confirmPassword: ['', Validators.required]
    }, { validators: passwordsMatchValidator });

    ngOnInit(): void {
        this.token = this.route.snapshot.queryParamMap.get('token');
    }

    onSubmit(): void {
        if (this.activationForm.invalid || !this.token) return;

        this.loading = true;
        this.errorMessage = null;

        this.authService.activateAccount(this.token, this.activationForm.value.password).subscribe({
            next: () => {
                this.loading = false;
                this.successMessage = 'Compte activé avec succès ! Vous pouvez maintenant vous connecter.';
            },
            error: (err) => {
                this.loading = false;
                this.errorMessage = err?.error?.message || 'Échec de l\'activation. Le lien est peut-être expiré ou déjà utilisé.';
            }
        });
    }
}