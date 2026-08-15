import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login.component';
import { MainLayoutComponent } from './features/layout/main-layout.component';
import { DashboardHomeComponent } from './features/home/dashboard-home.component';
import { OrganizationListComponent } from './features/organizations/organization-list.component';
import { UserListComponent } from './features/users/user-list.component';
import { FormListComponent } from './features/forms/form-list.component';
import { CollecteListComponent } from './features/collectes/collecte-list.component';
import { ActivationComponent } from './features/auth/activation.component';
import { AuditLogsComponent } from './features/audit-logs/audit-logs.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'activate', component: ActivationComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'reset-password', component: ResetPasswordComponent },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardHomeComponent },
      { path: 'organizations', component: OrganizationListComponent, canActivate: [roleGuard] },
      { path: 'users', component: UserListComponent, canActivate: [roleGuard] },
      { path: 'forms', component: FormListComponent, canActivate: [roleGuard] },
      { path: 'collectes', component: CollecteListComponent, canActivate: [roleGuard] },
      { path: 'audit-logs', component: AuditLogsComponent, canActivate: [roleGuard] }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
