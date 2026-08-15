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
}
