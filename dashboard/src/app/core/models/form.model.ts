export interface FormResponse {
  id: number;
  name: string;
  description?: string;
  status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
  createdAt: string;
  organizationId?: number;
  latestVersionNumber?: number;
}

export interface CreateFormRequest {
  name: string;
  description?: string;
}

export interface FormVersionResponse {
  id: number;
  formId: number;
  versionNumber: number;
  schemaJson: string;
  createdAt: string;
  createdById: number;
}

export interface CreateFormVersionRequest {
  schemaJson: string;
}
