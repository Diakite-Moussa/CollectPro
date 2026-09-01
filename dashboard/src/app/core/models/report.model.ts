export type ReportType = 'ORGANIZATION' | 'MISSION';

export interface GeneratedBySummary {
  id: number;
  firstName: string;
  lastName: string;
}

export interface ReportResponse {
  id: number;
  organizationId: number;
  missionId: number | null;
  type: ReportType;
  filePath: string;
  generatedBy: GeneratedBySummary | null;
  generatedAt: string;
  periodStart: string | null;
  periodEnd: string | null;
}

export interface GenerateReportRequest {
  periodStart?: string | null;
  periodEnd?: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
