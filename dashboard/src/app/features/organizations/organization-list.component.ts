import { Component, OnInit, inject, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { OrganizationService } from '../../core/services/organization.service';
import { OrganizationResponse } from '../../core/models/organization.model';
import { environment } from '../../../environments/environment';
import { ReportHistoryDialogComponent } from '../reports/report-history-dialog.component';

@Component({
  selector: 'app-organization-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Nouvelle Organisation</h2>
    <mat-dialog-content>
      <form [formGroup]="orgForm" class="dialog-form">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Nom de l'organisation</mat-label>
          <input matInput formControlName="name" placeholder="Ex: Croix-Rouge" required>
        </mat-form-field>

        <hr class="my-4">
        <p class="font-bold text-sm text-gray-600">Administrateur Principal</p>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Email Admin</mat-label>
          <input matInput formControlName="adminEmail" type="email" required>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Prénom</mat-label>
            <input matInput formControlName="adminFirstName" required>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="adminLastName" required>
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" [disabled]="orgForm.invalid" (click)="onSave()">Créer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.5rem; width: 400px; }
    .w-full { width: 100%; }
    .form-row { display: flex; gap: 0.5rem; }
    .my-4 { margin: 1rem 0; }
  `]
})
export class OrganizationDialogComponent {
  orgForm: FormGroup;
  private dialogRef = inject(MatDialogRef<OrganizationDialogComponent>);

  constructor(private fb: FormBuilder) {
    this.orgForm = this.fb.group({
      name: ['', Validators.required],
      adminEmail: ['', [Validators.required, Validators.email]],
      adminFirstName: ['', Validators.required],
      adminLastName: ['', Validators.required]
    });
  }

  onSave(): void {
    if (this.orgForm.valid) {
      this.dialogRef.close(this.orgForm.value);
    }
  }
}

@Component({
  selector: 'app-edit-organization-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Modifier l'Organisation</h2>
    <mat-dialog-content>
      <form [formGroup]="editForm" class="dialog-form">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Nom de l'organisation</mat-label>
          <input matInput formControlName="name" required>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" [disabled]="editForm.invalid" (click)="onSave()">Enregistrer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.5rem; width: 400px; }
    .w-full { width: 100%; }
  `]
})
export class EditOrganizationDialogComponent {
  editForm: FormGroup;
  private dialogRef = inject(MatDialogRef<EditOrganizationDialogComponent>);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { organization: OrganizationResponse },
    private fb: FormBuilder
  ) {
    this.editForm = this.fb.group({
      name: [data.organization.name, Validators.required],
      description: [data.organization.description ?? '']
    });
  }

  onSave(): void {
    if (this.editForm.valid) {
      this.dialogRef.close(this.editForm.value);
    }
  }
}

