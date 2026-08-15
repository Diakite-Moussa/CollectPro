export interface OrganizationResponse {
  id: number;
  name: string;
  description?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
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