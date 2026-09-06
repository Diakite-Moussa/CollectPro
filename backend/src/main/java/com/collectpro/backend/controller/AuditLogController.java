package com.collectpro.backend.controller;

import com.collectpro.backend.dto.AuditLogResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    /**
     * Fix #12 — pagination serveur.
     * Paramètres supportés : ?page=0&size=25
     */
    @GetMapping
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'SUPER_ADMIN', 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogQueryService.getAuditLogsForUser(principal.getUser(), pageable));
    }
}