import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { SyncLogService } from '../../core/services/sync-log.service';
import { SyncLogResponse } from '../../core/models/sync-log.model';
import { AuthService } from '../../core/services/auth.service';
import { UserResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-sync-log-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule
  ],
  template: `
    <div class="sync-logs-container">
      <div class="page-header">
        <h2>Journal de Synchronisation</h2>
        <p class="subtitle">Suivi des tentatives de synchronisation terrain</p>
      </div>

      <!-- Fix #2 : filtre via endpoint dédié /users/my-agents -->
      <mat-form-field appearance="outline" class="agent-filter" *ngIf="isSupervisor">
        <mat-label>Filtrer par agent</mat-label>
        <mat-select
          [(ngModel)]="selectedAgentId"
          (selectionChange)="onAgentFilterChange($event.value)">
          <mat-option [value]="null">Tous mes agents</mat-option>
          <mat-option *ngFor="let agent of myAgents" [value]="agent.id">
            {{ agent.firstName }} {{ agent.lastName }}
          </mat-option>
        </mat-select>
      </mat-form-field>

      <mat-card class="table-card">
        <mat-card-content>
          <table mat-table [dataSource]="dataSource" class="w-full">
            <ng-container matColumnDef="agent">
              <th mat-header-cell *matHeaderCellDef> Agent </th>
              <td mat-cell *matCellDef="let row">{{ row.agent.firstName }} {{ row.agent.lastName }}</td>
            </ng-container>

            <ng-container matColumnDef="localReference">
              <th mat-header-cell *matHeaderCellDef> Référence locale </th>
              <td mat-cell *matCellDef="let row">{{ row.localReference }}</td>
            </ng-container>

            <ng-container matColumnDef="collecteId">
              <th mat-header-cell *matHeaderCellDef> Collecte </th>
              <td mat-cell *matCellDef="let row">
                <span *ngIf="row.collecteId">#{{ row.collecteId }}</span>
                <span *ngIf="!row.collecteId">-</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="result">
              <th mat-header-cell *matHeaderCellDef> Résultat </th>
              <td mat-cell *matCellDef="let row">
                <span class="result-chip" [ngClass]="row.result.toLowerCase()">{{ row.result }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="errorMessage">
              <th mat-header-cell *matHeaderCellDef> Message d'erreur </th>
              <td mat-cell *matCellDef="let row" class="details-cell">{{ row.errorMessage || '-' }}</td>
            </ng-container>

            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef> Date </th>
              <td mat-cell *matCellDef="let row">{{ row.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

            <!-- Fix : message vide quand aucun résultat -->
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell empty-cell" [attr.colspan]="displayedColumns.length">
                <mat-icon>inbox</mat-icon>
                <span>Aucun journal de synchronisation trouvé.</span>
              </td>
            </tr>
          </table>

          <!-- Fix #3 : pagination serveur — (page) émet PageEvent vers loadSyncLogs -->
          <mat-paginator
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageSizeOptions]="[10, 25, 50]"
            showFirstLastButtons
            (page)="onPageChange($event)">
          </mat-paginator>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .sync-logs-container { padding: 0.5rem; }
    .page-header h2 { font-weight: 700; margin-bottom: 0.2rem; }
    .subtitle { color: #64748b; margin-bottom: 1rem; }
    .agent-filter { width: 280px; margin-bottom: 1rem; }
    .w-full { width: 100%; }
    .details-cell { max-width: 300px; word-break: break-word; font-size: 0.88rem; }
    .result-chip {
      display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 700;
      &.success { background: #dcfce7; color: #15803d; }
      &.error { background: #fee2e2; color: #b91c1c; }
      &.duplicate { background: #fef3c7; color: #b45309; }
    }
    .empty-cell {
      text-align: center; padding: 2rem; color: #94a3b8;
      display: flex; align-items: center; justify-content: center; gap: 0.5rem;
    }
  `]
})
export class SyncLogListComponent implements OnInit {
  private syncLogService = inject(SyncLogService);
  private authService = inject(AuthService);

  dataSource = new MatTableDataSource<SyncLogResponse>([]);
  displayedColumns = ['agent', 'localReference', 'collecteId', 'result', 'errorMessage', 'createdAt'];

  isSupervisor = false;
  myAgents: UserResponse[] = [];
  selectedAgentId: number | null = null;

  // Fix #3 — état de pagination côté client
  totalElements = 0;
  pageSize = 25;
  currentPage = 0;

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  ngOnInit(): void {
    const user = this.authService.currentUser();
    this.isSupervisor = user?.role === 'SUPERVISOR';

    // Fix #2 — endpoint dédié, pas de chargement de tous les users de l'org
    if (this.isSupervisor) {
      this.syncLogService.getMyAgents().subscribe({
        next: (agents) => { this.myAgents = agents; },
        error: () => { }
      });
    }

    this.loadSyncLogs();
  }

  loadSyncLogs(): void {
    this.syncLogService.getSyncLogs(this.selectedAgentId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.dataSource.data = page.content;
        this.totalElements = page.totalElements;
      },
      error: () => { }
    });
  }

  onAgentFilterChange(agentId: number | null): void {
    this.selectedAgentId = agentId;
    this.currentPage = 0; // reset à la 1ère page quand le filtre change
    this.loadSyncLogs();
  }

  // Fix #3 — appelé par le MatPaginator quand l'utilisateur change de page/taille
  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadSyncLogs();
  }
}