import { Component, OnInit, inject, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, FormControl, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatTabsModule, MatTabChangeEvent } from '@angular/material/tabs';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MapViewComponent, MapMarker } from '../../shared/map-view/map-view.component';
import { MissionService } from '../../core/services/mission.service';
import { MissionResponse, MissionProgressResponse } from '../../core/models/mission.model';
import { UserService } from '../../core/services/user.service';
import { FormService } from '../../core/services/form.service';
import { ReportService } from '../../core/services/report.service';
import { ReportHistoryDialogComponent } from '../reports/report-history-dialog.component';

@Component({
  selector: 'app-mission-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule,
    MatDatepickerModule, MatNativeDateModule
  ],
  template: `
    <h2 mat-dialog-title>Nouvelle Mission</h2>
    <mat-dialog-content>
      <form [formGroup]="missionForm" class="dialog-form">
        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Nom de la mission</mat-label>
          <input matInput formControlName="name" required>
        </mat-form-field>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Date de début</mat-label>
            <input matInput [matDatepicker]="startPicker" formControlName="startDate" (click)="startPicker.open()">
            <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
            <mat-datepicker #startPicker></mat-datepicker>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Date de fin</mat-label>
            <input matInput [matDatepicker]="endPicker" formControlName="endDate" (click)="endPicker.open()">
            <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
            <mat-datepicker #endPicker></mat-datepicker>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="w-full">
          <mat-label>Objectif de collectes attendues (optionnel)</mat-label>
          <input matInput type="number" min="1" formControlName="expectedCollectesCount">
          <mat-hint>Utilisé pour calculer la progression de la mission</mat-hint>
        </mat-form-field>

        <hr class="my-4">
        <p class="font-bold text-sm text-gray-600">Zone GPS (optionnel)</p>
        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Latitude</mat-label>
            <input matInput type="number" formControlName="latitude">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Longitude</mat-label>
            <input matInput type="number" formControlName="longitude">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Rayon (m)</mat-label>
            <input matInput type="number" formControlName="radiusMeters">
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-raised-button color="primary" [disabled]="missionForm.invalid" (click)="onSave()">Créer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.5rem; width: 450px; }
    .w-full { width: 100%; }
    .form-row { display: flex; gap: 0.5rem; }
    .my-4 { margin: 1rem 0; }
  `]
})
export class MissionDialogComponent {
  missionForm: FormGroup;
  private dialogRef = inject(MatDialogRef<MissionDialogComponent>);

  constructor(private fb: FormBuilder) {
    this.missionForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      startDate: [null],
      endDate: [null],
      latitude: [null],
      longitude: [null],
      radiusMeters: [null],
      expectedCollectesCount: [null, [Validators.min(1)]]
    });
  }

  onSave(): void {
    if (this.missionForm.invalid) return;
    const v = this.missionForm.value;
    this.dialogRef.close({
      name: v.name,
      description: v.description || undefined,
      startDate: v.startDate ? new Date(v.startDate).toISOString() : undefined,
      endDate: v.endDate ? new Date(v.endDate).toISOString() : undefined,
      latitude: v.latitude ?? undefined,
      longitude: v.longitude ?? undefined,
      radiusMeters: v.radiusMeters ?? undefined,
      expectedCollectesCount: v.expectedCollectesCount ?? undefined
    });
  }
}

