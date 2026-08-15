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
