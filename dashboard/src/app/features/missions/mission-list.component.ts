import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { MatTabsModule, MatTabChangeEvent } from '@angular/material/tabs';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MapViewComponent, MapMarker } from '../../shared/map-view/map-view.component';
import { MissionService } from '../../core/services/mission.service';
import {
  MissionResponse,
  MissionProgressResponse,
  MissionResponsePage,
  CreateMissionRequest,
  UpdateMissionRequest
} from '../../core/models/mission.model';
import { UserService } from '../../core/services/user.service';
import { FormService } from '../../core/services/form.service';
import { ReportService } from '../../core/services/report.service';
import { MissionFormDialogComponent } from './mission-form-dialog.component';
import { MissionAssignDialogComponent } from './mission-assign-dialog.component';
import { ReportHistoryDialogComponent } from '../reports/report-history-dialog.component';

@Component({
  selector: 'app-mission-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDialogModule,
    MatTabsModule,
    MatProgressBarModule,
    MatPaginatorModule,
    MatSnackBarModule,
    MatTooltipModule,
    MapViewComponent
  ],
  template: `
    <div class="mission-container">
      <div class="header-actions">
        <div>
          <h2>Gestion des Missions</h2>
          <p class="subtitle">Zones d'intervention géolocalisées, objectifs de collecte et affectations terrain</p>
        </div>
        <button mat-raised-button color="primary" (click)="openCreateDialog()">
          <mat-icon>add</mat-icon> Nouvelle Mission
        </button>
      </div>

      <mat-tab-group (selectedTabChange)="onTabChange($event)" animationDuration="200ms">
        <!-- ================= ONGLET 1 : LISTE DES MISSIONS ================= -->
        <mat-tab label="Liste des missions">
          <mat-card class="table-card">
            <mat-card-content>
              <table mat-table [dataSource]="missions" class="w-full">
                <!-- Nom & Description -->
                <ng-container matColumnDef="name">
                  <th mat-header-cell *matHeaderCellDef> Mission </th>
                  <td mat-cell *matCellDef="let m" class="mission-name-cell">
                    <div class="font-semibold text-slate-800">{{ m.name }}</div>
                    <div *ngIf="m.description" class="text-xs text-slate-500 text-truncate" [matTooltip]="m.description">
                      {{ m.description }}
                    </div>
                  </td>
                </ng-container>

                <!-- Statut -->
                <ng-container matColumnDef="status">
                  <th mat-header-cell *matHeaderCellDef> Statut </th>
                  <td mat-cell *matCellDef="let m">
                    <span class="status-badge" [ngClass]="m.status.toLowerCase()">
                      {{ formatStatus(m.status) }}
                    </span>
                  </td>
                </ng-container>

                <!-- Période / Échéances -->
                <ng-container matColumnDef="dates">
                  <th mat-header-cell *matHeaderCellDef> Échéances </th>
                  <td mat-cell *matCellDef="let m">
                    <span class="text-sm">
                      {{ m.startDate ? (m.startDate | date:'dd/MM/yyyy') : '—' }}
                      <span class="text-slate-400">→</span>
                      {{ m.endDate ? (m.endDate | date:'dd/MM/yyyy') : '—' }}
                    </span>
                  </td>
                </ng-container>

                <!-- Zone GPS -->
                <ng-container matColumnDef="zone">
                  <th mat-header-cell *matHeaderCellDef> Zone GPS </th>
                  <td mat-cell *matCellDef="let m">
                    <span *ngIf="m.latitude != null && m.longitude != null" class="gps-badge" matTooltip="Centre: {{ m.latitude }}, {{ m.longitude }} (Rayon: {{ m.radiusMeters || 'N/A' }} m)">
                      <mat-icon class="inline-icon">location_on</mat-icon>
                      {{ m.radiusMeters ? m.radiusMeters + ' m' : 'Point fixe' }}
                    </span>
                    <span *ngIf="m.latitude == null || m.longitude == null" class="text-slate-400 text-sm">
                      Non définie
                    </span>
                  </td>
                </ng-container>

                <!-- Affectations Agents & Formulaires -->
                <ng-container matColumnDef="assignments">
                  <th mat-header-cell *matHeaderCellDef> Affectations </th>
                  <td mat-cell *matCellDef="let m">
                    <div class="flex items-center gap-3">
                      <span class="counter-badge" matTooltip="{{ m.agents.length }} agent(s) assigné(s)">
                        <mat-icon class="mini-icon">group</mat-icon> {{ m.agents.length }}
                      </span>
                      <span class="counter-badge form-badge" matTooltip="{{ m.forms.length }} formulaire(s) lié(s)">
                        <mat-icon class="mini-icon">description</mat-icon> {{ m.forms.length }}
                      </span>
                    </div>
                  </td>
                </ng-container>

                <!-- Actions -->
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef class="text-right"> Actions </th>
                  <td mat-cell *matCellDef="let m" class="text-right actions-cell">
                    <!-- Édition -->
                    <button mat-icon-button color="primary" (click)="openEditDialog(m)" matTooltip="Modifier les détails de la mission">
                      <mat-icon>edit</mat-icon>
                    </button>

                    <!-- Assigner agents/formulaires -->
                    <button mat-icon-button color="accent" (click)="openAssignDialog(m)" matTooltip="Assigner des agents ou des formulaires">
                      <mat-icon>group_add</mat-icon>
                    </button>

                    <!-- Activer la mission (si DRAFT) -->
                    <button mat-icon-button class="activate-btn" *ngIf="m.status === 'DRAFT'"
                            [disabled]="activatingMissionId === m.id"
                            (click)="onActivate(m)" matTooltip="Activer la mission pour le terrain">
                      <mat-icon *ngIf="activatingMissionId !== m.id">play_circle</mat-icon>
                      <mat-icon *ngIf="activatingMissionId === m.id">hourglass_top</mat-icon>
                    </button>

                    <!-- Télécharger rapport PDF instantané -->
                    <button mat-icon-button class="pdf-btn" (click)="exportMissionReport(m)"
                            [disabled]="exportingMissionId === m.id" matTooltip="Générer et télécharger le rapport PDF">
                      <mat-icon *ngIf="exportingMissionId !== m.id">picture_as_pdf</mat-icon>
                      <mat-icon *ngIf="exportingMissionId === m.id">hourglass_top</mat-icon>
                    </button>

                    <!-- Historique des rapports PDF -->
                    <button mat-icon-button (click)="openMissionReports(m)" matTooltip="Consulter l'historique des rapports générés">
                      <mat-icon>history</mat-icon>
                    </button>

                    <!-- Annuler la mission -->
                    <button mat-icon-button color="warn" *ngIf="m.status !== 'CANCELLED' && m.status !== 'COMPLETED'"
                            (click)="onCancel(m)" matTooltip="Annuler cette mission">
                      <mat-icon>cancel</mat-icon>
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
              </table>

              <div *ngIf="!missions.length" class="empty-state">
                <mat-icon class="empty-icon">explore_off</mat-icon>
                <p>Aucune mission trouvée pour le moment.</p>
                <button mat-stroked-button color="primary" (click)="openCreateDialog()">Créer une mission</button>
              </div>

              <!-- Pagination serveur -->
              <mat-paginator
                [length]="totalElements"
                [pageIndex]="pageIndex"
                [pageSize]="pageSize"
                [pageSizeOptions]="[10, 25, 50]"
                showFirstLastButtons
                (page)="onPageChange($event)">
              </mat-paginator>
            </mat-card-content>
          </mat-card>
        </mat-tab>

        <!-- ================= ONGLET 2 : CARTE GPS ================= -->
        <mat-tab label="Cartographie GPS">
          <mat-card class="table-card">
            <mat-card-content>
              <div class="map-wrapper">
                <app-map-view
                  [markers]="missionMarkers"
                  [defaultCenter]="[12.6392, -8.0029]"
                  [defaultZoom]="6">
                </app-map-view>
              </div>
              <p *ngIf="!missionMarkers.length" class="empty-state">
                Aucune mission géolocalisée (coordonnées GPS non définies).
              </p>
            </mat-card-content>
          </mat-card>
        </mat-tab>

        <!-- ================= ONGLET 3 : SUIVI DES OBJECTIFS ================= -->
        <mat-tab label="Suivi de la progression">
          <mat-card class="table-card">
            <mat-card-content>
              <div class="progress-row header-row">
                <span class="col-name">Mission</span>
                <span class="col-objective">Objectif</span>
                <span class="col-received">Collectes Reçues</span>
                <span class="col-bar">Progression</span>
                <span class="col-agents">Agents Actifs</span>
              </div>
              <div class="progress-row" *ngFor="let p of missionsProgress">
                <span class="col-name font-semibold text-slate-800">{{ p.missionName }}</span>
                <span class="col-objective">
                  {{ p.expectedCollectesCount != null ? p.expectedCollectesCount : 'Non défini' }}
                </span>
                <span class="col-received font-semibold">{{ p.receivedCollectesCount }}</span>
                <span class="col-bar">
                  <ng-container *ngIf="p.progressPercent != null; else noObjective">
                    <mat-progress-bar mode="determinate" [value]="p.progressPercent"
                      [color]="p.progressPercent >= 100 ? 'primary' : 'accent'">
                    </mat-progress-bar>
                    <span class="bar-label">{{ p.progressPercent | number:'1.0-0' }}%</span>
                  </ng-container>
                  <ng-template #noObjective>
                    <span class="text-slate-400 italic">Pas d'objectif fixé</span>
                  </ng-template>
                </span>
                <span class="col-agents">
                  <span class="font-semibold text-emerald-600">{{ p.activeAgentsCount }}</span>
                  / {{ p.assignedAgentsCount }}
                </span>
              </div>
              <div *ngIf="!missionsProgress.length" class="empty-state">
                <mat-icon class="empty-icon">trending_up</mat-icon>
                <p>Aucune donnée de progression disponible.</p>
              </div>
            </mat-card-content>
          </mat-card>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .mission-container {
      padding: 1.5rem;
    }
    .header-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
      h2 { margin: 0; font-weight: 700; font-size: 1.5rem; color: #0f172a; }
      .subtitle { margin: 0.25rem 0 0 0; color: #64748b; font-size: 0.9rem; }
    }
    .table-card {
      border-radius: 12px;
      box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
      margin-top: 1rem;
    }
    .w-full { width: 100%; }
    .text-right { text-align: right; }
    .mission-name-cell {
      max-width: 240px;
    }
    .text-truncate {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .status-badge {
      display: inline-block;
      padding: 0.25rem 0.65rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
      &.draft { background-color: #f1f5f9; color: #475569; }
      &.active { background-color: #dcfce7; color: #166534; }
      &.completed { background-color: #e0e7ff; color: #3730a3; }
      &.cancelled { background-color: #fee2e2; color: #991b1b; }
    }
    .gps-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.2rem;
      padding: 0.2rem 0.5rem;
      background-color: #eff6ff;
      color: #1d4ed8;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: 500;
    }
    .inline-icon {
      font-size: 1rem;
      width: 1rem;
      height: 1rem;
    }
    .counter-badge {
      display: inline-flex;
      align-items: center;
      gap: 0.25rem;
      background-color: #f8fafc;
      border: 1px solid #e2e8f0;
      padding: 0.2rem 0.5rem;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: 600;
      color: #334155;
    }
    .form-badge {
      background-color: #faf5ff;
      border-color: #f3e8ff;
      color: #7e22ce;
    }
    .mini-icon {
      font-size: 1rem;
      width: 1rem;
      height: 1rem;
    }
    .actions-cell {
      white-space: nowrap;
      button { margin-left: 0.2rem; }
    }
    .activate-btn { color: #16a34a; }
    .pdf-btn { color: #ea580c; }
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem 1rem;
      color: #94a3b8;
      gap: 0.5rem;
      .empty-icon { font-size: 3rem; width: 3rem; height: 3rem; color: #cbd5e1; }
    }
    .map-wrapper {
      margin-top: 0.5rem;
      min-height: 480px;
    }
    .progress-row {
      display: grid;
      grid-template-columns: 2.2fr 1fr 1fr 2.5fr 1fr;
      align-items: center;
      gap: 1rem;
      padding: 0.85rem 0.5rem;
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
      gap: 0.75rem;
      mat-progress-bar {
        flex: 1;
        height: 8px;
        border-radius: 4px;
      }
      .bar-label {
        flex: 0 0 45px;
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
  displayedColumns = ['name', 'status', 'dates', 'zone', 'assignments', 'actions'];

  // Pagination serveur
  pageIndex = 0;
  pageSize = 25;
  totalElements = 0;

  exportingMissionId: number | null = null;
  activatingMissionId: number | null = null;

  private allAgents: { id: number; firstName: string; lastName: string; email?: string }[] = [];
  private allForms: { id: number; name: string }[] = [];
  private progressLoaded = false;

  private missionService = inject(MissionService);
  private userService = inject(UserService);
  private formService = inject(FormService);
  private reportService = inject(ReportService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  ngOnInit(): void {
    this.loadMissions();
    this.loadMetadata();
  }

  loadMetadata(): void {
    this.userService.getUsers(0, 500).subscribe({
      next: (page) => {
        this.allAgents = page.content.filter(u => u.role === 'AGENT');
      },
      error: (err) => console.error('Erreur chargement agents:', err)
    });

    this.formService.getPublishedForms().subscribe({
      next: (forms) => {
        this.allForms = forms.map(f => ({ id: f.id, name: f.name }));
      },
      error: (err) => console.error('Erreur chargement formulaires:', err)
    });
  }

  loadMissions(): void {
    this.missionService.getMissionsPaged(this.pageIndex, this.pageSize).subscribe({
      next: (page: MissionResponsePage) => {
        this.missions = page.content;
        this.totalElements = page.totalElements;
        this.missionMarkers = this.buildMarkers(page.content);
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Erreur lors du chargement des missions', 'Fermer', { duration: 4000 });
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadMissions();
  }

  loadProgress(): void {
    this.missionService.getMissionsProgress().subscribe({
      next: (data) => this.missionsProgress = data,
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Erreur chargement progression', 'Fermer', { duration: 4000 });
      }
    });
  }

  private buildMarkers(missions: MissionResponse[]): MapMarker[] {
    return missions
      .filter(m => m.latitude != null && m.longitude != null)
      .map(m => ({
        id: m.id,
        lat: m.latitude!,
        lng: m.longitude!,
        label: `${m.name} (${this.formatStatus(m.status)})`,
        radiusMeters: m.radiusMeters,
        color: this.markerColor(m.status)
      }));
  }

  private markerColor(status: string): string {
    if (status === 'ACTIVE') return '#22c55e';
    if (status === 'CANCELLED') return '#ef4444';
    if (status === 'COMPLETED') return '#6366f1';
    return '#3b82f6'; // DRAFT
  }

  formatStatus(status: string): string {
    switch (status) {
      case 'DRAFT': return 'Brouillon';
      case 'ACTIVE': return 'Active';
      case 'COMPLETED': return 'Terminée';
      case 'CANCELLED': return 'Annulée';
      default: return status;
    }
  }

  onTabChange(event: MatTabChangeEvent): void {
    if (event.tab.textLabel === 'Cartographie GPS') {
      setTimeout(() => window.dispatchEvent(new Event('resize')), 100);
    }
    if (event.tab.textLabel === 'Suivi de la progression' && !this.progressLoaded) {
      this.progressLoaded = true;
      this.loadProgress();
    }
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(MissionFormDialogComponent, {
      width: '560px'
    });

    dialogRef.afterClosed().subscribe((result: CreateMissionRequest | undefined) => {
      if (result) {
        this.missionService.createMission(result).subscribe({
          next: () => {
            this.snackBar.open('Mission créée avec succès', 'OK', { duration: 3000 });
            this.loadMissions();
            this.progressLoaded = false;
          },
          error: (err) => {
            this.snackBar.open(err?.error?.message || 'Erreur lors de la création de la mission', 'Fermer', { duration: 4000 });
          }
        });
      }
    });
  }

  openEditDialog(mission: MissionResponse): void {
    const dialogRef = this.dialog.open(MissionFormDialogComponent, {
      width: '560px',
      data: { mission }
    });

    dialogRef.afterClosed().subscribe((result: UpdateMissionRequest | undefined) => {
      if (result) {
        this.missionService.updateMission(mission.id, result).subscribe({
          next: () => {
            this.snackBar.open('Mission mise à jour avec succès', 'OK', { duration: 3000 });
            this.loadMissions();
            this.progressLoaded = false;
          },
          error: (err) => {
            this.snackBar.open(err?.error?.message || 'Erreur lors de la modification de la mission', 'Fermer', { duration: 4000 });
          }
        });
      }
    });
  }

  openAssignDialog(mission: MissionResponse): void {
    const dialogRef = this.dialog.open(MissionAssignDialogComponent, {
      width: '600px',
      data: {
        mission,
        availableAgents: this.allAgents,
        availableForms: this.allForms
      }
    });

    dialogRef.afterClosed().subscribe((updatedMission: MissionResponse | null) => {
      if (updatedMission) {
        this.loadMissions();
        this.progressLoaded = false;
      }
    });
  }

  onActivate(mission: MissionResponse): void {
    const confirmed = confirm(`Activer la mission "${mission.name}" ? Elle sera immédiatement disponible pour la collecte par les agents assignés.`);
    if (!confirmed) return;

    this.activatingMissionId = mission.id;
    this.missionService.updateMission(mission.id, {
      name: mission.name,
      description: mission.description,
      status: 'ACTIVE',
      startDate: mission.startDate,
      endDate: mission.endDate,
      latitude: mission.latitude,
      longitude: mission.longitude,
      radiusMeters: mission.radiusMeters,
      expectedCollectesCount: mission.expectedCollectesCount
    }).subscribe({
      next: () => {
        this.activatingMissionId = null;
        this.snackBar.open('Mission activée', 'OK', { duration: 3000 });
        this.loadMissions();
        this.progressLoaded = false;
      },
      error: (err) => {
        this.activatingMissionId = null;
        this.snackBar.open(err?.error?.message || "Erreur lors de l'activation de la mission", 'Fermer', { duration: 4000 });
      }
    });
  }

  onCancel(mission: MissionResponse): void {
    const confirmed = confirm(`Voulez-vous vraiment annuler la mission "${mission.name}" ? Les agents ne pourront plus soumettre de collectes sur cette mission.`);
    if (!confirmed) return;

    this.missionService.cancelMission(mission.id).subscribe({
      next: () => {
        this.snackBar.open('Mission annulée', 'OK', { duration: 3000 });
        this.loadMissions();
        this.progressLoaded = false;
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || "Erreur lors de l'annulation", 'Fermer', { duration: 4000 });
      }
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
            this.snackBar.open('Rapport PDF téléchargé', 'OK', { duration: 3000 });
          },
          error: (err) => {
            this.exportingMissionId = null;
            this.snackBar.open(err?.error?.message || 'Erreur téléchargement du rapport', 'Fermer', { duration: 4000 });
          }
        });
      },
      error: (err) => {
        this.exportingMissionId = null;
        this.snackBar.open(err?.error?.message || 'Erreur génération du rapport', 'Fermer', { duration: 4000 });
      }
    });
  }

  openMissionReports(mission: MissionResponse): void {
    this.dialog.open(ReportHistoryDialogComponent, {
      width: '750px',
      data: {
        title: `Mission : ${mission.name}`,
        type: 'MISSION',
        targetId: mission.id,
        canGenerate: true
      }
    });
  }
}