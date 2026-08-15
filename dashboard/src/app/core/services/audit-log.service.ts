import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLogResponse } from '../models/audit-log.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private readonly apiUrl = `${environment.apiUrl}/audit-logs`;

  constructor(private http: HttpClient) {}

  getAuditLogs(): Observable<AuditLogResponse[]> {
    return this.http.get<AuditLogResponse[]>(this.apiUrl);
  }
}
