export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: 'SUPER_ADMIN' | 'ADMIN_PRINCIPAL' | 'ADMIN_SECONDAIRE' | 'SUPERVISOR' | 'AGENT';
  status: 'INVITED' | 'ACTIVE' | 'DISABLED';
  organizationId?: number;
  organizationName?: string;
  createdAt: string;
  /** Agent déjà affecté à un superviseur */
  assignedSupervisorId?: number;
  assignedSupervisorName?: string;
}

export interface CreateUserRequest {
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  roleType: 'ADMIN_SECONDAIRE' | 'SUPERVISOR' | 'AGENT';
}

export interface AssignSupervisorRequest {
  agentId: number;
  supervisorId: number;
}