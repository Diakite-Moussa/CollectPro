export type MissionStatus = 'DRAFT' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface CreatorSummary {
    id: number;
    firstName: string;
    lastName: string;
}

export interface FormSummary {
    id: number;
    name: string;
}

export interface AgentSummary {
    id: number;
    firstName: string;
    lastName: string;
}

export interface MissionResponse {
    id: number;
    name: string;
    description?: string;
    organizationId: number;
    status: MissionStatus;
    startDate?: string;
    endDate?: string;
    latitude?: number;
    longitude?: number;
    radiusMeters?: number;
    expectedCollectesCount?: number;
    createdBy: CreatorSummary;
    forms: FormSummary[];
    agents: AgentSummary[];
    createdAt: string;
    updatedAt?: string;
}

export interface CreateMissionRequest {
    name: string;
    description?: string;
    startDate?: string;
    endDate?: string;
    latitude?: number;
    longitude?: number;
    radiusMeters?: number;
    expectedCollectesCount?: number;
}

export interface UpdateMissionRequest {
    name: string;
    description?: string;
    status?: MissionStatus;
    startDate?: string;
    endDate?: string;
    latitude?: number;
    longitude?: number;
    radiusMeters?: number;
    expectedCollectesCount?: number;
}

export interface AssignAgentsToMissionRequest {
    agentIds: number[];
}

export interface AssignFormsToMissionRequest {
    formIds: number[];
}

export interface MissionProgressResponse {
    missionId: number;
    missionName: string;
    expectedCollectesCount?: number;
    receivedCollectesCount: number;
    progressPercent?: number;
    activeAgentsCount: number;
    assignedAgentsCount: number;
}

/** Fix #12 — représente la réponse paginée retournée par Spring Page<MissionResponse>. */
export interface MissionResponsePage {
    content: MissionResponse[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
    first: boolean;
    last: boolean;
}