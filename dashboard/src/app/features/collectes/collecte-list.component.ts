import { Component, OnInit, Inject } from '@angular/core';
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
import { CollecteService } from '../../core/services/collecte.service';
import { AuthService } from '../../core/services/auth.service';
import { CollecteResponse } from '../../core/models/collecte.model';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-collecte-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
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

      <h3>Données collectées (JSON)</h3>
      <pre class="json-box">{{ formattedJson }}</pre>

      <div *ngIf="attachmentUrls.length > 0" class="photos-section">
        <h3>Pièces jointes ({{ attachmentUrls.length }})</h3>
        <div class="photos-grid">
          <a *ngFor="let url of attachmentUrls" [href]="fileBaseUrl + url" target="_blank" rel="noopener">
            <img *ngIf="isImageUrl(url)" [src]="fileBaseUrl + url" alt="Pièce jointe" class="photo-thumb">
            <div *ngIf="!isImageUrl(url)" class="file-link">
              <mat-icon>attach_file</mat-icon>
              <span>{{ url.split('/').pop() }}</span>
            </div>
          </a>
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
    .json-box { background: #0f172a; color: #38bdf8; padding: 1rem; border-radius: 8px; font-size: 0.85rem; max-height: 250px; overflow-y: auto; }
    .photos-grid { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-top: 0.5rem; }
    .photo-thumb { width: 100px; height: 100px; object-fit: cover; border-radius: 8px; border: 1px solid #cbd5e1; }
    .file-link {
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      width: 100px; height: 100px; border: 1px solid #cbd5e1; border-radius: 8px;
      font-size: 0.7rem; color: #475569; text-decoration: none; padding: 4px;
    }
    .validation-info { background: #f0fdf4; color: #166534; padding: 0.6rem 0.8rem; border-radius: 8px; margin-top: 0.8rem; font-size: 0.85rem; }
    .validation-info.rejected { background: #fef2f2; color: #991b1b; }
    .validation-info p { margin: 0.2rem 0; display: flex; align-items: center; gap: 4px; }
    .comment-box { font-style: italic; margin-left: 22px !important; }
  `]
})
export class CollecteDetailDialogComponent {
  formattedJson: string = '';
  attachmentUrls: string[] = [];
  legacyPhotos: string[] = [];
  readonly fileBaseUrl = environment.apiUrl;

  constructor(@Inject(MAT_DIALOG_DATA) public data: CollecteResponse) {
    if (data.attachments?.length) {
      this.attachmentUrls = data.attachments.map((a) => a.url);
    }
    try {
      const parsed = JSON.parse(data.dataJson);
      if (parsed._photos && Array.isArray(parsed._photos)) {
        this.legacyPhotos = parsed._photos;
        delete parsed._photos;
      }
      this.formattedJson = JSON.stringify(parsed, null, 2);
    } catch {
      this.formattedJson = data.dataJson;
    }
  }

  isImageUrl(url: string): boolean {
    return url.includes('image') || /\.(jpg|jpeg|png|gif|webp)$/i.test(url);
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
    MatDialogModule
  ],
  template: `
    <div class="collecte-container">
      <div class="header-actions">
        <div>
          <h2>Collectes Terrain Synchronisées</h2>
          <p class="subtitle">Consulter et valider les soumissions reçues des agents terrain</p>
        </div>
      </div>

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
    </div>
  `,
  styles: [`
    .header-actions { margin-bottom: 1.5rem; h2 { margin: 0; font-weight: 700; } .subtitle { margin: 0; color: #64748b; } }
    .table-card { border-radius: 12px; }
    .w-full { width: 100%; }
    .font-semibold { font-weight: 600; }
    .gps-badge { display: inline-flex; align-items: center; gap: 2px; background: #eff6ff; color: #2563eb; padding: 2px 8px; border-radius: 6px; font-size: 0.8rem; }
  `]
})
export class CollecteListComponent implements OnInit {
  collectes: CollecteResponse[] = [];
  displayedColumns = ['id', 'agent', 'coords', 'status', 'createdAt', 'actions'];
  processingId: number | null = null;
  isSupervisor = false;

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
      next: (data) => this.collectes = data,
      error: (err) => console.error(err)
    });
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
}
