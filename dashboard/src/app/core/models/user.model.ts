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

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface UpdateUserStatusRequest {
  status: 'ACTIVE' | 'DISABLED';
}

/** Fix #12 — représente la réponse paginée retournée par Spring Page<UserResponse>. */
export interface UserResponsePage {
  content: UserResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}