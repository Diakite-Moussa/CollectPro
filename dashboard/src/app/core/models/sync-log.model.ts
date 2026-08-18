export interface AgentSummary {
    id: number;
    firstName: string;
    lastName: string;
}

export interface SyncLogResponse {
    id: number;
    agent: AgentSummary;
    localReference: string;
    collecteId?: number;
    result: 'SUCCESS' | 'ERROR' | 'DUPLICATE';
    errorMessage?: string;
    createdAt: string;
}

/** Fix #3 — représente la réponse paginée retournée par Spring Page<SyncLogResponse>. */
export interface SyncLogPage {
    content: SyncLogResponse[];
    totalElements: number;
    totalPages: number;
    number: number;      // numéro de page courant (0-indexé)
    size: number;
    first: boolean;
    last: boolean;
}