@Component({
  selector: 'app-organization-list',
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
    <div class="org-container">
      <div class="header-actions">
        <div>
          <h2>Gestion des Organisations</h2>
          <p class="subtitle">Isolation multi-tenant et administration des structures partneraires</p>
        </div>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon> Nouvelle Organisation
        </button>
      </div>

      <mat-card class="table-card">
        <mat-card-content>
          <table mat-table [dataSource]="organizations" class="w-full">
            <ng-container matColumnDef="logo">
              <th mat-header-cell *matHeaderCellDef> Logo </th>
              <td mat-cell *matCellDef="let org">
                <img *ngIf="org.logoUrl" [src]="getLogoUrl(org)" class="org-logo" alt="Logo" (error)="onImgError($event)">
                <button mat-icon-button (click)="fileInput.click()" title="Changer le logo" [disabled]="uploadingLogoId === org.id">
                  <mat-icon>image</mat-icon>
                </button>
                <input #fileInput type="file" accept="image/png,image/jpeg" hidden (change)="onLogoSelected($event, org)">
              </td>
            </ng-container>

            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> ID </th>
              <td mat-cell *matCellDef="let org"> #{{ org.id }} </td>
            </ng-container>

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef> Nom </th>
              <td mat-cell *matCellDef="let org" class="font-semibold"> {{ org.name }} </td>
            </ng-container>

            <ng-container matColumnDef="admin">
              <th mat-header-cell *matHeaderCellDef> Admin Principal </th>
              <td mat-cell *matCellDef="let org">
                {{ org.principalAdmin?.email || 'Non assigné' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef> Statut </th>
              <td mat-cell *matCellDef="let org">
                <mat-chip-option [selectable]="false" [color]="org.status === 'ACTIVE' ? 'accent' : 'warn'">
                  {{ org.status }}
                </mat-chip-option>
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Action </th>
              <td mat-cell *matCellDef="let org">
                <button mat-icon-button color="accent"
                        (click)="openOrgReports(org)"
                        title="Rapports PDF de l'organisation">
                  <mat-icon>analytics</mat-icon>
                </button>
                <button mat-icon-button color="primary"
                        (click)="openEditDialog(org)"
                        [disabled]="editingId === org.id"
                        title="Modifier l'organisation">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button
                        [color]="org.status === 'ACTIVE' ? 'warn' : 'accent'"
                        (click)="toggleOrganizationStatus(org)"
                        [disabled]="togglingStatusId === org.id"
                        [title]="org.status === 'ACTIVE' ? 'Désactiver' : 'Réactiver'">
                  <mat-icon>{{ org.status === 'ACTIVE' ? 'block' : 'check_circle' }}</mat-icon>
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
    .org-logo { width: 32px; height: 32px; object-fit: contain; border-radius: 4px; vertical-align: middle; margin-right: 4px; }
  `]
})
export class OrganizationListComponent implements OnInit {
  organizations: OrganizationResponse[] = [];
  displayedColumns = ['logo', 'id', 'name', 'admin', 'status', 'actions'];
  editingId: number | null = null;
  togglingStatusId: number | null = null;
  uploadingLogoId: number | null = null;

  constructor(
    private orgService: OrganizationService,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadOrganizations();
  }

  loadOrganizations(): void {
    this.orgService.getOrganizations().subscribe({
      next: (data) => this.organizations = data,
      error: (err) => console.error(err)
    });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(OrganizationDialogComponent, { width: '450px' });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.orgService.createOrganization(result).subscribe({
          next: () => this.loadOrganizations(),
          error: (err) => alert(err?.error?.message || 'Erreur lors de la création')
        });
      }
    });
  }

  openEditDialog(org: OrganizationResponse): void {
    const dialogRef = this.dialog.open(EditOrganizationDialogComponent, {
      width: '450px',
      data: { organization: org }
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (!result) return;
      this.editingId = org.id;
      this.orgService.updateOrganization(org.id, result).subscribe({
        next: (updated) => {
          this.editingId = null;
          const idx = this.organizations.findIndex((o) => o.id === org.id);
          if (idx !== -1) this.organizations[idx] = updated;
          this.organizations = [...this.organizations];
        },
        error: (err) => {
          this.editingId = null;
          alert(err?.error?.message || 'Erreur lors de la modification');
        }
      });
    });
  }

  toggleOrganizationStatus(org: OrganizationResponse): void {
    const action = org.status === 'ACTIVE' ? 'désactiver' : 'réactiver';
    const confirmed = confirm(
      `Voulez-vous vraiment ${action} l'organisation "${org.name}" ?`
    );
    if (!confirmed) return;

    this.togglingStatusId = org.id;
    const newStatus: 'ACTIVE' | 'INACTIVE' = org.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.orgService.updateOrganizationStatus(org.id, newStatus).subscribe({
      next: (updated) => {
        this.togglingStatusId = null;
        const idx = this.organizations.findIndex((o) => o.id === org.id);
        if (idx !== -1) this.organizations[idx] = updated;
        this.organizations = [...this.organizations];
      },
      error: (err) => {
        this.togglingStatusId = null;
        alert(err?.error?.message || `Erreur lors de la ${action === 'désactiver' ? 'désactivation' : 'réactivation'}`);
      }
    });
  }

  onLogoSelected(event: Event, org: OrganizationResponse): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.uploadingLogoId = org.id;
    this.orgService.uploadLogo(org.id, file).subscribe({
      next: (updated) => {
        this.uploadingLogoId = null;
        const idx = this.organizations.findIndex((o) => o.id === org.id);
        if (idx !== -1) this.organizations[idx] = updated;
        this.organizations = [...this.organizations];
        input.value = '';
      },
      error: (err) => {
        this.uploadingLogoId = null;
        alert(err?.error?.message || 'Erreur lors de l\'upload du logo');
        input.value = '';
      }
    });
  }

  getLogoUrl(org: OrganizationResponse): string | null {
    if (!org.logoUrl) return null;
    return org.logoUrl.startsWith('http') ? org.logoUrl : `${environment.apiUrl}${org.logoUrl}`;
  }

  onImgError(event: Event): void {
    (event.target as HTMLElement).style.display = 'none';
  }

  openOrgReports(org: OrganizationResponse): void {
    this.dialog.open(ReportHistoryDialogComponent, {
      width: '750px',
      data: {
        title: org.name,
        type: 'ORGANIZATION',
        targetId: org.id,
        canGenerate: true
      }
    });
  }
}