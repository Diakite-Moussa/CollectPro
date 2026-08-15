package com.collectpro.backend.controller;

import com.collectpro.backend.dto.PermissionResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * Permissions qu'un Admin principal peut déléguer à un Admin secondaire.
     */
    @GetMapping("/delegatable")
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'ADMIN_PRINCIPAL', 'SUPER_ADMIN')")
    public ResponseEntity<List<PermissionResponse>> getDelegatablePermissions(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(permissionService.getDelegatablePermissions(principal.getUser()));
    }
}
