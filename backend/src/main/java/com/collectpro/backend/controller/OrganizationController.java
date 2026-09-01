package com.collectpro.backend.controller;

import com.collectpro.backend.dto.CreateOrganizationRequest;
import com.collectpro.backend.dto.OrganizationResponse;
import com.collectpro.backend.dto.UpdateOrganizationRequest;
import com.collectpro.backend.dto.UpdateOrganizationStatusRequest;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @PreAuthorize("@securityAuth.isSuperAdmin(authentication)")
    public ResponseEntity<OrganizationResponse> createOrganization(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationResponse response = organizationService.createOrganizationWithPrincipalAdmin(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@securityAuth.isSuperAdmin(authentication)")
    public ResponseEntity<java.util.List<OrganizationResponse>> getOrganizations() {
        return ResponseEntity.ok(organizationService.getAllOrganizations());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityAuth.isSuperAdmin(authentication)")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        return ResponseEntity.ok(organizationService.updateOrganization(principal.getUser(), id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("@securityAuth.isSuperAdmin(authentication)")
    public ResponseEntity<OrganizationResponse> updateOrganizationStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationStatusRequest request) {
        return ResponseEntity.ok(organizationService.updateOrganizationStatus(principal.getUser(), id, request));
    }

    @PutMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@securityAuth.isSuperAdmin(authentication)")
    public ResponseEntity<OrganizationResponse> updateLogo(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(organizationService.updateLogo(principal.getUser(), id, file));
    }
}