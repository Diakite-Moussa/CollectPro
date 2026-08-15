export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserSummary {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  organizationId?: number;
  organizationName?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserSummary;
}
