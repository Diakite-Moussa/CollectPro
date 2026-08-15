export interface PermissionResponse {
  code: string;
  description: string;
}

export interface UserPermissionsResponse {
  userId: number;
  role: string;
  rolePermissionCodes: string[];
  individualPermissionCodes: string[];
  effectivePermissionCodes: string[];
}
