export interface ActorSummary {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface AuditLogResponse {
  id: number;
  action: string;
  entityType?: string;
  entityId?: number;
  details?: string;
  createdAt: string;
  actor?: ActorSummary;
}

/** Fix #12 — représente la réponse paginée retournée par Spring Page<AuditLogResponse>. */
export interface AuditLogPage {
  content: AuditLogResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
