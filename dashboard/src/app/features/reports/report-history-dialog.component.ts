import { Component, OnInit, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ReportService } from '../../core/services/report.service';
import { ReportResponse } from '../../core/models/report.model';

export interface ReportHistoryDialogData {
  title: string;
  type: 'ORGANIZATION' | 'MISSION';
  targetId: number;
  canGenerate?: boolean;
}

@Component({
  selector: 'app-report-history-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatChipsModule,
    MatProgressBarModule
  ],
  template: `
    <div class="report-dialog-container">
      <div class="dialog-header">
        <h2 mat-dialog-title class="dialog-title">
          <mat-icon color="primary" class="header-icon">analytics</mat-icon>
          Rapports & Exports PDF — {{ data.title }}
        </h2>
      </div>

      <mat-dialog-content>
        <!-- Formulaire de génération avec filtre par période -->
        <div class="generate-box" *ngIf="data.canGenerate !== false">
          <div class="box-title">
            <mat-icon>add_circle_outline</mat-icon>
            <span>Générer un nouveau rapport PDF</span>
          </div>

          <form [formGroup]="rangeForm" class="filter-form">
            <div class="form-row">
              <mat-form-field appearance="outline">
                <mat-label>Date de début (optionnel)</mat-label>
                <input matInput [matDatepicker]="startPicker" formControlName="periodStart">
                <mat-datepicker-toggle matIconSuffix [for]="startPicker"></mat-datepicker-toggle>
                <mat-datepicker #startPicker></mat-datepicker>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Date de fin (optionnel)</mat-label>
                <input matInput [matDatepicker]="endPicker" formControlName="periodEnd">
                <mat-datepicker-toggle matIconSuffix [for]="endPicker"></mat-datepicker-toggle>
                <mat-datepicker #endPicker></mat-datepicker>
              </mat-form-field>

              <button mat-raised-button color="primary" class="generate-btn"
                      [disabled]="isGenerating" (click)="onGenerate()">
                <mat-icon *ngIf="!isGenerating">picture_as_pdf</mat-icon>
                <mat-icon *ngIf="isGenerating" class="animate-spin">hourglass_top</mat-icon>
                {{ isGenerating ? 'Génération...' : 'Générer & Télécharger' }}
              </button>
            </div>
            <p class="hint-text">
              * Laissez les dates vides pour agréger toutes les collectes disponibles.
            </p>
          </form>
        </div>

        <mat-progress-bar *ngIf="isLoading" mode="indeterminate" class="mb-4"></mat-progress-bar>

        <!-- Tableau d'historique -->
        <div class="history-section">
          <h3 class="section-title">Historique des rapports générés</h3>

          <table mat-table [dataSource]="reports" class="w-full report-table" *ngIf="reports.length">
            <ng-container matColumnDef="date">
              <th mat-header-cell *matHeaderCellDef> Date Génération </th>
              <td mat-cell *matCellDef="let r">
                {{ r.generatedAt | date:'dd/MM/yyyy HH:mm' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="period">
              <th mat-header-cell *matHeaderCellDef> Période </th>
              <td mat-cell *matCellDef="let r">
                <span *ngIf="r.periodStart || r.periodEnd">
                  {{ r.periodStart ? (r.periodStart | date:'dd/MM/yyyy') : 'Début' }}
                  →
                  {{ r.periodEnd ? (r.periodEnd | date:'dd/MM/yyyy') : 'Fin' }}
                </span>
                <span *ngIf="!r.periodStart && !r.periodEnd" class="text-gray-400">
                  Toutes données
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="author">
              <th mat-header-cell *matHeaderCellDef> Généré par </th>
              <td mat-cell *matCellDef="let r">
                {{ r.generatedBy ? (r.generatedBy.firstName + ' ' + r.generatedBy.lastName) : 'Système' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef> Action </th>
              <td mat-cell *matCellDef="let r">
                <button mat-flat-button color="accent" size="small"
                        (click)="onDownload(r)"
                        [disabled]="downloadingId === r.id">
                  <mat-icon>download</mat-icon>
                  {{ downloadingId === r.id ? 'Téléchargement...' : 'Télécharger' }}
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <div *ngIf="!isLoading && !reports.length" class="empty-state">
            <mat-icon class="empty-icon">description</mat-icon>
            <p>Aucun rapport généré pour le moment.</p>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close>Fermer</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .report-dialog-container {
      min-width: 600px;
      max-width: 800px;
      padding: 0.5rem;
    }
    .dialog-header {
      margin-bottom: 0.5rem;
    }
    .dialog-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.25rem;
      font-weight: 700;
      margin: 0;
    }
    .generate-box {
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      padding: 1rem;
      margin-bottom: 1.5rem;
    }
    .box-title {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-weight: 600;
      color: #1e293b;
      margin-bottom: 0.75rem;
    }
    .form-row {
      display: flex;
      gap: 0.75rem;
      align-items: center;
      flex-wrap: wrap;
    }
    .form-row mat-form-field {
      flex: 1;
      min-width: 160px;
    }
    .generate-btn {
      height: 52px;
      margin-bottom: 22px;
      font-weight: 600;
    }
    .hint-text {
      font-size: 0.75rem;
      color: #64748b;
      margin: -10px 0 0 0;
    }
    .history-section {
      margin-top: 1rem;
    }
    .section-title {
      font-size: 1rem;
      font-weight: 600;
      color: #334155;
      margin-bottom: 0.75rem;
    }
    .report-table {
      background: white;
      border-radius: 8px;
      overflow: hidden;
    }
    .empty-state {
      text-align: center;
      padding: 2rem;
      color: #94a3b8;
    }
    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      margin-bottom: 0.5rem;
    }
  `]
})
export class ReportHistoryDialogComponent implements OnInit {
  reports: ReportResponse[] = [];
  displayedColumns = ['date', 'period', 'author', 'actions'];
  rangeForm: FormGroup;
  isLoading = false;
  isGenerating = false;
  downloadingId: number | null = null;

