export interface OrganizationResponse {
  id: number;
  name: string;
  description?: string;
  status: 'ACTIVE' | 'INACTIVE'; // ⚠️ corrigé : 'SUSPENDED' n'existe pas côté backend
  createdAt: string;
  principalAdmin?: {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    status: string;
  };
}

export interface CreateOrganizationRequest {
  name: string;
  description?: string;
  adminEmail: string;
  adminFirstName: string;
  adminLastName: string;
  adminPhone?: string;
}

export interface UpdateOrganizationRequest {
  name: string;
  description?: string;
}

export interface UpdateOrganizationStatusRequest {
  status: 'ACTIVE' | 'INACTIVE';
}