@Component({
  selector: 'app-mission-assign-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatSelectModule, MatButtonModule, MatChipsModule, MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Assigner à "{{ data.mission.name }}"</h2>
    <mat-dialog-content>
      <p class="font-bold text-sm text-gray-600">Agents assignés</p>
      <div class="chip-list" *ngIf="data.mission.agents.length">
        <mat-chip-option *ngFor="let a of data.mission.agents" [selectable]="false" (removed)="onRemoveAgent(a.id)">
          {{ a.firstName }} {{ a.lastName }}
          <button matChipRemove><mat-icon>cancel</mat-icon></button>
        </mat-chip-option>
      </div>
      <p *ngIf="!data.mission.agents.length" class="text-sm text-gray-500">Aucun agent assigné</p>

      <mat-form-field appearance="outline" class="w-full">
        <mat-label>Ajouter des agents</mat-label>
        <mat-select [formControl]="agentIdsControl" multiple>
          <mat-option *ngFor="let agent of availableAgents" [value]="agent.id">
            {{ agent.firstName }} {{ agent.lastName }}
          </mat-option>
        </mat-select>
      </mat-form-field>
      <button mat-stroked-button color="primary" [disabled]="!agentIdsControl.value?.length" (click)="onAssignAgents()">
        Assigner les agents sélectionnés
      </button>

      <hr class="my-4">

      <p class="font-bold text-sm text-gray-600">Formulaires liés</p>
      <div class="chip-list" *ngIf="data.mission.forms.length">
        <mat-chip-option *ngFor="let f of data.mission.forms" [selectable]="false">
          {{ f.name }}
        </mat-chip-option>
      </div>
      <p *ngIf="!data.mission.forms.length" class="text-sm text-gray-500">Aucun formulaire lié</p>

      <mat-form-field appearance="outline" class="w-full">
        <mat-label>Ajouter des formulaires</mat-label>
        <mat-select [formControl]="formIdsControl" multiple>
          <mat-option *ngFor="let form of availableForms" [value]="form.id">
            {{ form.name }}
          </mat-option>
        </mat-select>
      </mat-form-field>
      <button mat-stroked-button color="primary" [disabled]="!formIdsControl.value?.length" (click)="onAssignForms()">
        Lier les formulaires sélectionnés
      </button>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-raised-button mat-dialog-close (click)="onClose()">Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .w-full { width: 100%; margin-top: 0.75rem; }
    .chip-list { display: flex; flex-wrap: wrap; gap: 0.25rem; margin-bottom: 0.5rem; }
    .my-4 { margin: 1.5rem 0; }
  `]
})
export class MissionAssignDialogComponent {
  agentIdsControl = new FormControl<number[]>([]);
  formIdsControl = new FormControl<number[]>([]);
  changed = false;
  private missionService = inject(MissionService);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { mission: MissionResponse; availableAgents: { id: number; firstName: string; lastName: string }[]; availableForms: { id: number; name: string }[] }
  ) { }

  get availableAgents() {
    const assignedIds = new Set(this.data.mission.agents.map(a => a.id));
    return this.data.availableAgents.filter(a => !assignedIds.has(a.id));
  }

  get availableForms() {
    const linkedIds = new Set(this.data.mission.forms.map(f => f.id));
    return this.data.availableForms.filter(f => !linkedIds.has(f.id));
  }

  onAssignAgents(): void {
    this.missionService.assignAgents(this.data.mission.id, { agentIds: this.agentIdsControl.value ?? [] }).subscribe({
      next: (updated) => {
        this.data.mission = updated;
        this.agentIdsControl.setValue([]);
        this.changed = true;
      },
      error: (err) => alert(err?.error?.message || "Erreur lors de l'assignation")
    });
  }

  onRemoveAgent(agentId: number): void {
    this.missionService.unassignAgent(this.data.mission.id, agentId).subscribe({
      next: (updated) => {
        this.data.mission = updated;
        this.changed = true;
      },
      error: (err) => alert(err?.error?.message || 'Erreur lors du retrait')
    });
  }

  onAssignForms(): void {
    this.missionService.assignForms(this.data.mission.id, { formIds: this.formIdsControl.value ?? [] }).subscribe({
      next: (updated) => {
        this.data.mission = updated;
        this.formIdsControl.setValue([]);
        this.changed = true;
      },
      error: (err) => alert(err?.error?.message || "Erreur lors de l'association")
    });
  }

  onClose(): void { }
}

@Component({
  selector: 'app-mission-list',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatTableModule, MatButtonModule,
    MatIconModule, MatChipsModule, MatDialogModule,
    MatTabsModule, MatProgressBarModule, MapViewComponent
  ],
  template: `
    <div class="mission-container">
      <div class="header-actions">
        <div>
          <h2>Gestion des Missions</h2>
          <p class="subtitle">Zones d'intervention, échéances et affectations terrain</p>
        </div>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon> Nouvelle Mission
        </button>
      </div>

      <mat-tab-group (selectedTabChange)="onTabChange($event)">
        <mat-tab label="Liste">
          <mat-card class="table-card">
            <mat-card-content>
              <table mat-table [dataSource]="missions" class="w-full">
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef> Nom </th>
                  <td mat-cell *matCellDef="let m" class="font-semibold"> {{ m.name }} </td>
                </ng-container>

                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef> Statut </th>
                  <td mat-cell *matCellDef="let m">
                    <mat-chip-option [selectable]="false" [color]="statusColor(m.status)">
                      {{ m.status }}
                    </mat-chip-option>
                  </td>
                </ng-container>

                <ng-container matColumnDef="dates">
                  <th mat-header-cell *matHeaderCellDef> Échéance </th>
                  <td mat-cell *matCellDef="let m">
                    {{ m.startDate ? (m.startDate | date:'dd/MM/yyyy') : '—' }}
                    →
                    {{ m.endDate ? (m.endDate | date:'dd/MM/yyyy') : '—' }}
                  </td>
                </ng-container>

                <ng-container matColumnDef="agents">
                  <th mat-header-cell *matHeaderCellDef> Agents </th>
                  <td mat-cell *matCellDef="let m"> {{ m.agents.length }} </td>
                </ng-container>

                <ng-container matColumnDef="forms">
                  <th mat-header-cell *matHeaderCellDef> Formulaires </th>
                  <td mat-cell *matCellDef="let m"> {{ m.forms.length }} </td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef> Action </th>
                  <td mat-cell *matCellDef="let m">
                    <button mat-icon-button color="accent" (click)="exportMissionReport(m)"
                            [disabled]="exportingMissionId === m.id" title="Exporter le rapport PDF">
                      <mat-icon *ngIf="exportingMissionId !== m.id">picture_as_pdf</mat-icon>
                      <mat-icon *ngIf="exportingMissionId === m.id">hourglass_top</mat-icon>
                    </button>
                    <button mat-icon-button (click)="openMissionReports(m)" title="Historique des rapports PDF">
                      <mat-icon>history</mat-icon>
                    </button>
                    <button mat-icon-button color="primary" (click)="openAssignDialog(m)" title="Assigner agents/formulaires">
                      <mat-icon>group_add</mat-icon>
                    </button>
                    <button mat-icon-button color="warn" *ngIf="m.status !== 'CANCELLED' && m.status !== 'COMPLETED'"
                            (click)="onCancel(m)" title="Annuler la mission">
                      <mat-icon>cancel</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
              </table>
              <p *ngIf="!missions.length" class="empty-state">Aucune mission pour le moment</p>
            </mat-card-content>
          </mat-card>
        </mat-tab>

        <mat-tab label="Carte">
          <mat-card class="table-card">
            <mat-card-content>
              <app-map-view
                [markers]="missionMarkers"
                [defaultCenter]="[12.6392, -8.0029]"
                [defaultZoom]="6">
              </app-map-view>
              <p *ngIf="!missionMarkers.length" class="empty-state">
                Aucune mission géolocalisée
              </p>
            </mat-card-content>
          </mat-card>
        </mat-tab>

        <mat-tab label="Suivi">
          <mat-card class="table-card">
            <mat-card-content>
              <div class="progress-row header-row">
                <span class="col-name">Mission</span>
                <span class="col-objective">Objectif</span>
                <span class="col-received">Reçues</span>
                <span class="col-bar">Progression</span>
                <span class="col-agents">Agents</span>
              </div>
              <div class="progress-row" *ngFor="let p of missionsProgress">
                <span class="col-name font-semibold">{{ p.missionName }}</span>
                <span class="col-objective">
                  {{ p.expectedCollectesCount != null ? p.expectedCollectesCount : 'Non défini' }}
                </span>
                <span class="col-received">{{ p.receivedCollectesCount }}</span>
                <span class="col-bar">
                  <ng-container *ngIf="p.progressPercent != null; else noObjective">
                    <mat-progress-bar mode="determinate" [value]="p.progressPercent"
                      [color]="p.progressPercent >= 100 ? 'primary' : 'accent'">
                    </mat-progress-bar>
                    <span class="bar-label">{{ p.progressPercent | number:'1.0-0' }}%</span>
                  </ng-container>
                  <ng-template #noObjective>
                    <span class="text-gray-400">—</span>
                  </ng-template>
                </span>
                <span class="col-agents">{{ p.activeAgentsCount }} / {{ p.assignedAgentsCount }}</span>
              </div>
              <p *ngIf="!missionsProgress.length" class="empty-state">Aucune mission à suivre</p>
            </mat-card-content>
          </mat-card>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .header-actions {
      display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem;
      h2 { margin: 0; font-weight: 700; }
      .subtitle { margin: 0; color: #64748b; }
    }
    .table-card { border-radius: 12px; }
    .w-full { width: 100%; }
    .font-semibold { font-weight: 600; }
    .empty-state { text-align: center; color: #94a3b8; padding: 2rem; }
    .progress-row {
      display: grid;
      grid-template-columns: 2fr 1fr 1fr 2.5fr 1fr;
      align-items: center;
      gap: 1rem;
      padding: 0.75rem 0.5rem;
      border-bottom: 1px solid #f1f5f9;
      &.header-row {
        font-size: 0.8rem;
        text-transform: uppercase;
        color: #94a3b8;
        font-weight: 600;
        border-bottom: 2px solid #e2e8f0;
      }
      &:last-of-type { border-bottom: none; }
    }
    .col-bar {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      mat-progress-bar {
        flex: 1;
        height: 8px;
        border-radius: 4px;
      }
      .bar-label {
        flex: 0 0 40px;
        text-align: right;
        font-size: 0.85rem;
        font-weight: 600;
      }
    }
  `]
})
export class MissionListComponent implements OnInit {
  missions: MissionResponse[] = [];
  missionMarkers: MapMarker[] = [];
  missionsProgress: MissionProgressResponse[] = [];
  displayedColumns = ['name', 'status', 'dates', 'agents', 'forms', 'actions'];
  exportingMissionId: number | null = null;

  private allAgents: { id: number; firstName: string; lastName: string }[] = [];
  private allForms: { id: number; name: string }[] = [];
  private progressLoaded = false;

  constructor(
    private missionService: MissionService,
    private userService: UserService,
    private formService: FormService,
    private reportService: ReportService,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadMissions();
    this.userService.getUsers().subscribe({
      next: (users) => this.allAgents = users.filter(u => u.role === 'AGENT'),
      error: (err) => console.error(err)
    });
    this.formService.getPublishedForms().subscribe({
      next: (forms) => this.allForms = forms.map(f => ({ id: f.id, name: f.name })),
      error: (err) => console.error(err)
    });
  }

  loadMissions(): void {
    this.missionService.getMissions().subscribe({
      next: (data) => {
        this.missions = data;
        this.missionMarkers = this.buildMarkers(data);
      },
      error: (err) => console.error(err)
    });
  }

  loadProgress(): void {
    this.missionService.getMissionsProgress().subscribe({
      next: (data) => this.missionsProgress = data,
      error: (err) => console.error(err)
    });
  }

  private buildMarkers(missions: MissionResponse[]): MapMarker[] {
    return missions
      .filter(m => m.latitude != null && m.longitude != null)
      .map(m => ({
        id: m.id,
        lat: m.latitude!,
        lng: m.longitude!,
        label: `${m.name} — ${m.status}`,
        radiusMeters: m.radiusMeters,
        color: this.markerColor(m.status)
      }));
  }

  private markerColor(status: string): string {
    if (status === 'ACTIVE') return '#22c55e';
    if (status === 'CANCELLED') return '#ef4444';
    if (status === 'COMPLETED') return '#64748b';
    return '#3388ff'; // DRAFT / défaut
  }

  onTabChange(event: MatTabChangeEvent): void {
    if (event.tab.textLabel === 'Carte') {
      // Leaflet mesure le conteneur au moment de l'activation de l'onglet ;
      // sans ce coup de pouce, les tuiles restent grises tant qu'aucun
      // redimensionnement de fenêtre ne force Leaflet à se recalculer.
      setTimeout(() => window.dispatchEvent(new Event('resize')), 0);
    }
    if (event.tab.textLabel === 'Suivi' && !this.progressLoaded) {
      // Chargé à la demande, pas au démarrage du composant : évite un
      // appel réseau inutile si l'utilisateur ne consulte jamais cet onglet.
      this.progressLoaded = true;
      this.loadProgress();
    }
  }

  statusColor(status: string): 'primary' | 'accent' | 'warn' {
    if (status === 'ACTIVE') return 'accent';
    if (status === 'CANCELLED') return 'warn';
    return 'primary';
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(MissionDialogComponent, { width: '500px' });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.missionService.createMission(result).subscribe({
          next: () => {
            this.loadMissions();
            this.progressLoaded = false; // force le rechargement au prochain passage sur l'onglet Suivi
          },
          error: (err) => alert(err?.error?.message || 'Erreur lors de la création')
        });
      }
    });
  }

  openAssignDialog(mission: MissionResponse): void {
    const dialogRef = this.dialog.open(MissionAssignDialogComponent, {
      width: '500px',
      data: { mission, availableAgents: this.allAgents, availableForms: this.allForms }
    });
    dialogRef.afterClosed().subscribe(() => {
      this.loadMissions();
      this.progressLoaded = false;
    });
  }

  onCancel(mission: MissionResponse): void {
    const confirmed = confirm(`Voulez-vous vraiment annuler la mission "${mission.name}" ?`);
    if (!confirmed) return;
    this.missionService.cancelMission(mission.id).subscribe({
      next: () => this.loadMissions(),
      error: (err) => alert(err?.error?.message || "Erreur lors de l'annulation")
    });
  }

  exportMissionReport(mission: MissionResponse): void {
    this.exportingMissionId = mission.id;
    this.reportService.generateMissionReport(mission.id).subscribe({
      next: (report) => {
        this.reportService.downloadReport(report.id).subscribe({
          next: (blob) => {
            this.exportingMissionId = null;
            const filename = `rapport-mission-${mission.id}.pdf`;
            this.reportService.triggerBrowserDownload(blob, filename);
          },
          error: (err) => {
            this.exportingMissionId = null;
            alert(err?.error?.message || 'Erreur lors du téléchargement du rapport');
          }
        });
      },
      error: (err) => {
        this.exportingMissionId = null;
        alert(err?.error?.message || 'Erreur lors de la génération du rapport');
      }
    });
  }

  openMissionReports(mission: MissionResponse): void {
    this.dialog.open(ReportHistoryDialogComponent, {
      width: '750px',
      data: {
        title: `Mission ${mission.name}`,
        type: 'MISSION',
        targetId: mission.id,
        canGenerate: true
      }
    });
  }
}