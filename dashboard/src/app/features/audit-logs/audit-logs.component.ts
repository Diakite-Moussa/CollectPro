import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { AuditLogService } from '../../core/services/audit-log.service';
import { AuditLogResponse } from '../../core/models/audit-log.model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatChipsModule
  ],
  template: `
    <div class="audit-logs-container">
      <div class="page-header">
        <div>
          <h2>Journal d'Audit & Sécurité</h2>
          <p class="subtitle">Historique des actions sensibles de la plateforme</p>
        </div>
      </div>

      <mat-card class="filter-card">
        <mat-card-content class="filter-content">
          <mat-form-field appearance="outline" class="search-field">
            <mat-label>Rechercher (page actuelle uniquement)</mat-label>
            <input matInput [(ngModel)]="searchTerm" (ngModelChange)="applyFilter()" placeholder="Ex: admin@test.com...">
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>

          <mat-form-field appearance="outline" class="action-field">
            <mat-label>Filtrer par Action</mat-label>
            <mat-select (selectionChange)="onActionFilterChange($event.value)">
              <mat-option value="">Toutes les actions</mat-option>
              <mat-option *ngFor="let act of availableActions" [value]="act">{{ act }}</mat-option>
            </mat-select>
          </mat-form-field>
        </mat-card-content>
      </mat-card>

      <mat-card class="table-card mt-4">
        <mat-card-content>
          <table mat-table [dataSource]="filteredLogs" class="w-full">
            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef> Date & Heure </th>
              <td mat-cell *matCellDef="let row">
                <span class="date-badge">
                  <mat-icon inline>schedule</mat-icon> {{ row.createdAt | date:'dd/MM/yyyy HH:mm:ss' }}
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="actor">
              <th mat-header-cell *matHeaderCellDef> Acteur </th>
              <td mat-cell *matCellDef="let row">
                <div *ngIf="row.actor" class="actor-info">
                  <strong>{{ row.actor.firstName }} {{ row.actor.lastName }}</strong>
                  <span class="actor-email">{{ row.actor.email }}</span>
                </div>
                <span *ngIf="!row.actor" class="text-gray-400">Système</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="action">
              <th mat-header-cell *matHeaderCellDef> Action </th>
              <td mat-cell *matCellDef="let row">
                <span class="action-chip" [ngClass]="getActionClass(row.action)">
                  {{ row.action }}
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="target">
              <th mat-header-cell *matHeaderCellDef> Cible </th>
              <td mat-cell *matCellDef="let row">
                <span *ngIf="row.entityType" class="target-tag">
                  {{ row.entityType }} <ng-container *ngIf="row.entityId">#{{ row.entityId }}</ng-container>
                </span>
                <span *ngIf="!row.entityType">-</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="details">
              <th mat-header-cell *matHeaderCellDef> Détails </th>
              <td mat-cell *matCellDef="let row" class="details-cell"> {{ row.details }} </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>

          <!-- Fix #12 : pagination serveur — [length]/[pageIndex]/[pageSize] pilotés
               par le backend, (page) déclenche un nouvel appel HTTP. -->
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
    </div>
  `,
  styles: [`
    .audit-logs-container { padding: 0.5rem; }
    .page-header h2 { font-weight: 700; margin-bottom: 0.2rem; }
    .subtitle { color: #64748b; margin-bottom: 1rem; }
    .filter-card { border-radius: 12px; }
    .filter-content { display: flex; gap: 1rem; flex-wrap: wrap; padding-top: 1rem; }
    .search-field { flex: 1; min-width: 250px; }
    .action-field { width: 250px; }
    .mt-4 { margin-top: 1rem; }
    .w-full { width: 100%; }
    .actor-info { display: flex; flex-direction: column; }
    .actor-email { font-size: 0.8rem; color: #64748b; }
    .date-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 0.85rem; color: #334155; }
    .target-tag { background: #f1f5f9; color: #475569; padding: 3px 8px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; }
    .details-cell { max-width: 300px; word-break: break-word; font-size: 0.88rem; }
    .action-chip {
      display: inline-block; padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 700;
      &.create { background: #e0f2fe; color: #0369a1; }
      &.validate { background: #dcfce7; color: #15803d; }
      &.reject { background: #fee2e2; color: #b91c1c; }
      &.auth { background: #f3e8ff; color: #6b21a8; }
      &.default { background: #f1f5f9; color: #334155; }
    }
  `]
})
export class AuditLogsComponent implements OnInit {
  private auditLogService = inject(AuditLogService);

  logs: AuditLogResponse[] = [];
  filteredLogs: AuditLogResponse[] = [];
  displayedColumns = ['createdAt', 'actor', 'action', 'target', 'details'];
  availableActions: string[] = [];

  // Fix #12 — état de pagination serveur
  pageIndex = 0;
  pageSize = 25;
  totalElements = 0;

  searchTerm = '';
  selectedAction = '';

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.auditLogService.getAuditLogs(this.pageIndex, this.pageSize).subscribe({
      next: (page) => {
        this.logs = page.content;
        this.totalElements = page.totalElements;
        // Reconstruit la liste des actions disponibles à partir de la page courante
        // (limitation acceptée : ne reflète pas forcément tout l'historique).
        this.availableActions = Array.from(new Set(this.logs.map(l => l.action)));
        this.applyFilter();
      },
      error: () => { }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadLogs();
  }

  applyFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();
    this.filteredLogs = this.logs.filter((log) => {
      const matchesAction = !this.selectedAction || log.action === this.selectedAction;
      if (!matchesAction) return false;
      if (!term) return true;
      const haystack = [
        log.actor?.firstName, log.actor?.lastName, log.actor?.email,
        log.action, log.entityType, log.details
      ].filter(Boolean).join(' ').toLowerCase();
      return haystack.includes(term);
    });
  }

  onActionFilterChange(action: string): void {
    this.selectedAction = action;
    this.applyFilter();
  }

  getActionClass(action: string): string {
    if (action.includes('CREATED') || action.includes('PUBLISHED')) return 'create';
    if (action.includes('VALIDATED')) return 'validate';
    if (action.includes('REJECTED') || action.includes('DISABLED')) return 'reject';
    if (action.includes('PASSWORD') || action.includes('ACTIVATED') || action.includes('INVITATION')) return 'auth';
    return 'default';
  }
}