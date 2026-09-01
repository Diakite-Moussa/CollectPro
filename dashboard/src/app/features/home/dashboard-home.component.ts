import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule, MatButtonToggleChange } from '@angular/material/button-toggle';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { StatisticsService } from '../../core/services/statistics.service';
import { CollecteService } from '../../core/services/collecte.service';
import { CollecteResponse } from '../../core/models/collecte.model';
import { AgentRejectionRate } from '../../core/models/statistics.model';
import { AuthService } from '../../core/services/auth.service';
import { ReportHistoryDialogComponent } from '../reports/report-history-dialog.component';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatDialogModule,
    BaseChartDirective
  ],
  template: `
    <div class="dashboard-home">
      <div class="welcome-header">
        <div>
          <h2>Tableau de bord — Vue Générale</h2>
          <p>Bienvenue {{ user()?.firstName }} ({{ user()?.organizationName || 'Super Admin' }})</p>
        </div>
        <button *ngIf="user()?.organizationId" mat-raised-button color="primary" (click)="openOrgReports()">
          <mat-icon>analytics</mat-icon> Rapports PDF
        </button>
      </div>

      <div class="stats-grid">
        <mat-card class="stat-card blue">
          <mat-card-content class="stat-content">
            <div class="stat-icon"><mat-icon>corporate_fare</mat-icon></div>
            <div class="stat-info">
              <span class="stat-value">{{ orgCount }}</span>
              <span class="stat-label">{{ isSuperAdmin ? 'Organisations Actives' : 'Membres Équipe' }}</span>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card green">
          <mat-card-content class="stat-content">
            <div class="stat-icon"><mat-icon>dynamic_form</mat-icon></div>
            <div class="stat-info">
              <span class="stat-value">{{ formCount }}</span>
              <span class="stat-label">Formulaires Publiés</span>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card purple">
          <mat-card-content class="stat-content">
            <div class="stat-icon"><mat-icon>sync_alt</mat-icon></div>
            <div class="stat-info">
              <span class="stat-value">{{ collecteCount }}</span>
              <span class="stat-label">Collectes Total</span>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card orange">
          <mat-card-content class="stat-content">
            <div class="stat-icon"><mat-icon>pending_actions</mat-icon></div>
            <div class="stat-info">
              <span class="stat-value">{{ pendingCount }}</span>
              <span class="stat-label">En Attente de Validation</span>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="stat-card teal">
          <mat-card-content class="stat-content">
            <div class="stat-icon"><mat-icon>schedule</mat-icon></div>
            <div class="stat-info">
              <span class="stat-value">{{ formattedAvgValidationTime }}</span>
              <span class="stat-label">Délai Moyen de Validation</span>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <div class="charts-grid mt-6" *ngIf="hasChartData">
        <mat-card class="chart-card">
          <mat-card-header>
            <mat-card-title>Répartition des Collectes</mat-card-title>
          </mat-card-header>
          <mat-card-content class="chart-content">
            <canvas baseChart
              [data]="collecteStatusChartData"
              [type]="'doughnut'"
              [options]="doughnutOptions">
            </canvas>
          </mat-card-content>
        </mat-card>

        <mat-card class="chart-card">
          <mat-card-header>
            <mat-card-title>Répartition des Utilisateurs par Rôle</mat-card-title>
          </mat-card-header>
          <mat-card-content class="chart-content">
            <canvas baseChart
              [data]="userRoleChartData"
              [type]="'bar'"
              [options]="barOptions">
            </canvas>
          </mat-card-content>
        </mat-card>
      </div>

      <mat-card class="chart-card trend-card mt-6" *ngIf="hasChartData">
        <mat-card-header class="trend-header">
          <mat-card-title>Évolution des Collectes</mat-card-title>
          <mat-button-toggle-group [value]="selectedDays" (change)="onPeriodChange($event)">
            <mat-button-toggle [value]="7">7j</mat-button-toggle>
            <mat-button-toggle [value]="30">30j</mat-button-toggle>
            <mat-button-toggle [value]="90">90j</mat-button-toggle>
          </mat-button-toggle-group>
        </mat-card-header>
        <mat-card-content class="chart-content trend-content">
          <canvas baseChart
            *ngIf="trendChartData.labels?.length; else noTrend"
            [data]="trendChartData"
            [type]="'line'"
            [options]="lineOptions">
          </canvas>
          <ng-template #noTrend>
            <p class="empty-state">Aucune collecte sur cette période</p>
          </ng-template>
        </mat-card-content>
      </mat-card>

      <mat-card class="rejection-card mt-6" *ngIf="rejectionByAgent.length">
        <mat-card-header>
          <mat-card-title>Taux de Rejet par Agent</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="rejection-row" *ngFor="let a of sortedRejectionByAgent">
            <span class="agent-name">{{ a.firstName }} {{ a.lastName }}</span>
            <div class="rejection-bar-track">
              <div class="rejection-bar-fill" [style.width.%]="a.rejectionRatePercent"
                   [class.high]="a.rejectionRatePercent >= 30"></div>
            </div>
            <span class="rejection-value">{{ a.rejectionRatePercent | number:'1.0-1' }}%</span>
            <span class="rejection-count">({{ a.rejectedCount }}/{{ a.totalCount }})</span>
          </div>
        </mat-card-content>
      </mat-card>

      <mat-card class="recent-card mt-6">
        <mat-card-header>
          <mat-card-title>Dernières Collectes Reçues</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="recentCollectes" class="w-full">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef> ID </th>
              <td mat-cell *matCellDef="let row"> #{{ row.id }} </td>
            </ng-container>

            <ng-container matColumnDef="agent">
              <th mat-header-cell *matHeaderCellDef> Agent </th>
              <td mat-cell *matCellDef="let row"> {{ row.agent.firstName }} {{ row.agent.lastName }} </td>
            </ng-container>

            <ng-container matColumnDef="coords">
              <th mat-header-cell *matHeaderCellDef> GPS </th>
              <td mat-cell *matCellDef="let row">
                <span *ngIf="row.latitude && row.longitude" class="gps-badge">
                  <mat-icon inline>location_on</mat-icon> {{ row.latitude | number:'1.4-4' }}, {{ row.longitude | number:'1.4-4' }}
                </span>
                <span *ngIf="!row.latitude" class="text-gray-400">Non capturé</span>
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
              <th mat-header-cell *matHeaderCellDef> Date </th>
              <td mat-cell *matCellDef="let row"> {{ row.createdAt | date:'dd/MM/yyyy HH:mm' }} </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .welcome-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
      gap: 1rem;
      h2 {
        font-weight: 700;
        margin-bottom: 0.2rem;
      }
      p {
        color: #64748b;
        margin: 0;
      }
    }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1rem;
    }
    .stat-card {
      border-radius: 12px;
      border: none;
      .stat-content {
        display: flex;
        align-items: center;
        gap: 1rem;
        padding: 1.2rem;
      }
      .stat-icon {
        width: 52px;
        height: 52px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        mat-icon {
          font-size: 28px;
          width: 28px;
          height: 28px;
          color: #ffffff;
        }
      }
      .stat-info {
        display: flex;
        flex-direction: column;
        .stat-value {
          font-size: 1.6rem;
          font-weight: 700;
        }
        .stat-label {
          font-size: 0.85rem;
          color: #64748b;
        }
      }
      &.blue .stat-icon { background: #2563eb; }
      &.green .stat-icon { background: #10b981; }
      &.purple .stat-icon { background: #8b5cf6; }
      &.orange .stat-icon { background: #f59e0b; }
      &.teal .stat-icon { background: #14b8a6; }
    }
    .charts-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
      gap: 1rem;
    }
    .chart-card {
      border-radius: 12px;
      border: none;
    }
    .chart-content {
      height: 280px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 1rem;
    }
    .trend-card {
      .trend-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        width: 100%;
      }
    }
    .trend-content {
      height: 260px;
    }
    .rejection-card {
      border-radius: 12px;
      border: none;
    }
    .rejection-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.5rem 0;
      border-bottom: 1px solid #f1f5f9;
      &:last-child { border-bottom: none; }
    }
    .agent-name {
      flex: 0 0 160px;
      font-weight: 600;
      font-size: 0.9rem;
    }
    .rejection-bar-track {
      flex: 1;
      height: 8px;
      background: #f1f5f9;
      border-radius: 4px;
      overflow: hidden;
    }
    .rejection-bar-fill {
      height: 100%;
      background: #f59e0b;
      border-radius: 4px;
      transition: width 0.3s ease;
      &.high { background: #ef4444; }
    }
    .rejection-value {
      flex: 0 0 50px;
      text-align: right;
      font-weight: 600;
      font-size: 0.85rem;
    }
    .rejection-count {
      flex: 0 0 60px;
      color: #94a3b8;
      font-size: 0.8rem;
    }
    .mt-6 {
      margin-top: 1.5rem;
    }
    .w-full {
      width: 100%;
    }
    .gps-badge {
      display: inline-flex;
      align-items: center;
      gap: 2px;
      background: #eff6ff;
      color: #2563eb;
      padding: 2px 8px;
      border-radius: 6px;
      font-size: 0.8rem;
    }
    .empty-state {
      text-align: center;
      color: #94a3b8;
      padding: 2rem;
    }
  `]
})
export class DashboardHomeComponent implements OnInit {
  private authService = inject(AuthService);
  private statisticsService = inject(StatisticsService);
  private collecteService = inject(CollecteService);
  private dialog = inject(MatDialog);

