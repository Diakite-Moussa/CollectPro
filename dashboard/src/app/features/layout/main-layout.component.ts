import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../core/services/auth.service';
import { ROUTE_ROLES } from '../../core/constants/route-roles';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule
  ],
  template: `
    <div class="layout-container">
      <mat-toolbar color="primary" class="main-toolbar shadow-sm">
        <button mat-icon-button (click)="sidenav.toggle()">
          <mat-icon>menu</mat-icon>
        </button>
        <span class="brand-title">
          <mat-icon class="brand-icon">assignment_turned_in</mat-icon>
          CollectPro Dashboard
        </span>
        <span class="spacer"></span>

        <div class="user-profile" *ngIf="user()">
          <button mat-button [matMenuTriggerFor]="userMenu">
            <mat-icon class="avatar-icon">account_circle</mat-icon>
            <span class="user-name">{{ user()?.firstName }} {{ user()?.lastName }}</span>
            <span class="role-chip">{{ user()?.role }}</span>
          </button>
          <mat-menu #userMenu="matMenu">
            <div class="menu-header">
              <p class="menu-email">{{ user()?.email }}</p>
              <p class="menu-org" *ngIf="user()?.organizationName">{{ user()?.organizationName }}</p>
            </div>
            <button mat-menu-item routerLink="/profile">
              <mat-icon>account_circle</mat-icon>
              <span>Mon profil</span>
            </button>
            <button mat-menu-item (click)="onLogout()">
              <mat-icon color="warn">exit_to_app</mat-icon>
              <span>Déconnexion</span>
            </button>
          </mat-menu>
        </div>
      </mat-toolbar>

      <mat-sidenav-container class="sidenav-container">
        <mat-sidenav #sidenav mode="side" opened class="main-sidenav">
          <mat-nav-list>
            <a mat-list-item routerLink="/dashboard" routerLinkActive="active-link">
              <mat-icon matListItemIcon>dashboard</mat-icon>
              <span matListItemTitle>Vue Générale</span>
            </a>
            <a mat-list-item routerLink="/organizations" routerLinkActive="active-link" *ngIf="canAccess('organizations')">
              <mat-icon matListItemIcon>corporate_fare</mat-icon>
              <span matListItemTitle>Organisations</span>
            </a>
            <a mat-list-item routerLink="/users" routerLinkActive="active-link" *ngIf="canAccess('users')">
              <mat-icon matListItemIcon>people</mat-icon>
              <span matListItemTitle>Utilisateurs</span>
            </a>
            <a mat-list-item routerLink="/forms" routerLinkActive="active-link" *ngIf="canAccess('forms')">
              <mat-icon matListItemIcon>dynamic_form</mat-icon>
              <span matListItemTitle>Formulaires</span>
            </a>
            <a mat-list-item routerLink="/collectes" routerLinkActive="active-link" *ngIf="canAccess('collectes')">
              <mat-icon matListItemIcon>sync_alt</mat-icon>
              <span matListItemTitle>Collectes Terrain</span>
            </a>

            <a mat-list-item routerLink="/missions" routerLinkActive="active-link" *ngIf="canAccess('missions')">
              <mat-icon matListItemIcon>flag</mat-icon>
              <span matListItemTitle>Missions</span>
            </a>

            <a mat-list-item routerLink="/sync-logs" routerLinkActive="active-link" *ngIf="canAccess('sync-logs')">
              <mat-icon matListItemIcon>sync</mat-icon>
              <span matListItemTitle>Synchronisations</span>
            </a>

            <a mat-list-item routerLink="/audit-logs" routerLinkActive="active-link" *ngIf="canAccess('audit-logs')">
              <mat-icon matListItemIcon>shield</mat-icon>
              <span matListItemTitle>Journal d'Audit</span>
            </a>
          </mat-nav-list>
        </mat-sidenav>

        <mat-sidenav-content class="main-content">
          <router-outlet></router-outlet>
        </mat-sidenav-content>
      </mat-sidenav-container>
    </div>
  `,
  styles: [`
    .layout-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
    }
    .main-toolbar {
      z-index: 10;
      background: #1e293b;
      color: #ffffff;
    }
    .brand-title {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 700;
      font-size: 1.2rem;
    }
    .brand-icon {
      color: #3b82f6;
    }
    .spacer {
      flex: 1 1 auto;
    }
    .sidenav-container {
      flex: 1;
      background-color: #f8fafc;
    }
    .main-sidenav {
      width: 240px;
      background: #ffffff;
      border-right: 1px solid #e2e8f0;
    }
    .main-content {
      padding: 1.5rem;
      background: #f8fafc;
    }
    .user-name {
      margin: 0 0.5rem;
      font-weight: 500;
    }
    .role-chip {
      background: rgba(255, 255, 255, 0.2);
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .active-link {
      background-color: #eff6ff !important;
      color: #2563eb !important;
      mat-icon {
        color: #2563eb !important;
      }
    }
    .menu-header {
      padding: 0.5rem 1rem;
      border-bottom: 1px solid #e2e8f0;
      .menu-email {
        font-weight: 600;
        margin: 0;
      }
      .menu-org {
        font-size: 0.8rem;
        color: #64748b;
        margin: 0;
      }
    }
  `]
})
export class MainLayoutComponent {
  private authService = inject(AuthService);
  user = this.authService.currentUser;

  canAccess(routeKey: string): boolean {
    const allowedRoles = ROUTE_ROLES[routeKey];
    if (!allowedRoles) return true;
    const userRole = this.user()?.role;
    return !!userRole && allowedRoles.includes(userRole as any);
  }

  onLogout(): void {
    this.authService.logout();
  }
}
