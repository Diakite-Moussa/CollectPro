import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AssignSupervisorRequest, CreateUserRequest, UserResponse } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly apiUrl = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
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
}