  user = this.authService.currentUser;
  isSuperAdmin = false;
  orgCount = 0;
  formCount = 0;
  collecteCount = 0;
  pendingCount = 0;

  openOrgReports(): void {
    const orgId = this.user()?.organizationId;
    if (!orgId) return;
    this.dialog.open(ReportHistoryDialogComponent, {
      width: '750px',
      data: {
        title: this.user()?.organizationName || 'Organisation',
        type: 'ORGANIZATION',
        targetId: orgId,
        canGenerate: this.user()?.role === 'ADMIN_PRINCIPAL' || this.user()?.role === 'ADMIN_SECONDAIRE'
      }
    });
  }
  recentCollectes: CollecteResponse[] = [];
  displayedColumns = ['id', 'agent', 'coords', 'status', 'createdAt'];

  hasChartData = false;
  selectedDays = 30;
  rejectionByAgent: AgentRejectionRate[] = [];
  avgValidationTimeHours: number | null = null;

  collecteStatusChartData: ChartData<'doughnut'> = {
    labels: ['En attente', 'Validées', 'Rejetées'],
    datasets: [{
      data: [0, 0, 0],
      backgroundColor: ['#f59e0b', '#10b981', '#ef4444'],
      borderWidth: 0
    }]
  };

  userRoleChartData: ChartData<'bar'> = {
    labels: ['Agents', 'Superviseurs', 'Admins'],
    datasets: [{
      label: 'Nombre d\'utilisateurs',
      data: [0, 0, 0],
      backgroundColor: ['#2563eb', '#8b5cf6', '#10b981'],
      borderRadius: 6
    }]
  };

