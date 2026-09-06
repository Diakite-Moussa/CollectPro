export interface AgentSummary {
  id: number;
  firstName: string;
  lastName: string;
}

export interface CollecteAttachment {
  id: number;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
  url: string;
}

export interface CollecteResponse {
  id: number;
  agent: AgentSummary;
  formVersionId: number;
  dataJson: string;
  attachments?: CollecteAttachment[];
  latitude?: number;
  longitude?: number;
  status: 'PENDING_VALIDATION' | 'VALIDATED' | 'REJECTED';
  validationComment?: string;
  validatedBy?: AgentSummary;
  validatedAt?: string;
  createdAt: string;
  updatedAt?: string;
  missionId?: number;
  outsideMissionZone?: boolean | null;
}

/** Fix #12 — représente la réponse paginée retournée par Spring Page<CollecteResponse>. */
export interface CollecteResponsePage {
  content: CollecteResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
