import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
    MissionResponse,
    CreateMissionRequest,
    UpdateMissionRequest,
    AssignAgentsToMissionRequest,
    AssignFormsToMissionRequest,
    MissionProgressResponse
} from '../models/mission.model';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class MissionService {
    private readonly apiUrl = `${environment.apiUrl}/missions`;

    constructor(private http: HttpClient) { }

    getMissions(): Observable<MissionResponse[]> {
        return this.http.get<MissionResponse[]>(this.apiUrl);
    }

    getMission(id: number): Observable<MissionResponse> {
        return this.http.get<MissionResponse>(`${this.apiUrl}/${id}`);
    }

    createMission(request: CreateMissionRequest): Observable<MissionResponse> {
        return this.http.post<MissionResponse>(this.apiUrl, request);
    }

    updateMission(id: number, request: UpdateMissionRequest): Observable<MissionResponse> {
        return this.http.put<MissionResponse>(`${this.apiUrl}/${id}`, request);
    }

    cancelMission(id: number): Observable<MissionResponse> {
        return this.http.patch<MissionResponse>(`${this.apiUrl}/${id}/cancel`, {});
    }

    assignAgents(id: number, request: AssignAgentsToMissionRequest): Observable<MissionResponse> {
        return this.http.post<MissionResponse>(`${this.apiUrl}/${id}/agents`, request);
    }

    unassignAgent(id: number, agentId: number): Observable<MissionResponse> {
        return this.http.delete<MissionResponse>(`${this.apiUrl}/${id}/agents/${agentId}`);
    }

    assignForms(id: number, request: AssignFormsToMissionRequest): Observable<MissionResponse> {
        return this.http.post<MissionResponse>(`${this.apiUrl}/${id}/forms`, request);
    }

    getMissionsProgress(): Observable<MissionProgressResponse[]> {
        return this.http.get<MissionProgressResponse[]>(`${this.apiUrl}/progress`);
    }

    getMissionProgress(id: number): Observable<MissionProgressResponse> {
        return this.http.get<MissionProgressResponse>(`${this.apiUrl}/${id}/progress`);
    }
}