  private reportService = inject(ReportService);
  private fb = inject(FormBuilder);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: ReportHistoryDialogData,
    private dialogRef: MatDialogRef<ReportHistoryDialogComponent>
  ) {
    this.rangeForm = this.fb.group({
      periodStart: [null],
      periodEnd: [null]
    });
  }

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.isLoading = true;
    const req$ = this.data.type === 'MISSION'
      ? this.reportService.getMissionReports(this.data.targetId)
      : this.reportService.getOrganizationReports(this.data.targetId);

    req$.subscribe({
      next: (page) => {
        this.reports = page.content;
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  onGenerate(): void {
    this.isGenerating = true;
    const startVal = this.rangeForm.value.periodStart ? new Date(this.rangeForm.value.periodStart).toISOString() : null;
    const endVal = this.rangeForm.value.periodEnd ? new Date(this.rangeForm.value.periodEnd).toISOString() : null;

    const requestPayload = {
      periodStart: startVal,
      periodEnd: endVal
    };

    const req$ = this.data.type === 'MISSION'
      ? this.reportService.generateMissionReport(this.data.targetId, requestPayload)
      : this.reportService.generateOrganizationReport(this.data.targetId, requestPayload);

    req$.subscribe({
      next: (created) => {
        this.loadReports();
        this.onDownload(created);
        this.isGenerating = false;
      },
      error: (err) => {
        this.isGenerating = false;
        alert(err?.error?.message || 'Erreur lors de la génération du rapport');
      }
    });
  }

  onDownload(report: ReportResponse): void {
    this.downloadingId = report.id;
    this.reportService.downloadReport(report.id).subscribe({
      next: (blob) => {
        this.downloadingId = null;
        const prefix = report.type === 'MISSION' ? `rapport-mission-${report.missionId}` : `rapport-org-${report.organizationId}`;
        const filename = `${prefix}-${report.id}.pdf`;
        this.reportService.triggerBrowserDownload(blob, filename);
      },
      error: (err) => {
        this.downloadingId = null;
        alert(err?.error?.message || 'Erreur lors du téléchargement du rapport');
      }
    });
  }
}
