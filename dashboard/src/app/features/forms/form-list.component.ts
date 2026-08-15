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
import { MatTabsModule } from '@angular/material/tabs';
import { FormService } from '../../core/services/form.service';
import { FormResponse } from '../../core/models/form.model';
import { FormBuilderComponent } from './form-builder.component';

@Component({
  selector: 'app-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatTabsModule,
    FormBuilderComponent
  ],
  template: `
    <h2 mat-dialog-title>Nouveau Formulaire</h2>
    <mat-dialog-content class="dialog-content">
      <form [formGroup]="formGroup" class="dialog-form">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Titre du Formulaire</mat-label>
          <input matInput formControlName="name" placeholder="Ex: Enquête de Santé Publique" required>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>

        <mat-tab-group>
          <mat-tab label="Constructeur visuel">
            <div class="tab-panel">
              <app-form-builder formControlName="schemaJson"></app-form-builder>
            </div>
          </mat-tab>
          <mat-tab label="JSON (avancé)">
            <div class="tab-panel">
              <mat-form-field appearance="outline" class="w-full">
                <mat-label>Schéma JSON</mat-label>
                <textarea matInput formControlName="schemaJson" rows="12" required></textarea>
              </mat-form-field>
            </div>
          </mat-tab>
        </mat-tab-group>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" [disabled]="formGroup.invalid" (click)="onSave()">
        Enregistrer Brouillon
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content { width: 620px; max-width: 95vw; }
    .dialog-form { display: flex; flex-direction: column; gap: 0.5rem; }
    .w-full { width: 100%; }
    .tab-panel { padding: 1rem 0; min-height: 200px; }
  `]
})
export class FormDialogComponent {
  formGroup: FormGroup;
  private dialogRef = inject(MatDialogRef<FormDialogComponent>);

  constructor(private fb: FormBuilder) {
    const defaultSchema = JSON.stringify({
      version: 1,
      fields: [
        { key: 'patient_name', label: 'Nom du patient', type: 'text', required: true },
        { key: 'age', label: 'Âge', type: 'number', required: true },
        { key: 'symptoms', label: 'Symptômes', type: 'textarea', required: false }
      ]
    }, null, 2);

    this.formGroup = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      schemaJson: [defaultSchema, Validators.required]
    });
  }

  onSave(): void {
    if (this.formGroup.valid) {
      this.dialogRef.close(this.formGroup.value);
    }
  }
}

@Component({
  selector: 'app-form-list',
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
    <div class="form-container">
      <div class="header-actions">
        <div>
          <h2>Gestion des Formulaires</h2>
          <p class="subtitle">Création, publication et versioning des formulaires de collecte</p>
        </div>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon> Créer un Formulaire
        </button>
      </div>

      <mat-card class="table-card">
        <mat-card-content>
          <table mat-table [dataSource]="forms" class="w-full">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> ID </th>
              <td mat-cell *matCellDef="let form"> #{{ form.id }} </td>
            </ng-container>

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef> Titre </th>
              <td mat-cell *matCellDef="let form" class="font-semibold"> {{ form.name }} </td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef> Statut </th>
              <td mat-cell *matCellDef="let form">
                <mat-chip-option [selectable]="false" [color]="getStatusColor(form.status)">
                  {{ form.status }}
                </mat-chip-option>
              </td>
            </ng-container>

            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef> Date Création </th>
              <td mat-cell *matCellDef="let form"> {{ form.createdAt | date:'dd/MM/yyyy' }} </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Actions </th>
              <td mat-cell *matCellDef="let form">
                <button mat-icon-button color="primary" *ngIf="form.status === 'DRAFT'" (click)="publishForm(form.id)" title="Publier">
                  <mat-icon>publish</mat-icon>
                </button>
                <button mat-icon-button color="warn" *ngIf="form.status === 'PUBLISHED'" (click)="archiveForm(form.id)" title="Archiver">
                  <mat-icon>archive</mat-icon>
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
  `]
})
export class FormListComponent implements OnInit {
  forms: FormResponse[] = [];
  displayedColumns = ['id', 'name', 'status', 'createdAt', 'actions'];

  constructor(
    private formService: FormService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadForms();
  }

  loadForms(): void {
    this.formService.getForms().subscribe({
      next: (data) => this.forms = data,
      error: (err) => console.error(err)
    });
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(FormDialogComponent, { width: '680px', maxHeight: '90vh' });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.formService.createForm({ name: result.name, description: result.description }).subscribe({
          next: (createdForm) => {
            this.formService.createFormVersion(createdForm.id, { schemaJson: result.schemaJson }).subscribe({
              next: () => this.loadForms(),
              error: (err) => alert(err?.error?.message || 'Formulaire créé, mais échec de la création de la version')
            });
          },
          error: (err) => alert(err?.error?.message || 'Erreur lors de la création')
        });
      }
    });
  }

  publishForm(id: number): void {
    this.formService.publishForm(id).subscribe({
      next: () => this.loadForms(),
      error: (err) => alert(err?.error?.message || 'Erreur de publication')
    });
  }

  archiveForm(id: number): void {
    this.formService.archiveForm(id).subscribe({
      next: () => this.loadForms(),
      error: (err) => alert(err?.error?.message || 'Erreur d\'archivage')
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PUBLISHED': return 'accent';
      case 'ARCHIVED': return 'warn';
      default: return 'primary';
    }
  }
}
