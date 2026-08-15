import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateFormRequest, CreateFormVersionRequest, FormResponse, FormVersionResponse } from '../models/form.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FormService {
  private readonly apiUrl = `${environment.apiUrl}/forms`;

  constructor(private http: HttpClient) {}

  getForms(): Observable<FormResponse[]> {
    return this.http.get<FormResponse[]>(this.apiUrl);
  }

  getPublishedForms(): Observable<FormResponse[]> {
    return this.http.get<FormResponse[]>(`${this.apiUrl}/published`);
  }

  createForm(request: CreateFormRequest): Observable<FormResponse> {
    return this.http.post<FormResponse>(this.apiUrl, request);
  }

  createFormVersion(formId: number, request: CreateFormVersionRequest): Observable<FormVersionResponse> {
    return this.http.post<FormVersionResponse>(`${this.apiUrl}/${formId}/versions`, request);
  }

  publishForm(id: number): Observable<FormResponse> {
    return this.http.post<FormResponse>(`${this.apiUrl}/${id}/publish`, {});
  }

  archiveForm(id: number): Observable<FormResponse> {
    return this.http.post<FormResponse>(`${this.apiUrl}/${id}/archive`, {});
  }
}
