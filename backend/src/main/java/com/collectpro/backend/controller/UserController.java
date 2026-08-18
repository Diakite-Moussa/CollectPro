package com.collectpro.backend.controller;

import com.collectpro.backend.dto.AssignSupervisorRequest;
import com.collectpro.backend.dto.ChangePasswordRequest;
import com.collectpro.backend.dto.CreateUserRequest;
import com.collectpro.backend.dto.UpdateProfileRequest;
import com.collectpro.backend.dto.UpdateUserPermissionsRequest;
import com.collectpro.backend.dto.UpdateUserStatusRequest;
import com.collectpro.backend.dto.UserPermissionsResponse;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.PermissionService;
import com.collectpro.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserResponse>> getUsers(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getUsers(principal.getUser()));
    }

    @PostMapping
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'CREATE_USER')")
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/assign-supervisor")
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE', 'SUPER_ADMIN')")
    public ResponseEntity<Void> assignSupervisor(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AssignSupervisorRequest request) {
        userService.assignAgentToSupervisor(principal.getUser(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/resend-invitation")
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE', 'SUPER_ADMIN')")
    public ResponseEntity<Void> resendInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        userService.resendInvitation(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'ADMIN_PRINCIPAL', 'SUPER_ADMIN')")
    public ResponseEntity<UserPermissionsResponse> getUserPermissions(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(permissionService.getUserPermissions(principal.getUser(), id));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'ADMIN_PRINCIPAL', 'SUPER_ADMIN')")
    public ResponseEntity<UserPermissionsResponse> updateUserPermissions(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateUserPermissionsRequest request) {
        return ResponseEntity.ok(permissionService.updateUserPermissions(principal.getUser(), id, request));
    }
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getUser()));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(principal.getUser(), request));
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getUser(), request);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/status")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'DISABLE_USER')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.updateUserStatus(principal.getUser(), id, request));
    }

    /**
     * Fix #2 — endpoint dédié pour les Superviseurs : retourne uniquement
     * les Agents qu'ils supervisent, sans charger tous les utilisateurs de l'org.
     */
    @GetMapping("/my-agents")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'SUPERVISOR')")
    public ResponseEntity<List<UserResponse>> getMyAgents(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(userService.getMyAgents(principal.getUser()));
    }

}