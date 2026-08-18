import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { UserService } from '../../core/services/user.service';
import { UserResponse } from '../../core/models/user.model';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const newPassword = control.get('newPassword')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;
  return newPassword && confirmPassword && newPassword !== confirmPassword
    ? { passwordMismatch: true }
    : null;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  template: `
    <div class="profile-container">
      <div class="profile-header">
        <div class="avatar-circle">
          <mat-icon class="avatar-icon">account_circle</mat-icon>
        </div>
        <div class="header-info" *ngIf="currentUser()">
          <h1 class="profile-title">{{ currentUser()!.firstName }} {{ currentUser()!.lastName }}</h1>
          <span class="role-badge">{{ currentUser()!.role }}</span>
        </div>
      </div>

      <div class="cards-grid">

        <!-- Bloc 1 : Informations du profil -->
        <mat-card class="profile-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-avatar-icon">person</mat-icon>
            <mat-card-title>Informations personnelles</mat-card-title>
            <mat-card-subtitle>Modifiez vos informations de contact</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <form [formGroup]="profileForm" (ngSubmit)="onUpdateProfile()" class="form-content">

              <div class="form-row">
                <mat-form-field appearance="outline" class="half-width">
                  <mat-label>Prénom</mat-label>
                  <input matInput formControlName="firstName" placeholder="Votre prénom" id="profile-firstName">
                  <mat-error *ngIf="profileForm.get('firstName')?.hasError('required')">Le prénom est obligatoire</mat-error>
                </mat-form-field>

                <mat-form-field appearance="outline" class="half-width">
                  <mat-label>Nom</mat-label>
                  <input matInput formControlName="lastName" placeholder="Votre nom" id="profile-lastName">
                  <mat-error *ngIf="profileForm.get('lastName')?.hasError('required')">Le nom est obligatoire</mat-error>
                </mat-form-field>
              </div>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Téléphone</mat-label>
                <mat-icon matPrefix>phone</mat-icon>
                <input matInput formControlName="phone" placeholder="+221 XX XXX XX XX" id="profile-phone">
              </mat-form-field>

              <mat-divider class="section-divider"></mat-divider>
              <p class="read-only-label">Champs non modifiables</p>

              <mat-form-field appearance="outline" class="full-width read-only-field">
                <mat-label>Email</mat-label>
                <mat-icon matPrefix>email</mat-icon>
                <input matInput [value]="currentUser()?.email || ''" readonly id="profile-email">
                <mat-hint>L'adresse email ne peut pas être modifiée</mat-hint>
              </mat-form-field>

              <div class="form-row">
                <mat-form-field appearance="outline" class="half-width read-only-field">
                  <mat-label>Rôle</mat-label>
                  <mat-icon matPrefix>badge</mat-icon>
                  <input matInput [value]="currentUser()?.role || ''" readonly id="profile-role">
                </mat-form-field>

                <mat-form-field appearance="outline" class="half-width read-only-field">
                  <mat-label>Organisation</mat-label>
                  <mat-icon matPrefix>corporate_fare</mat-icon>
                  <input matInput [value]="currentUser()?.organizationName ?? 'Aucune'" readonly id="profile-org">
                </mat-form-field>
              </div>

              <div class="form-actions">
                <button
                  mat-flat-button
                  color="primary"
                  type="submit"
                  id="profile-save-btn"
                  [disabled]="profileForm.invalid || profileSaving()">
                  <mat-spinner *ngIf="profileSaving()" diameter="18" class="btn-spinner"></mat-spinner>
                  <mat-icon *ngIf="!profileSaving()">save</mat-icon>
                  {{ profileSaving() ? 'Enregistrement...' : 'Enregistrer' }}
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>

        <!-- Bloc 2 : Changement de mot de passe -->
        <mat-card class="profile-card">
          <mat-card-header>
            <mat-icon mat-card-avatar class="card-avatar-icon security-icon">lock</mat-icon>
            <mat-card-title>Sécurité</mat-card-title>
            <mat-card-subtitle>Modifiez votre mot de passe</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>
            <form [formGroup]="passwordForm" (ngSubmit)="onChangePassword()" class="form-content">

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Mot de passe actuel</mat-label>
                <mat-icon matPrefix>lock_outline</mat-icon>
                <input matInput
                       [type]="showCurrentPwd() ? 'text' : 'password'"
                       formControlName="currentPassword"
                       id="password-current">
                <button mat-icon-button matSuffix type="button" (click)="showCurrentPwd.set(!showCurrentPwd())">
                  <mat-icon>{{ showCurrentPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                <mat-error *ngIf="passwordForm.get('currentPassword')?.hasError('required')">Obligatoire</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nouveau mot de passe</mat-label>
                <mat-icon matPrefix>lock</mat-icon>
                <input matInput
                       [type]="showNewPwd() ? 'text' : 'password'"
                       formControlName="newPassword"
                       id="password-new">
                <button mat-icon-button matSuffix type="button" (click)="showNewPwd.set(!showNewPwd())">
                  <mat-icon>{{ showNewPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('required')">Obligatoire</mat-error>
                <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('minlength')">Au moins 8 caractères</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Confirmer le nouveau mot de passe</mat-label>
                <mat-icon matPrefix>lock</mat-icon>
                <input matInput
                       [type]="showConfirmPwd() ? 'text' : 'password'"
                       formControlName="confirmPassword"
                       id="password-confirm">
                <button mat-icon-button matSuffix type="button" (click)="showConfirmPwd.set(!showConfirmPwd())">
                  <mat-icon>{{ showConfirmPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
                <mat-error *ngIf="passwordForm.hasError('passwordMismatch') && passwordForm.get('confirmPassword')?.dirty">
                  Les mots de passe ne correspondent pas
                </mat-error>
              </mat-form-field>

              <div class="password-strength" *ngIf="passwordForm.get('newPassword')?.value">
                <span class="strength-label">Force :</span>
                <span class="strength-bar" [class]="getPasswordStrengthClass()">
                  {{ getPasswordStrengthLabel() }}
                </span>
              </div>

              <div class="form-actions">
                <button
                  mat-flat-button
                  color="accent"
                  type="submit"
                  id="password-save-btn"
                  [disabled]="passwordForm.invalid || passwordSaving()">
                  <mat-spinner *ngIf="passwordSaving()" diameter="18" class="btn-spinner"></mat-spinner>
                  <mat-icon *ngIf="!passwordSaving()">key</mat-icon>
                  {{ passwordSaving() ? 'Modification...' : 'Modifier le mot de passe' }}
                </button>
              </div>
            </form>
          </mat-card-content>
        </mat-card>

      </div>
    </div>
  `,
  styles: [`
    .profile-container {
      max-width: 1100px;
      margin: 0 auto;
      padding: 1rem 0;
    }

    .profile-header {
      display: flex;
      align-items: center;
      gap: 1.5rem;
      margin-bottom: 2rem;
      padding: 1.5rem 2rem;
      background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
      border-radius: 16px;
      color: white;
    }

    .avatar-circle {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      background: rgba(59, 130, 246, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      border: 2px solid rgba(59, 130, 246, 0.6);
    }

    .avatar-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #93c5fd;
    }

    .profile-title {
      margin: 0 0 0.4rem;
      font-size: 1.6rem;
      font-weight: 700;
    }

    .role-badge {
      background: rgba(59, 130, 246, 0.25);
      border: 1px solid rgba(59, 130, 246, 0.5);
      color: #93c5fd;
      padding: 3px 12px;
      border-radius: 20px;
      font-size: 0.8rem;
      font-weight: 600;
      letter-spacing: 0.04em;
    }

    .cards-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1.5rem;
    }

    @media (max-width: 900px) {
      .cards-grid {
        grid-template-columns: 1fr;
      }
    }

    .profile-card {
      border-radius: 12px !important;
      box-shadow: 0 2px 12px rgba(0,0,0,0.08) !important;
    }

    .card-avatar-icon {
      font-size: 28px !important;
      width: 28px !important;
      height: 28px !important;
      color: #3b82f6 !important;
      background: #eff6ff !important;
      border-radius: 50% !important;
      padding: 6px !important;
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
    }

    .security-icon {
      color: #8b5cf6 !important;
      background: #f5f3ff !important;
    }

    .form-content {
      padding-top: 1rem;
    }

    .form-row {
      display: flex;
      gap: 1rem;
    }

    .full-width {
      width: 100%;
    }

    .half-width {
      flex: 1;
    }

    .section-divider {
      margin: 0.5rem 0 1rem !important;
    }

    .read-only-label {
      font-size: 0.8rem;
      color: #94a3b8;
      margin: 0 0 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }

    .read-only-field input {
      color: #94a3b8 !important;
      cursor: not-allowed;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      margin-top: 1.5rem;
    }

    .form-actions button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0 1.5rem;
      height: 44px;
    }

    .btn-spinner {
      display: inline-block;
    }

    .password-strength {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
      font-size: 0.85rem;
    }

    .strength-label {
      color: #64748b;
      font-weight: 600;
    }

    .strength-bar {
      padding: 2px 10px;
      border-radius: 12px;
      font-weight: 600;
      font-size: 0.8rem;
    }

    .strength-bar.weak {
      background: #fee2e2;
      color: #dc2626;
    }

    .strength-bar.medium {
      background: #fef3c7;
      color: #d97706;
    }

    .strength-bar.strong {
      background: #d1fae5;
      color: #059669;
    }
  `]
})
export class ProfileComponent implements OnInit {
  private userService = inject(UserService);
  private fb = inject(FormBuilder);
  private snackBar = inject(MatSnackBar);

