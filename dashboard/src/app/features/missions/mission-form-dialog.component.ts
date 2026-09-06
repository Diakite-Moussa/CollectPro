import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MissionResponse, MissionStatus } from '../../core/models/mission.model';

export interface MissionDialogData {
  mission?: MissionResponse;
}

@Component({
  selector: 'app-mission-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  template: `
    <h2 mat-dialog-title>{{ isEditMode ? 'Modifier la Mission' : 'Nouvelle Mission' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="missionForm" class="dialog-form">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Nom de la mission</mat-label>
          <input matInput formControlName="name" placeholder="Ex: Recensement Quartier Nord" required>
          <mat-error *ngIf="missionForm.get('name')?.hasError('required')">
            Le nom de la mission est requis
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2" placeholder="Objectifs et consignes de la mission..."></textarea>
        </mat-form-field>

        <div *ngIf="isEditMode" class="w-full">
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Statut</mat-label>
            <mat-select formControlName="status">
              <mat-option value="DRAFT">Brouillon (DRAFT)</mat-option>
              <mat-option value="ACTIVE">Active (ACTIVE)</mat-option>
              <mat-option value="COMPLETED">Terminée (COMPLETED)</mat-option>
              <mat-option value="CANCELLED">Annulée (CANCELLED)</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline" class="flex-1">
            <mat-label>Date de début</mat-label>
            <input matInput [matDatepicker]="startPicker" formControlName="startDate" (click)="startPicker.open()">
            <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
            <mat-datepicker #startPicker></mat-datepicker>
          </mat-form-field>

          <mat-form-field appearance="outline" class="flex-1">
            <mat-label>Date de fin</mat-label>
            <input matInput [matDatepicker]="endPicker" formControlName="endDate" (click)="endPicker.open()">
            <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
            <mat-datepicker #endPicker></mat-datepicker>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Objectif de collectes attendues (optionnel)</mat-label>
          <input matInput type="number" min="1" formControlName="expectedCollectesCount" placeholder="Ex: 100">
          <mat-hint>Utilisé pour calculer la jauge de progression de la mission</mat-hint>
        </mat-form-field>

        <div class="gps-section">
          <p class="section-title">📍 Zone d'intervention GPS (optionnel)</p>
          <div class="form-row">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Latitude</mat-label>
              <input matInput type="number" step="any" formControlName="latitude" placeholder="Ex: 12.6392">
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Longitude</mat-label>
              <input matInput type="number" step="any" formControlName="longitude" placeholder="Ex: -8.0029">
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Rayon (mètres)</mat-label>
              <input matInput type="number" min="10" formControlName="radiusMeters" placeholder="Ex: 500">
            </mat-form-field>
          </div>
          <span class="text-xs text-gray-500">
            Les collectes soumises au-delà de ce rayon recevront un avertissement "Hors zone".
          </span>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" [disabled]="missionForm.invalid" (click)="onSave()">
        {{ isEditMode ? 'Enregistrer les modifications' : 'Créer la mission' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      width: 100%;
      min-width: 480px;
      max-width: 560px;
      padding-top: 0.5rem;
    }
    .w-full { width: 100%; }
    .form-row { display: flex; gap: 0.75rem; }
    .flex-1 { flex: 1; }
    .gps-section {
      background-color: #f8fafc;
      border: 1px dashed #cbd5e1;
      border-radius: 8px;
      padding: 0.85rem;
      margin-top: 0.5rem;
    }
    .section-title {
      font-weight: 600;
      font-size: 0.875rem;
      color: #334155;
      margin: 0 0 0.5rem 0;
    }
    .text-xs { font-size: 0.75rem; }
    .text-gray-500 { color: #64748b; }
  `]
})
export class MissionFormDialogComponent implements OnInit {
  missionForm: FormGroup;
  isEditMode = false;
  private dialogRef = inject(MatDialogRef<MissionFormDialogComponent>);

  constructor(
    private fb: FormBuilder,
    @Inject(MAT_DIALOG_DATA) public data?: MissionDialogData
  ) {
    this.isEditMode = !!this.data?.mission;
    this.missionForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      status: ['DRAFT'],
      startDate: [null],
      endDate: [null],
      latitude: [null],
      longitude: [null],
      radiusMeters: [null],
      expectedCollectesCount: [null, [Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    if (this.data?.mission) {
      const m = this.data.mission;
      this.missionForm.patchValue({
        name: m.name,
        description: m.description || '',
        status: m.status || 'DRAFT',
        startDate: m.startDate ? new Date(m.startDate) : null,
        endDate: m.endDate ? new Date(m.endDate) : null,
        latitude: m.latitude ?? null,
        longitude: m.longitude ?? null,
        radiusMeters: m.radiusMeters ?? null,
        expectedCollectesCount: m.expectedCollectesCount ?? null
      });
    }
  }

  onSave(): void {
    if (this.missionForm.invalid) return;
    const v = this.missionForm.value;

    const payload = {
      name: v.name,
      description: v.description?.trim() || undefined,
      status: (this.isEditMode ? v.status : undefined) as MissionStatus | undefined,
      startDate: v.startDate ? new Date(v.startDate).toISOString() : undefined,
      endDate: v.endDate ? new Date(v.endDate).toISOString() : undefined,
      latitude: v.latitude !== null && v.latitude !== '' ? Number(v.latitude) : undefined,
      longitude: v.longitude !== null && v.longitude !== '' ? Number(v.longitude) : undefined,
      radiusMeters: v.radiusMeters !== null && v.radiusMeters !== '' ? Number(v.radiusMeters) : undefined,
      expectedCollectesCount: v.expectedCollectesCount !== null && v.expectedCollectesCount !== '' ? Number(v.expectedCollectesCount) : undefined
    };

    this.dialogRef.close(payload);
  }
}
