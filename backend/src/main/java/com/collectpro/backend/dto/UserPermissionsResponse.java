package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserPermissionsResponse {
    private Long userId;
    private String role;
    /** Permissions héritées du rôle (souvent vide pour ADMIN_SECONDAIRE). */
    private List<String> rolePermissionCodes;
    /** Permissions accordées individuellement (user_permissions). */
    private List<String> individualPermissionCodes;
    /** Union effective utilisée par le backend. */
    private List<String> effectivePermissionCodes;
}
