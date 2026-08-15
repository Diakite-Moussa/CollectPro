import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { StatisticsResponse } from '../models/statistics.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private readonly apiUrl = `${environment.apiUrl}/statistics`;

  constructor(private http: HttpClient) {}

  getStatistics(): Observable<StatisticsResponse> {
    return this.http.get<StatisticsResponse>(this.apiUrl);
  }
}
