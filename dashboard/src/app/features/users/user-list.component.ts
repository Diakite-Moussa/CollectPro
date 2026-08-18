import { Component, inject, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { UserService } from '../../core/services/user.service';
import { PermissionService } from '../../core/services/permission.service';
import { AuthService } from '../../core/services/auth.service';
import { UserResponse } from '../../core/models/user.model';
import { PermissionResponse } from '../../core/models/permission.model';

@Component({
  selector: 'app-user-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Inviter un Utilisateur</h2>
    <mat-dialog-content>
      <form [formGroup]="userForm" class="dialog-form">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Adresse Email</mat-label>
          <input matInput formControlName="email" type="email" required>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Prénom</mat-label>
            <input matInput formControlName="firstName" required>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="lastName" required>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Rôle</mat-label>
          <mat-select formControlName="roleType" required>
            <mat-option value="ADMIN_SECONDAIRE">Admin Secondaire</mat-option>
            <mat-option value="SUPERVISOR">Superviseur</mat-option>
            <mat-option value="AGENT">Agent Terrain</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" [disabled]="userForm.invalid" (click)="onSave()">Envoyer l'invitation</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.5rem; width: 400px; }
    .w-full { width: 100%; }
    .form-row { display: flex; gap: 0.5rem; }
  `]
})
export class UserDialogComponent {
  userForm: FormGroup;
  private dialogRef = inject(MatDialogRef<UserDialogComponent>);

  constructor(private fb: FormBuilder) {
    this.userForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      roleType: ['AGENT', Validators.required]
    });
  }

  onSave(): void {
    if (this.userForm.valid) {
      this.dialogRef.close(this.userForm.value);
    }
  }
}

@Component({
  selector: 'app-assign-supervisor-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>{{ dialogTitle }}</h2>
    <mat-dialog-content>
      <p class="agent-info">
        Agent : <strong>{{ data.agent.firstName }} {{ data.agent.lastName }}</strong>
        ({{ data.agent.email }})
      </p>
      <p *ngIf="data.agent.assignedSupervisorName" class="current-supervisor">
        Superviseur actuel : <strong>{{ data.agent.assignedSupervisorName }}</strong>
      </p>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>{{ supervisorFieldLabel }}</mat-label>
          <mat-select formControlName="supervisorId" required>
            <mat-option *ngFor="let sup of data.supervisors" [value]="sup.id">
              {{ sup.firstName }} {{ sup.lastName }} ({{ sup.email }})
            </mat-option>
          </mat-select>
        </mat-form-field>
      </form>
      <p *ngIf="data.supervisors.length === 0" class="no-supervisor">
        Aucun superviseur actif dans l'organisation. Invitez d'abord un compte Superviseur.
      </p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary"
              [disabled]="form.invalid || data.supervisors.length === 0"
              (click)="onConfirm()">
        {{ confirmLabel }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { width: 420px; max-width: 90vw; margin-top: 0.5rem; }
    .w-full { width: 100%; }
    .agent-info { margin: 0 0 0.5rem; color: #475569; }
    .current-supervisor { margin: 0 0 0.75rem; font-size: 0.9rem; color: #166534; }
    .no-supervisor { color: #b45309; font-size: 0.9rem; margin-top: 0.5rem; }
  `]
})
export class AssignSupervisorDialogComponent {
  form: FormGroup;
  private dialogRef = inject(MatDialogRef<AssignSupervisorDialogComponent>);

  get dialogTitle(): string {
    return this.data.agent.assignedSupervisorId
      ? 'Changer le superviseur'
      : 'Affecter un superviseur';
  }

  get supervisorFieldLabel(): string {
    return this.data.agent.assignedSupervisorId ? 'Nouveau superviseur' : 'Superviseur';
  }

  get confirmLabel(): string {
    return this.data.agent.assignedSupervisorId
      ? 'Confirmer le changement'
      : "Confirmer l'affectation";
  }

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { agent: UserResponse; supervisors: UserResponse[] },
    private fb: FormBuilder
  ) {
    const defaultSupervisorId = data.agent.assignedSupervisorId ?? data.supervisors[0]?.id ?? null;
    this.form = this.fb.group({
      supervisorId: [defaultSupervisorId, Validators.required]
    });
  }

  onConfirm(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value.supervisorId as number);
    }
  }
}

