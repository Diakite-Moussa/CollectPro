export interface DailyCountEntry {
  date: string;
  count: number;
}

export interface AgentRejectionRate {
  agentId: number;
  firstName: string;
  lastName: string;
  totalCount: number;
  rejectedCount: number;
  rejectionRatePercent: number;
}

export interface StatisticsResponse {
  scope: 'GLOBAL' | 'ORGANIZATION' | 'TEAM';
  organizationId?: number;
  organizationName?: string;

  totalOrganizations?: number;
  activeOrganizations?: number;

  totalUsers: number;
  totalAgents: number;
  totalSupervisors: number;
  totalAdmins: number;

  totalForms: number;
  publishedForms: number;

  totalCollectes: number;
  pendingCollectes: number;
  validatedCollectes: number;
  rejectedCollectes: number;

  collecteTrend: DailyCountEntry[];
  rejectionByAgent: AgentRejectionRate[];
  avgValidationTimeHours: number | null;
}