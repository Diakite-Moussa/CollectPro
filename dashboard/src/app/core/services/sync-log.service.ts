import { SyncLogResponse, SyncLogPage } from "../models/sync-log.model";
import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import { UserResponse } from "../models/user.model";

@Injectable({ providedIn: 'root' })
export class SyncLogService {
    private readonly apiUrl = `${environment.apiUrl}/sync-logs`;
    private readonly usersApiUrl = `${environment.apiUrl}/users`;

    constructor(private http: HttpClient) { }

    /**
     * Fix #3 — pagination serveur.
     * @param agentId   filtre optionnel par agent
     * @param page      numéro de page (0-indexé), défaut 0
     * @param size      taille de page, défaut 25
     */
    getSyncLogs(agentId?: number | null, page = 0, size = 25): Observable<SyncLogPage> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (agentId) params = params.set('agentId', agentId.toString());
        return this.http.get<SyncLogPage>(this.apiUrl, { params });
    }

    /**
     * Fix #2 — endpoint dédié aux Superviseurs pour ne charger que leurs agents.
     */
    getMyAgents(): Observable<UserResponse[]> {
        return this.http.get<UserResponse[]>(`${this.usersApiUrl}/my-agents`);
    }
}