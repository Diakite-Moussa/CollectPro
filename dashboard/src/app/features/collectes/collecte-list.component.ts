import { Component, OnInit, OnDestroy, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule, MatTabChangeEvent } from '@angular/material/tabs';
import { MapViewComponent, MapMarker } from '../../shared/map-view/map-view.component';
import { CollecteService } from '../../core/services/collecte.service';
import { AuthService } from '../../core/services/auth.service';
import { FileService } from '../../core/services/file.service';
import { FormService } from '../../core/services/form.service';
import { CollecteResponse } from '../../core/models/collecte.model';

@Component({
  selector: 'app-collecte-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <h2 mat-dialog-title>Fiche de Collecte #{{ data.id }}</h2>
    <mat-dialog-content class="dialog-content">
      <div class="meta-info">
        <p><strong>Agent :</strong> {{ data.agent.firstName }} {{ data.agent.lastName }}</p>
        <p><strong>Date de soumission :</strong> {{ data.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</p>
        <p *ngIf="data.latitude && data.longitude">
          <strong>Position GPS :</strong>
          <span class="gps-tag"><mat-icon inline>location_on</mat-icon> {{ data.latitude }}, {{ data.longitude }}</span>
        </p>
      </div>

      <div *ngIf="data.status !== 'PENDING_VALIDATION'" class="validation-info" [class.rejected]="data.status === 'REJECTED'">
        <p>
          <mat-icon inline>{{ data.status === 'VALIDATED' ? 'check_circle' : 'cancel' }}</mat-icon>
          <strong>{{ data.status === 'VALIDATED' ? 'Validée' : 'Rejetée' }}</strong>
          par {{ data.validatedBy?.firstName }} {{ data.validatedBy?.lastName }}
          le {{ data.validatedAt | date:'dd/MM/yyyy HH:mm' }}
        </p>
        <p *ngIf="data.validationComment" class="comment-box">« {{ data.validationComment }} »</p>
      </div>

      <hr class="my-3">

      <h3>Données collectées</h3>
      <div *ngIf="!fieldLabels" class="loading-fields">Chargement du formulaire…</div>
      <table *ngIf="fieldLabels" class="data-table">
        <tr *ngFor="let entry of dataEntries">
          <td class="data-label">{{ fieldLabels.get(entry.key) || entry.key }}</td>
          <td class="data-value">{{ formatValue(entry.value) }}</td>
        </tr>
      </table>

      <div *ngIf="data.attachments?.length" class="photos-section">
        <h3>Pièces jointes ({{ data.attachments!.length }})</h3>
        <div class="photos-grid">
          <ng-container *ngFor="let att of data.attachments">
            <a *ngIf="attachmentBlobUrls.get(att.id) as blobUrl" [href]="blobUrl" [download]="att.originalFilename" target="_blank" rel="noopener">
              <img *ngIf="isImageAttachment(att)" [src]="blobUrl" alt="Pièce jointe" class="photo-thumb">
              <div *ngIf="!isImageAttachment(att)" class="file-link">
                <mat-icon>attach_file</mat-icon>
                <span>{{ att.originalFilename }}</span>
              </div>
            </a>
            <div *ngIf="!attachmentBlobUrls.get(att.id)" class="file-link loading">
              <mat-progress-spinner diameter="24" mode="indeterminate"></mat-progress-spinner>
            </div>
          </ng-container>
        </div>
      </div>

      <div *ngIf="legacyPhotos.length > 0" class="photos-section">
        <h3>Photos legacy ({{ legacyPhotos.length }})</h3>
        <div class="photos-grid">
          <img *ngFor="let photo of legacyPhotos" [src]="photo" alt="Photo collecte" class="photo-thumb">
        </div>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Fermer</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content { width: 500px; max-width: 90vw; }
    .meta-info p { margin: 0.3rem 0; font-size: 0.9rem; }
    .gps-tag { background: #eff6ff; color: #2563eb; padding: 2px 6px; border-radius: 4px; }
    .my-3 { margin: 0.8rem 0; }
    .loading-fields { color: #94a3b8; font-size: 0.85rem; font-style: italic; }
    .data-table { width: 100%; border-collapse: collapse; }
    .data-table td { padding: 6px 8px; border-bottom: 1px solid #e2e8f0; font-size: 0.9rem; vertical-align: top; }
    .data-label { font-weight: 600; color: #334155; width: 40%; }
    .data-value { color: #1e293b; word-break: break-word; }
    .photos-grid { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.5rem; }
    .photo-thumb { width: 100px; height: 100px; object-fit: cover; border-radius: 8px; border: 1px solid #cbd5e1; }
    .file-link {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      width: 100px; height: 100px; border: 1px solid #cbd5e1; border-radius: 8px;
      font-size: 0.7rem; color: #475569; text-decoration: none; padding: 4px;
    }
    .file-link.loading { border-style: dashed; }
    .validation-info { background: #f0fdf4; color: #166534; padding: 0.6rem 0.8rem; border-radius: 8px; margin-top: 0.8rem; font-size: 0.85rem; }
    .validation-info.rejected { background: #fef2f2; color: #991b1b; }
    .validation-info p { margin: 0.2rem 0; display: flex; align-items: center; gap: 4px; }
    .comment-box { font-style: italic; margin-left: 22px !important; }
  `]
})
export class CollecteDetailDialogComponent implements OnDestroy {
  legacyPhotos: string[] = [];
  attachmentBlobUrls = new Map<number, string>();
  fieldLabels: Map<string, string> | null = null;
  dataEntries: { key: string; value: unknown }[] = [];

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: CollecteResponse,
    private fileService: FileService,
    private formService: FormService
  ) {
    if (data.attachments?.length) {
      for (const att of data.attachments) {
        this.fileService.downloadFile(att.url).subscribe({
          next: (blob: Blob) => this.attachmentBlobUrls.set(att.id, URL.createObjectURL(blob)),
          error: (err: unknown) => console.error(`Échec chargement pièce jointe #${att.id}`, err)
        });
      }
    }

    try {
      const parsed = JSON.parse(data.dataJson);
      if (parsed._photos && Array.isArray(parsed._photos)) {
        this.legacyPhotos = parsed._photos;
        delete parsed._photos;
      }
      this.dataEntries = Object.entries(parsed).map(([key, value]) => ({ key, value }));
    } catch {
      this.dataEntries = [];
    }

    this.formService.getFormVersionById(data.formVersionId).subscribe({
      next: (version) => {
        try {
          const schema = JSON.parse(version.schemaJson);
          const fields: { key: string; label: string }[] = schema.fields ?? [];
          this.fieldLabels = new Map(fields.map((f) => [f.key, f.label]));
        } catch {
          this.fieldLabels = new Map();
        }
      },
      error: () => {
        // formulaire supprimé ou inaccessible : on retombe sur les clés brutes
        this.fieldLabels = new Map();
      }
    });
  }

  formatValue(value: unknown): string {
    if (value === null || value === undefined || value === '') return '—';
    if (Array.isArray(value)) return value.join(', ');
    return String(value);
  }

  isImageAttachment(att: { contentType?: string; originalFilename: string }): boolean {
    return (att.contentType?.startsWith('image/'))
      || /\.(jpg|jpeg|png|gif|webp)$/i.test(att.originalFilename);
  }

  ngOnDestroy(): void {
    for (const url of this.attachmentBlobUrls.values()) {
      URL.revokeObjectURL(url);
    }
  }
}

@Component({
  selector: 'app-reject-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <h2 mat-dialog-title>Rejeter la collecte #{{ data.id }}</h2>
    <mat-dialog-content class="dialog-content">
      <p>Un commentaire est <strong>obligatoire</strong> pour expliquer le rejet à l'agent {{ data.agent.firstName }}.</p>
      <mat-form-field appearance="outline" class="w-full">
        <mat-label>Motif du rejet</mat-label>
        <textarea matInput rows="3" [(ngModel)]="comment" placeholder="Ex: Photo manquante, données incohérentes..."></textarea>
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">Annuler</button>
      <button mat-flat-button color="warn" [disabled]="!comment.trim()" (click)="dialogRef.close(comment.trim())">
        Confirmer le rejet
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-content { width: 420px; max-width: 90vw; }
    .w-full { width: 100%; margin-top: 0.5rem; }
  `]
})
export class RejectDialogComponent {
  comment = '';
  constructor(
    public dialogRef: MatDialogRef<RejectDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CollecteResponse
  ) {}
}

@Component({
  selector: 'app-collecte-list',
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
    MapViewComponent
  ],
  template: `
    <div class="collecte-container">
      <div class="header-actions">
        <div>
          <h2>Collectes Terrain Synchronisées</h2>
          <p class="subtitle">Consulter et valider les soumissions reçues des agents terrain</p>
        </div>
        <div class="export-actions">
          <button mat-stroked-button color="primary" (click)="exportCsv()" [disabled]="isExportingCsv">
            <mat-icon *ngIf="!isExportingCsv">description</mat-icon>
            <mat-icon *ngIf="isExportingCsv">hourglass_top</mat-icon>
            Exporter CSV
          </button>
          <button mat-raised-button color="primary" (click)="exportExcel()" [disabled]="isExportingExcel">
            <mat-icon *ngIf="!isExportingExcel">table_view</mat-icon>
            <mat-icon *ngIf="isExportingExcel">hourglass_top</mat-icon>
            Exporter Excel (.xlsx)
          </button>
        </div>
      </div>

      <mat-tab-group (selectedTabChange)="onTabChange($event)">
        <mat-tab label="Liste">
      <mat-card class="table-card">
        <mat-card-content>
          <table mat-table [dataSource]="collectes" class="w-full">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> ID </th>
              <td mat-cell *matCellDef="let row"> #{{ row.id }} </td>
            </ng-container>

            <ng-container matColumnDef="agent">
              <th mat-header-cell *matHeaderCellDef> Agent </th>
              <td mat-cell *matCellDef="let row" class="font-semibold"> {{ row.agent.firstName }} {{ row.agent.lastName }} </td>
            </ng-container>

            <ng-container matColumnDef="coords">
              <th mat-header-cell *matHeaderCellDef> Coordonnées GPS </th>
              <td mat-cell *matCellDef="let row">
                <span *ngIf="row.latitude && row.longitude" class="gps-badge">
                  <mat-icon inline>location_on</mat-icon> {{ row.latitude | number:'1.4-4' }}, {{ row.longitude | number:'1.4-4' }}
                </span>
                <span *ngIf="!row.latitude" class="text-gray-400">Non disponible</span>
                <mat-icon *ngIf="row.outsideMissionZone" inline color="warn" class="alert-icon" title="Collecte hors de la zone de mission">
                  warning
                </mat-icon>
              </td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef> Statut </th>
              <td mat-cell *matCellDef="let row">
                <mat-chip-option [selectable]="false" [color]="getStatusColor(row.status)">
                  {{ row.status }}
                </mat-chip-option>
              </td>
            </ng-container>

            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef> Reçu le </th>
              <td mat-cell *matCellDef="let row"> {{ row.createdAt | date:'dd/MM/yyyy HH:mm' }} </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Action </th>
              <td mat-cell *matCellDef="let row">
                <button mat-icon-button color="primary" (click)="openDetail(row)" title="Voir détails">
                  <mat-icon>visibility</mat-icon>
                </button>
                <ng-container *ngIf="isSupervisor && row.status === 'PENDING_VALIDATION'">
                  <button mat-icon-button color="accent" (click)="validate(row)" title="Valider" [disabled]="processingId === row.id">
                    <mat-icon>check_circle</mat-icon>
                  </button>
                  <button mat-icon-button color="warn" (click)="openReject(row)" title="Rejeter" [disabled]="processingId === row.id">
                    <mat-icon>cancel</mat-icon>
                  </button>
                </ng-container>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
        </mat-tab>

        <mat-tab label="Carte">
          <mat-card class="table-card">
            <mat-card-content>
              <app-map-view
                [markers]="collecteMarkers"
                [defaultCenter]="[12.6392, -8.0029]"
                [defaultZoom]="6">
              </app-map-view>
              <p *ngIf="!collecteMarkers.length" class="empty-state">
                Aucune collecte géolocalisée
              </p>
            </mat-card-content>
          </mat-card>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
  styles: [`
    .header-actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
      gap: 1rem;
      h2 { margin: 0; font-weight: 700; }
      .subtitle { margin: 0; color: #64748b; }
    }
    .export-actions {
      display: flex;
      gap: 0.75rem;
      align-items: center;
    }
    .table-card { border-radius: 12px; }
    .w-full { width: 100%; }
    .font-semibold { font-weight: 600; }
    .gps-badge { display: inline-flex; align-items: center; gap: 2px; background: #eff6ff; color: #2563eb; padding: 2px 8px; border-radius: 6px; font-size: 0.8rem; }
    .alert-icon { margin-left: 4px; vertical-align: middle; }
    .empty-state { text-align: center; color: #94a3b8; padding: 2rem; }
  `]
})
export class CollecteListComponent implements OnInit {
  collectes: CollecteResponse[] = [];
  collecteMarkers: MapMarker[] = [];
  displayedColumns = ['id', 'agent', 'coords', 'status', 'createdAt', 'actions'];
  processingId: number | null = null;
  isSupervisor = false;
  isExportingCsv = false;
  isExportingExcel = false;

  constructor(
    private collecteService: CollecteService,
    private authService: AuthService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.isSupervisor = this.authService.currentUser()?.role === 'SUPERVISOR';
    this.loadCollectes();
  }

  loadCollectes(): void {
    const request$ = this.isSupervisor
      ? this.collecteService.getTeamCollectes()
      : this.collecteService.getOrganizationCollectes();
    request$.subscribe({
      next: (data) => {
        this.collectes = data;
        this.collecteMarkers = this.buildMarkers(data);
      },
      error: (err) => console.error(err)
    });
  }

  private buildMarkers(collectes: CollecteResponse[]): MapMarker[] {
    return collectes
      .filter(c => c.latitude != null && c.longitude != null)
      .map(c => ({
        id: c.id,
        lat: c.latitude!,
        lng: c.longitude!,
        label: this.buildMarkerLabel(c),
        color: c.outsideMissionZone ? '#dc2626' : this.markerColor(c.status)
      }));
  }

  private buildMarkerLabel(c: CollecteResponse): string {
    const base = `#${c.id} — ${c.agent.firstName} ${c.agent.lastName} — ${c.status}`;
    return c.outsideMissionZone ? `${base} ⚠️ Hors zone de mission` : base;
  }

  private markerColor(status: string): string {
    if (status === 'VALIDATED') return '#22c55e';
    if (status === 'REJECTED') return '#ef4444';
    return '#f59e0b'; // PENDING_VALIDATION
  }

  onTabChange(event: MatTabChangeEvent): void {
    if (event.tab.textLabel === 'Carte') {
      setTimeout(() => window.dispatchEvent(new Event('resize')), 0);
    }
  }

  openDetail(collecte: CollecteResponse): void {
    this.dialog.open(CollecteDetailDialogComponent, {
      data: collecte
    });
  }

  validate(collecte: CollecteResponse): void {
    if (!confirm(`Valider la collecte #${collecte.id} de ${collecte.agent.firstName} ${collecte.agent.lastName} ?`)) {
      return;
    }
    this.processingId = collecte.id;
    this.collecteService.validateCollecte(collecte.id).subscribe({
      next: () => { this.processingId = null; this.loadCollectes(); },
      error: (err) => {
        this.processingId = null;
        alert(err?.error?.message || 'Erreur lors de la validation');
      }
    });
  }

  openReject(collecte: CollecteResponse): void {
    const dialogRef = this.dialog.open(RejectDialogComponent, { data: collecte });
    dialogRef.afterClosed().subscribe((comment: string | undefined) => {
      if (!comment) return;
      this.processingId = collecte.id;
      this.collecteService.rejectCollecte(collecte.id, comment).subscribe({
        next: () => { this.processingId = null; this.loadCollectes(); },
        error: (err) => {
          this.processingId = null;
          alert(err?.error?.message || err?.error?.fieldErrors?.comment || 'Erreur lors du rejet');
        }
      });
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'VALIDATED': return 'accent';
      case 'REJECTED': return 'warn';
      default: return 'primary';
    }
  }

  exportCsv(): void {
    this.isExportingCsv = true;
    this.collecteService.exportCsv().subscribe({
      next: (blob) => {
        this.isExportingCsv = false;
        const dateStr = new Date().toISOString().slice(0, 10);
        this.collecteService.triggerBrowserDownload(blob, `collectes_export_${dateStr}.csv`);
      },
      error: (err) => {
        this.isExportingCsv = false;
        alert(err?.error?.message || "Erreur lors de l'export CSV");
      }
    });
  }

  exportExcel(): void {
    this.isExportingExcel = true;
    this.collecteService.exportExcel().subscribe({
      next: (blob) => {
        this.isExportingExcel = false;
        const dateStr = new Date().toISOString().slice(0, 10);
        this.collecteService.triggerBrowserDownload(blob, `collectes_export_${dateStr}.xlsx`);
      },
      error: (err) => {
        this.isExportingExcel = false;
        alert(err?.error?.message || "Erreur lors de l'export Excel");
      }
    });
  }
}
