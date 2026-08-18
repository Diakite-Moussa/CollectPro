import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateOrganizationRequest,
  OrganizationResponse,
  UpdateOrganizationRequest,
  UpdateOrganizationStatusRequest
} from '../models/organization.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class OrganizationService {
  private readonly apiUrl = `${environment.apiUrl}/organizations`;

  constructor(private http: HttpClient) { }

  getOrganizations(): Observable<OrganizationResponse[]> {
    return this.http.get<OrganizationResponse[]>(this.apiUrl);
  }

  createOrganization(request: CreateOrganizationRequest): Observable<OrganizationResponse> {
    return this.http.post<OrganizationResponse>(this.apiUrl, request);
  }

  updateOrganization(id: number, request: UpdateOrganizationRequest): Observable<OrganizationResponse> {
    return this.http.put<OrganizationResponse>(`${this.apiUrl}/${id}`, request);
  }

  updateOrganizationStatus(id: number, status: 'ACTIVE' | 'INACTIVE'): Observable<OrganizationResponse> {
    const body: UpdateOrganizationStatusRequest = { status };
    return this.http.patch<OrganizationResponse>(`${this.apiUrl}/${id}/status`, body);
  }
}