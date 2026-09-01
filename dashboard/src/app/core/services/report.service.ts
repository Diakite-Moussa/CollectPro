import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GenerateReportRequest, PageResponse, ReportResponse } from '../models/report.model';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/reports`;

  generateOrganizationReport(orgId: number, request?: GenerateReportRequest): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.apiUrl}/organizations/${orgId}`, request || {});
  }

  generateMissionReport(missionId: number, request?: GenerateReportRequest): Observable<ReportResponse> {
    return this.http.post<ReportResponse>(`${this.apiUrl}/missions/${missionId}`, request || {});
  }

  getOrganizationReports(orgId: number, page = 0, size = 20): Observable<PageResponse<ReportResponse>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<ReportResponse>>(`${this.apiUrl}/organizations/${orgId}`, { params });
  }

  getMissionReports(missionId: number, page = 0, size = 20): Observable<PageResponse<ReportResponse>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<PageResponse<ReportResponse>>(`${this.apiUrl}/missions/${missionId}`, { params });
  }

  downloadReport(reportId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${reportId}/download`, { responseType: 'blob' });
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
