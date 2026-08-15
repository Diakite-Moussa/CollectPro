import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PermissionResponse, UserPermissionsResponse } from '../models/permission.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private readonly apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getDelegatablePermissions(): Observable<PermissionResponse[]> {
    return this.http.get<PermissionResponse[]>(`${this.apiUrl}/permissions/delegatable`);
  }

  getUserPermissions(userId: number): Observable<UserPermissionsResponse> {
    return this.http.get<UserPermissionsResponse>(`${this.apiUrl}/users/${userId}/permissions`);
  }

  updateUserPermissions(userId: number, permissionCodes: string[]): Observable<UserPermissionsResponse> {
    return this.http.put<UserPermissionsResponse>(`${this.apiUrl}/users/${userId}/permissions`, {
      permissionCodes
    });
  }
}