@Component({
  selector: 'app-user-permissions-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatCheckboxModule
  ],
  template: `
    <h2 mat-dialog-title>Permissions — {{ data.user.firstName }} {{ data.user.lastName }}</h2>
    <mat-dialog-content class="dialog-content">
      <p class="hint">
        Droits individuels pour cet Admin secondaire (en plus de ceux du rôle, souvent vides).
      </p>
      <div class="permission-list">
        <label *ngFor="let perm of data.delegatable" class="permission-row">
          <mat-checkbox
            [checked]="isSelected(perm.code)"
            (change)="toggle(perm.code, $event.checked)">
          </mat-checkbox>
          <span class="perm-label">{{ perm.description }}</span>
          <span class="perm-code">{{ perm.code }}</span>
        </label>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" (click)="onSave()">Enregistrer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content { width: 480px; max-width: 90vw; }
    .hint { color: #64748b; font-size: 0.9rem; margin: 0 0 1rem; }
    .permission-list { display: flex; flex-direction: column; gap: 0.5rem; }
    .permission-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.35rem 0;
      border-bottom: 1px solid #f1f5f9;
    }
    .perm-label { flex: 1; font-size: 0.95rem; }
    .perm-code { font-size: 0.75rem; color: #94a3b8; font-family: monospace; }
  `]
})
export class UserPermissionsDialogComponent {
  selected = new Set<string>();

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: {
      user: UserResponse;
      delegatable: PermissionResponse[];
      individualCodes: string[];
    },
    private dialogRef: MatDialogRef<UserPermissionsDialogComponent>
  ) {
    data.individualCodes.forEach((code) => this.selected.add(code));
  }

  isSelected(code: string): boolean {
    return this.selected.has(code);
  }

  toggle(code: string, checked: boolean): void {
    if (checked) {
      this.selected.add(code);
    } else {
      this.selected.delete(code);
    }
  }

  onSave(): void {
    this.dialogRef.close([...this.selected]);
  }
}

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule
  ],
  template: `
    <div class="user-container">
      <div class="header-actions">
        <div>
          <h2>Gestion des Utilisateurs</h2>
          <p class="subtitle">Gestion des rôles, invitations et hiérarchie d'équipe</p>
        </div>
        <button mat-raised-button color="primary" (click)="openInviteDialog()">
          <mat-icon>person_add</mat-icon> Inviter un Utilisateur
        </button>
      </div>

      <mat-card class="table-card">
        <mat-card-content>
          <table mat-table [dataSource]="users" class="w-full">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> ID </th>
              <td mat-cell *matCellDef="let user"> #{{ user.id }} </td>
            </ng-container>

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef> Nom complet </th>
              <td mat-cell *matCellDef="let user" class="font-semibold"> {{ user.firstName }} {{ user.lastName }} </td>
            </ng-container>

            <ng-container matColumnDef="email">
              <th mat-header-cell *matHeaderCellDef> Email </th>
              <td mat-cell *matCellDef="let user"> {{ user.email }} </td>
            </ng-container>

            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef> Rôle </th>
              <td mat-cell *matCellDef="let user">
                <span class="role-badge" [ngClass]="user.role.toLowerCase()">
                  {{ user.role }}
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef> Statut </th>
              <td mat-cell *matCellDef="let user">
                <mat-chip-option [selectable]="false" [color]="user.status === 'ACTIVE' ? 'accent' : 'primary'">
                  {{ user.status }}
                </mat-chip-option>
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Action </th>
              <td mat-cell *matCellDef="let user">
                <button mat-icon-button color="primary"
                        *ngIf="canManagePermissions && user.role === 'ADMIN_SECONDAIRE'"
                        (click)="openPermissions(user)"
                        [disabled]="permissionsUserId === user.id"
                        title="Gérer les permissions">
                  <mat-icon>admin_panel_settings</mat-icon>
                </button>
                <button mat-icon-button color="accent"
                        *ngIf="canAssignSupervisor && user.role === 'AGENT' && !user.assignedSupervisorId"
                        (click)="openAssignSupervisor(user)"
                        [disabled]="assigningAgentId === user.id"
                        title="Affecter un superviseur">
                  <mat-icon>supervisor_account</mat-icon>
                </button>
                <span *ngIf="user.role === 'AGENT' && user.assignedSupervisorName"
                      class="supervisor-assigned">
                  <span class="supervisor-badge" title="Superviseur affecté">
                    <mat-icon inline>supervisor_account</mat-icon>
                    {{ user.assignedSupervisorName }}
                  </span>
                  <button mat-icon-button color="accent"
                          *ngIf="canAssignSupervisor"
                          (click)="openAssignSupervisor(user)"
                          [disabled]="assigningAgentId === user.id"
                          title="Changer le superviseur">
                    <mat-icon>swap_horiz</mat-icon>
                  </button>
                </span>
                <button mat-icon-button color="primary"
                        *ngIf="user.status === 'INVITED'"
                        (click)="resend(user)"
                        [disabled]="resendingId === user.id"
                        title="Renvoyer l'invitation">
                  <mat-icon>send</mat-icon>
                </button>
                <button mat-icon-button
                        *ngIf="canDisableUser && user.status !== 'INVITED' && !(user.id === currentUserId && user.role === 'ADMIN_PRINCIPAL')"
                        [color]="user.status === 'ACTIVE' ? 'warn' : 'accent'"
                        (click)="toggleUserStatus(user)"
                        [disabled]="togglingStatusId === user.id"
                        [title]="user.status === 'ACTIVE' ? 'Désactiver le compte' : 'Réactiver le compte'">
                  <mat-icon>{{ user.status === 'ACTIVE' ? 'block' : 'check_circle' }}</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .header-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
      h2 { margin: 0; font-weight: 700; }
      .subtitle { margin: 0; color: #64748b; }
    }
    .table-card { border-radius: 12px; }
    .w-full { width: 100%; }
    .font-semibold { font-weight: 600; }
    .role-badge {
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 600;
      background: #f1f5f9;
      color: #334155;
      &.super_admin { background: #fef2f2; color: #991b1b; }
      &.admin_principal { background: #eff6ff; color: #1e40af; }
      &.supervisor { background: #f0fdf4; color: #166534; }
      &.agent { background: #faf5ff; color: #6b21a8; }
    }
    .supervisor-assigned {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      vertical-align: middle;
    }
    .supervisor-badge {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 0.8rem;
      color: #166534;
      background: #f0fdf4;
      padding: 4px 8px;
      border-radius: 6px;
      max-width: 120px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }
  `]
})
export class UserListComponent implements OnInit {
  users: UserResponse[] = [];
  displayedColumns = ['id', 'name', 'email', 'role', 'status', 'actions'];
  resendingId: number | null = null;
  assigningAgentId: number | null = null;
  permissionsUserId: number | null = null;
  canAssignSupervisor = false;
  canManagePermissions = false;
  canDisableUser = false;
  currentUserId: number | null = null;
  togglingStatusId: number | null = null;