  trendChartData: ChartData<'line'> = {
    labels: [],
    datasets: [{
      label: 'Collectes reçues',
      data: [],
      borderColor: '#2563eb',
      backgroundColor: 'rgba(37, 99, 235, 0.1)',
      fill: true,
      tension: 0.3,
      pointRadius: 3
    }]
  };

  doughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { position: 'bottom' }
    }
  };

  barOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: { beginAtZero: true, ticks: { precision: 0 } }
    }
  };

  lineOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: { beginAtZero: true, ticks: { precision: 0 } }
    }
  };

  get sortedRejectionByAgent(): AgentRejectionRate[] {
    return [...this.rejectionByAgent].sort((a, b) => b.rejectionRatePercent - a.rejectionRatePercent);
  }

  get formattedAvgValidationTime(): string {
    if (this.avgValidationTimeHours === null || this.avgValidationTimeHours === undefined) {
      return '—';
    }
    if (this.avgValidationTimeHours < 1) {
      return `${Math.round(this.avgValidationTimeHours * 60)} min`;
    }
    if (this.avgValidationTimeHours < 24) {
      return `${this.avgValidationTimeHours.toFixed(1)} h`;
    }
    return `${(this.avgValidationTimeHours / 24).toFixed(1)} j`;
  }

  ngOnInit(): void {
    const role = this.user()?.role;
    this.isSuperAdmin = role === 'SUPER_ADMIN';

    this.loadStatistics(this.selectedDays);

    // Chargement du tableau des dernières collectes récentes
    const collectes$ = role === 'SUPERVISOR'
      ? this.collecteService.getTeamCollectes()
      : this.collecteService.getOrganizationCollectes();

    collectes$.subscribe({
      next: (collectes) => {
        this.recentCollectes = collectes.slice(0, 5);
      },
      error: () => { }
    });
  }

  onPeriodChange(event: MatButtonToggleChange): void {
    this.selectedDays = event.value;
    this.loadStatistics(this.selectedDays);
  }

  private loadStatistics(days: number): void {
    this.statisticsService.getStatistics(days).subscribe({
      next: (stats) => {
        if (stats.scope === 'GLOBAL') {
          this.orgCount = stats.activeOrganizations ?? 0;
        } else {
          this.orgCount = stats.totalUsers ?? 0;
        }
        this.formCount = stats.publishedForms ?? 0;
        this.collecteCount = stats.totalCollectes ?? 0;
        this.pendingCount = stats.pendingCollectes ?? 0;
        this.avgValidationTimeHours = stats.avgValidationTimeHours ?? null;
        this.rejectionByAgent = stats.rejectionByAgent ?? [];

        this.collecteStatusChartData = {
          ...this.collecteStatusChartData,
          datasets: [{
            ...this.collecteStatusChartData.datasets[0],
            data: [stats.pendingCollectes ?? 0, stats.validatedCollectes ?? 0, stats.rejectedCollectes ?? 0]
          }]
        };

        this.userRoleChartData = {
          ...this.userRoleChartData,
          datasets: [{
            ...this.userRoleChartData.datasets[0],
            data: [stats.totalAgents ?? 0, stats.totalSupervisors ?? 0, stats.totalAdmins ?? 0]
          }]
        };

        const trend = stats.collecteTrend ?? [];
        this.trendChartData = {
          labels: trend.map(t => this.formatTrendDate(t.date)),
          datasets: [{
            ...this.trendChartData.datasets[0],
            data: trend.map(t => t.count)
          }]
        };

        this.hasChartData = true;
      },
      error: () => { }
    });
  }

  private formatTrendDate(isoDate: string): string {
    const d = new Date(isoDate);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'VALIDATED': return 'accent';
      case 'REJECTED': return 'warn';
      default: return 'primary';
    }
  }
}