  currentUser = signal<UserResponse | null>(null);
  profileSaving = signal(false);
  passwordSaving = signal(false);
  showCurrentPwd = signal(false);
  showNewPwd = signal(false);
  showConfirmPwd = signal(false);

  profileForm = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    phone: ['']
  });

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator });

  ngOnInit(): void {
    this.userService.getMe().subscribe({
      next: (user) => {
        this.currentUser.set(user);
        this.profileForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          phone: user.phone ?? ''
        });
      },
      error: () => this.snackBar.open('Erreur lors du chargement du profil', 'Fermer', { duration: 3000 })
    });
  }

  onUpdateProfile(): void {
    if (this.profileForm.invalid) return;
    this.profileSaving.set(true);
    const { firstName, lastName, phone } = this.profileForm.value;
    this.userService.updateProfile({ firstName: firstName!, lastName: lastName!, phone: phone ?? undefined }).subscribe({
      next: (updated) => {
        this.currentUser.set(updated);
        this.profileSaving.set(false);
        this.snackBar.open('Profil mis à jour avec succès', 'Fermer', { duration: 3000, panelClass: 'snack-success' });
      },
      error: () => {
        this.profileSaving.set(false);
        this.snackBar.open('Erreur lors de la mise à jour', 'Fermer', { duration: 3000 });
      }
    });
  }

  onChangePassword(): void {
    if (this.passwordForm.invalid) return;
    this.passwordSaving.set(true);
    const { currentPassword, newPassword } = this.passwordForm.value;
    this.userService.changePassword({ currentPassword: currentPassword!, newPassword: newPassword! }).subscribe({
      next: () => {
        this.passwordSaving.set(false);
        this.passwordForm.reset();
        this.snackBar.open('Mot de passe modifié avec succès', 'Fermer', { duration: 3000, panelClass: 'snack-success' });
      },
      error: (err) => {
        this.passwordSaving.set(false);
        const msg = err?.error?.message ?? 'Erreur lors du changement de mot de passe';
        this.snackBar.open(msg, 'Fermer', { duration: 4000 });
      }
    });
  }

  getPasswordStrengthClass(): string {
    const val: string = this.passwordForm.get('newPassword')?.value ?? '';
    if (val.length < 8) return 'weak';
    const hasUpper = /[A-Z]/.test(val);
    const hasNumber = /[0-9]/.test(val);
    const hasSpecial = /[^A-Za-z0-9]/.test(val);
    const score = [hasUpper, hasNumber, hasSpecial].filter(Boolean).length;
    if (score >= 2) return 'strong';
    if (score === 1) return 'medium';
    return 'weak';
  }

  getPasswordStrengthLabel(): string {
    const cls = this.getPasswordStrengthClass();
    return cls === 'strong' ? 'Fort' : cls === 'medium' ? 'Moyen' : 'Faible';
  }
}
