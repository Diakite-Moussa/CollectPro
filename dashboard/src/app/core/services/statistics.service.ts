import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StatisticsResponse } from '../models/statistics.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private readonly apiUrl = `${environment.apiUrl}/statistics`;

  constructor(private http: HttpClient) { }

  getStatistics(days: number = 30): Observable<StatisticsResponse> {
    const params = new HttpParams().set('days', days);
    return this.http.get<StatisticsResponse>(this.apiUrl, { params });
  }
}