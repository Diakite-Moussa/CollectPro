import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { MainLayoutComponent } from './features/layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'activate',
    loadComponent: () => import('./features/auth/activation.component').then(m => m.ActivationComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/home/dashboard-home.component').then(m => m.DashboardHomeComponent)
      },
      {
        path: 'organizations',
        loadComponent: () => import('./features/organizations/organization-list.component').then(m => m.OrganizationListComponent),
        canActivate: [roleGuard]
      },
      {
        path: 'users',
        loadComponent: () => import('./features/users/user-list.component').then(m => m.UserListComponent),
        canActivate: [roleGuard]
      },
      {
        path: 'forms',
        loadComponent: () => import('./features/forms/form-list.component').then(m => m.FormListComponent),
        canActivate: [roleGuard]
      },
      {
        path: 'collectes',
        loadComponent: () => import('./features/collectes/collecte-list.component').then(m => m.CollecteListComponent),
        canActivate: [roleGuard]
      },
      {
        path: 'sync-logs',
        loadComponent: () => import('./features/sync-logs/sync-log-list.component').then(m => m.SyncLogListComponent),
        canActivate: [roleGuard]
      },
      {
        path: 'audit-logs',
        loadComponent: () => import('./features/audit-logs/audit-logs.component').then(m => m.AuditLogsComponent),
        canActivate: [roleGuard]
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent)
      },
      {
        path: 'missions',
        loadComponent: () => import('./features/missions/mission-list.component').then(m => m.MissionListComponent),
        canActivate: [roleGuard]
      },
    ]
  },
  { path: '**', redirectTo: 'login' }
];