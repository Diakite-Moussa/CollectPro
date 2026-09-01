import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CollecteResponse } from '../models/collecte.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CollecteService {
  private readonly apiUrl = `${environment.apiUrl}/collectes`;

  constructor(private http: HttpClient) {}

  getMyCollectes(): Observable<CollecteResponse[]> {
    return this.http.get<CollecteResponse[]>(`${this.apiUrl}/mine`);
  }

  getTeamCollectes(): Observable<CollecteResponse[]> {
    return this.http.get<CollecteResponse[]>(`${this.apiUrl}/team`);
  }

  /** Vue Admin : toutes les collectes de l'organisation. */
  getOrganizationCollectes(): Observable<CollecteResponse[]> {
    return this.http.get<CollecteResponse[]>(`${this.apiUrl}/organization`);
  }

  getPendingValidation(): Observable<CollecteResponse[]> {
    return this.http.get<CollecteResponse[]>(`${this.apiUrl}/team/pending-validation`);
  }

  /** RF-012 : validation d'une collecte par son superviseur (commentaire facultatif). */
  validateCollecte(id: number, comment?: string): Observable<CollecteResponse> {
    return this.http.post<CollecteResponse>(`${this.apiUrl}/${id}/validate`, { comment: comment || null });
  }

  /** RF-012 : rejet d'une collecte par son superviseur (commentaire obligatoire). */
  rejectCollecte(id: number, comment: string): Observable<CollecteResponse> {
    return this.http.post<CollecteResponse>(`${this.apiUrl}/${id}/reject`, { comment });
  }

  exportCsv(filter?: { missionId?: number; formId?: number; status?: string; startDate?: string; endDate?: string; search?: string }): Observable<Blob> {
    let params: any = {};
    if (filter) {
      if (filter.missionId != null) params.missionId = filter.missionId;
      if (filter.formId != null) params.formId = filter.formId;
      if (filter.status) params.status = filter.status;
      if (filter.startDate) params.startDate = filter.startDate;
      if (filter.endDate) params.endDate = filter.endDate;
      if (filter.search) params.search = filter.search;
    }
    return this.http.get(`${this.apiUrl}/export/csv`, { params, responseType: 'blob' });
  }

  exportExcel(filter?: { missionId?: number; formId?: number; status?: string; startDate?: string; endDate?: string; search?: string }): Observable<Blob> {
    let params: any = {};
    if (filter) {
      if (filter.missionId != null) params.missionId = filter.missionId;
      if (filter.formId != null) params.formId = filter.formId;
      if (filter.status) params.status = filter.status;
      if (filter.startDate) params.startDate = filter.startDate;
      if (filter.endDate) params.endDate = filter.endDate;
      if (filter.search) params.search = filter.search;
    }
    return this.http.get(`${this.apiUrl}/export/excel`, { params, responseType: 'blob' });
  }

  triggerBrowserDownload(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);
  }
}