  constructor(
    private userService: UserService,
    private permissionService: PermissionService,
    private authService: AuthService,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    const role = this.authService.currentUser()?.role;
    this.canAssignSupervisor = role === 'SUPER_ADMIN'
      || role === 'ADMIN_PRINCIPAL'
      || role === 'ADMIN_SECONDAIRE';
    this.canManagePermissions = role === 'SUPER_ADMIN' || role === 'ADMIN_PRINCIPAL';
    this.canDisableUser = role === 'SUPER_ADMIN' || role === 'ADMIN_PRINCIPAL' || role === 'ADMIN_SECONDAIRE';
    this.currentUserId = this.authService.currentUser()?.id ?? null;
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getUsers().subscribe({
      next: (users) => this.users = users,
      error: (err) => console.error('Erreur de chargement des utilisateurs', err)
    });
  }

  resend(user: UserResponse): void {
    this.resendingId = user.id;
    this.userService.resendInvitation(user.id).subscribe({
      next: () => {
        this.resendingId = null;
        alert(`Invitation renvoyée à ${user.email}`);
      },
      error: (err) => {
        this.resendingId = null;
        alert(err?.error?.message || 'Erreur lors du renvoi de l\'invitation');
      }
    });
  }

  openInviteDialog(): void {
    const dialogRef = this.dialog.open(UserDialogComponent, { width: '450px' });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.userService.createUser(result).subscribe({
          next: () => this.loadUsers(),
          error: (err) => alert(err?.error?.message || 'Erreur lors de la création')
        });
      }
    });
  }

  openAssignSupervisor(agent: UserResponse): void {
    const supervisors = this.users.filter(
      (u) => u.role === 'SUPERVISOR' && u.status === 'ACTIVE'
    );
    const dialogRef = this.dialog.open(AssignSupervisorDialogComponent, {
      width: '480px',
      data: { agent, supervisors }
    });
    dialogRef.afterClosed().subscribe((supervisorId: number | undefined) => {
      if (!supervisorId) return;
      this.assigningAgentId = agent.id;
      this.userService.assignSupervisor({ agentId: agent.id, supervisorId }).subscribe({
        next: () => {
          this.assigningAgentId = null;
          this.loadUsers();
          const action = agent.assignedSupervisorId ? 'changé pour' : 'affecté à';
          alert(`Superviseur ${action} ${agent.firstName} ${agent.lastName}`);
        },
        error: (err) => {
          this.assigningAgentId = null;
          alert(err?.error?.message || 'Erreur lors de l\'affectation');
        }
      });
    });
  }

  openPermissions(user: UserResponse): void {
    this.permissionsUserId = user.id;
    this.permissionService.getDelegatablePermissions().subscribe({
      next: (delegatable) => {
        this.permissionService.getUserPermissions(user.id).subscribe({
          next: (perms) => {
            this.permissionsUserId = null;
            const dialogRef = this.dialog.open(UserPermissionsDialogComponent, {
              width: '520px',
              data: {
                user,
                delegatable,
                individualCodes: perms.individualPermissionCodes
              }
            });
            dialogRef.afterClosed().subscribe((codes: string[] | undefined) => {
              if (!codes) return;
              this.permissionsUserId = user.id;
              this.permissionService.updateUserPermissions(user.id, codes).subscribe({
                next: () => {
                  this.permissionsUserId = null;
                  alert(`Permissions mises à jour pour ${user.firstName} ${user.lastName}`);
                },
                error: (err) => {
                  this.permissionsUserId = null;
                  alert(err?.error?.message || 'Erreur lors de la mise à jour des permissions');
                }
              });
            });
          },
          error: (err) => {
            this.permissionsUserId = null;
            alert(err?.error?.message || 'Impossible de charger les permissions');
          }
        });
      },
      error: (err) => {
        this.permissionsUserId = null;
        alert(err?.error?.message || 'Impossible de charger les permissions déléguables');
      }
    });
  }

  toggleUserStatus(user: UserResponse): void {
    const action = user.status === 'ACTIVE' ? 'désactiver' : 'réactiver';
    const confirmed = confirm(
      `Voulez-vous vraiment ${action} le compte de ${user.firstName} ${user.lastName} (${user.email}) ?`
    );
    if (!confirmed) return;

    this.togglingStatusId = user.id;
    const newStatus: 'ACTIVE' | 'DISABLED' = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    this.userService.updateUserStatus(user.id, newStatus).subscribe({
      next: (updated) => {
        this.togglingStatusId = null;
        const idx = this.users.findIndex((u) => u.id === user.id);
        if (idx !== -1) this.users[idx] = updated;
        this.users = [...this.users]; // force change detection
        alert(`Compte ${newStatus === 'DISABLED' ? 'désactivé' : 'réactivé'} : ${user.email}`);
      },
      error: (err) => {
        this.togglingStatusId = null;
        alert(err?.error?.message || `Erreur lors de la ${action === 'désactiver' ? 'désactivation' : 'réactivation'}`);
      }
    });
  }
}
