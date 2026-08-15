package com.collectpro.backend.controller;

import com.collectpro.backend.dto.AuditLogResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'SUPER_ADMIN', 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE')")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(auditLogQueryService.getAuditLogsForUser(principal.getUser()));
    }
}
