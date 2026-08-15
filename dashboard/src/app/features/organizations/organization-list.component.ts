import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { OrganizationService } from '../../core/services/organization.service';
import { OrganizationResponse } from '../../core/models/organization.model';

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
  `]
})
export class OrganizationListComponent implements OnInit {
  organizations: OrganizationResponse[] = [];
  displayedColumns = ['id', 'name', 'admin', 'status'];

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
}