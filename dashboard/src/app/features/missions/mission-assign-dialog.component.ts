import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MissionResponse } from '../../core/models/mission.model';
import { MissionService } from '../../core/services/mission.service';

export interface MissionAssignDialogData {
  mission: MissionResponse;
  availableAgents: { id: number; firstName: string; lastName: string; email?: string }[];
  availableForms: { id: number; name: string }[];
}

@Component({
  selector: 'app-mission-assign-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatSnackBarModule,
    MatDividerModule
  ],
  template: `
    <h2 mat-dialog-title>Affectations — {{ currentMission.name }}</h2>
    <mat-dialog-content>
      <div class="assign-section">
        <div class="section-header">
          <mat-icon color="primary">people</mat-icon>
          <h3>Agents de terrain assignés ({{ currentMission.agents.length }})</h3>
        </div>

        <div class="chip-list" *ngIf="currentMission.agents.length; else noAgents">
          <mat-chip-row
            *ngFor="let a of currentMission.agents"
            (removed)="onRemoveAgent(a.id)"
            class="agent-chip">
            {{ a.firstName }} {{ a.lastName }}
            <button matChipRemove aria-label="Retirer cet agent">
              <mat-icon>cancel</mat-icon>
            </button>
          </mat-chip-row>
        </div>
        <ng-template #noAgents>
          <p class="empty-hint">Aucun agent de terrain assigné pour l'instant.</p>
        </ng-template>

        <div class="assign-controls">
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Sélectionner des agents à ajouter</mat-label>
            <mat-select [formControl]="agentIdsControl" multiple placeholder="Choisir des agents">
              <mat-option *ngFor="let agent of unassignedAgents" [value]="agent.id">
                {{ agent.firstName }} {{ agent.lastName }} {{ agent.email ? '(' + agent.email + ')' : '' }}
              </mat-option>
            </mat-select>
            <mat-hint *ngIf="!unassignedAgents.length">Tous les agents disponibles sont déjà assignés.</mat-hint>
          </mat-form-field>

          <button
            mat-flat-button
            color="primary"
            [disabled]="!agentIdsControl.value?.length || isAssigningAgents"
            (click)="onAssignAgents()">
            <mat-icon>person_add</mat-icon> Assigner les agents sélectionnés
          </button>
        </div>
      </div>

      <mat-divider class="my-4"></mat-divider>

      <div class="assign-section">
        <div class="section-header">
          <mat-icon color="accent">description</mat-icon>
          <h3>Formulaires d'enquête liés ({{ currentMission.forms.length }})</h3>
        </div>

        <div class="chip-list" *ngIf="currentMission.forms.length; else noForms">
          <mat-chip-row *ngFor="let f of currentMission.forms" class="form-chip">
            {{ f.name }}
          </mat-chip-row>
        </div>
        <ng-template #noForms>
          <p class="empty-hint">Aucun formulaire rattaché à cette mission.</p>
        </ng-template>

        <div class="assign-controls">
          <mat-form-field appearance="outline" class="w-full">
            <mat-label>Associer des formulaires supplémentaires</mat-label>
            <mat-select [formControl]="formIdsControl" multiple placeholder="Choisir des formulaires">
              <mat-option *ngFor="let form of unlinkedForms" [value]="form.id">
                {{ form.name }}
              </mat-option>
            </mat-select>
            <mat-hint *ngIf="!unlinkedForms.length">Tous les formulaires publiés sont déjà associés.</mat-hint>
          </mat-form-field>

          <button
            mat-flat-button
            color="accent"
            [disabled]="!formIdsControl.value?.length || isAssigningForms"
            (click)="onAssignForms()">
            <mat-icon>add_link</mat-icon> Associer les formulaires
          </button>
        </div>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-raised-button mat-dialog-close [color]="hasChanged ? 'primary' : ''" (click)="onClose()">
        {{ hasChanged ? 'Terminer' : 'Fermer' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      min-width: 500px;
      max-width: 650px;
      padding-top: 0.5rem;
    }
    .assign-section {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
    .section-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.25rem;
      h3 { margin: 0; font-size: 1rem; font-weight: 600; color: #1e293b; }
    }
    .chip-list {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
      margin-bottom: 0.75rem;
    }
    .agent-chip {
      background-color: #e0f2fe;
      color: #0369a1;
      font-weight: 500;
    }
    .form-chip {
      background-color: #f1f5f9;
      color: #334155;
      font-weight: 500;
    }
    .empty-hint {
      color: #94a3b8;
      font-size: 0.875rem;
      font-style: italic;
      margin: 0.25rem 0 0.75rem 0;
    }
    .assign-controls {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      margin-top: 0.25rem;
    }
    .w-full { width: 100%; }
    .my-4 { margin: 1.25rem 0; }
  `]
})
export class MissionAssignDialogComponent {
  currentMission: MissionResponse;
  agentIdsControl = new FormControl<number[]>([]);
  formIdsControl = new FormControl<number[]>([]);

  isAssigningAgents = false;
  isAssigningForms = false;
  hasChanged = false;

  private missionService = inject(MissionService);
  private snackBar = inject(MatSnackBar);
  private dialogRef = inject(MatDialogRef<MissionAssignDialogComponent>);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: MissionAssignDialogData
  ) {
    this.currentMission = { ...this.data.mission };
  }

  get unassignedAgents() {
    const assignedIds = new Set(this.currentMission.agents.map(a => a.id));
    return this.data.availableAgents.filter(a => !assignedIds.has(a.id));
  }

  get unlinkedForms() {
    const linkedIds = new Set(this.currentMission.forms.map(f => f.id));
    return this.data.availableForms.filter(f => !linkedIds.has(f.id));
  }

  onAssignAgents(): void {
    const agentIds = this.agentIdsControl.value ?? [];
    if (!agentIds.length) return;

    this.isAssigningAgents = true;
    this.missionService.assignAgents(this.currentMission.id, { agentIds }).subscribe({
      next: (updated) => {
        this.currentMission = updated;
        this.agentIdsControl.setValue([]);
        this.isAssigningAgents = false;
        this.hasChanged = true;
        this.snackBar.open('Agents assignés avec succès', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.isAssigningAgents = false;
        this.snackBar.open(err?.error?.message || "Erreur lors de l'assignation des agents", 'Fermer', { duration: 4000 });
      }
    });
  }

  onRemoveAgent(agentId: number): void {
    this.missionService.unassignAgent(this.currentMission.id, agentId).subscribe({
      next: (updated) => {
        this.currentMission = updated;
        this.hasChanged = true;
        this.snackBar.open('Agent retiré de la mission', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || "Erreur lors du retrait de l'agent", 'Fermer', { duration: 4000 });
      }
    });
  }

  onAssignForms(): void {
    const formIds = this.formIdsControl.value ?? [];
    if (!formIds.length) return;

    this.isAssigningForms = true;
    this.missionService.assignForms(this.currentMission.id, { formIds }).subscribe({
      next: (updated) => {
        this.currentMission = updated;
        this.formIdsControl.setValue([]);
        this.isAssigningForms = false;
        this.hasChanged = true;
        this.snackBar.open('Formulaires associés avec succès', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.isAssigningForms = false;
        this.snackBar.open(err?.error?.message || "Erreur lors de l'association des formulaires", 'Fermer', { duration: 4000 });
      }
    });
  }

  onClose(): void {
    this.dialogRef.close(this.hasChanged ? this.currentMission : null);
  }
}
