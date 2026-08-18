import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { StatisticsService } from '../../core/services/statistics.service';
import { CollecteService } from '../../core/services/collecte.service';
import { CollecteResponse } from '../../core/models/collecte.model';
import { AuthService } from '../../core/services/auth.service';

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
    BaseChartDirective
  ],
  template: `
    <div class="dashboard-home">
      <div class="welcome-header">
        <h2>Tableau de bord — Vue Générale</h2>
        <p>Bienvenue {{ user()?.firstName }} ({{ user()?.organizationName || 'Super Admin' }})</p>
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
      margin-bottom: 1.5rem;
      h2 {
        font-weight: 700;
        margin-bottom: 0.2rem;
      }
      p {
        color: #64748b;
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
  `]
})
export class DashboardHomeComponent implements OnInit {
  private authService = inject(AuthService);
  private statisticsService = inject(StatisticsService);
  private collecteService = inject(CollecteService);

  user = this.authService.currentUser;
  isSuperAdmin = false;
  orgCount = 0;
  formCount = 0;
  collecteCount = 0;
  pendingCount = 0;
  recentCollectes: CollecteResponse[] = [];
  displayedColumns = ['id', 'agent', 'coords', 'status', 'createdAt'];

  hasChartData = false;

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

  ngOnInit(): void {
    const role = this.user()?.role;
    this.isSuperAdmin = role === 'SUPER_ADMIN';

    // Un seul appel au service de statistiques du backend
    this.statisticsService.getStatistics().subscribe({
      next: (stats) => {
        if (stats.scope === 'GLOBAL') {
          this.orgCount = stats.activeOrganizations ?? 0;
        } else {
          this.orgCount = stats.totalUsers ?? 0;
        }
        this.formCount = stats.publishedForms ?? 0;
        this.collecteCount = stats.totalCollectes ?? 0;
        this.pendingCount = stats.pendingCollectes ?? 0;

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

        this.hasChartData = true;
      },
      error: () => {}
    });

    // Chargement du tableau des dernières collectes récentes
    const collectes$ = role === 'SUPERVISOR'
      ? this.collecteService.getTeamCollectes()
      : this.collecteService.getOrganizationCollectes();

    collectes$.subscribe({
      next: (collectes) => {
        this.recentCollectes = collectes.slice(0, 5);
      },
      error: () => {}
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
