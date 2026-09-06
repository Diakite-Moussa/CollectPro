import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssignSupervisorRequest, ChangePasswordRequest, CreateUserRequest, UpdateProfileRequest, UpdateUserStatusRequest, UserResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';
import { HttpParams } from '@angular/common/http';
import { UserResponsePage } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) { }

  /**
 * Fix #12 — pagination serveur.
 */
  getUsers(page = 0, size = 25): Observable<UserResponsePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<UserResponsePage>(this.apiUrl, { params });
  }

  createUser(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.apiUrl, request);
  }

  assignSupervisor(request: AssignSupervisorRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/assign-supervisor`, request);
  }

  resendInvitation(userId: number): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${userId}/resend-invitation`, {});
  }

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/me`);
  }

  updateProfile(request: UpdateProfileRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/me`, request);
  }

  changePassword(request: ChangePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/password`, request);
  }

  updateUserStatus(userId: number, status: 'ACTIVE' | 'DISABLED'): Observable<UserResponse> {
    const body: UpdateUserStatusRequest = { status };
    return this.http.patch<UserResponse>(`${this.apiUrl}/${userId}/status`, body);
  }
}
