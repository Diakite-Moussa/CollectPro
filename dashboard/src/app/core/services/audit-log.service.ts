import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogPage } from '../models/audit-log.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private readonly apiUrl = `${environment.apiUrl}/audit-logs`;

  constructor(private http: HttpClient) { }

  /**
   * Fix #12 — pagination serveur.
   */
  getAuditLogs(page = 0, size = 25): Observable<AuditLogPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<AuditLogPage>(this.apiUrl, { params });
  }
}