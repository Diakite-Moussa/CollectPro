package com.collectpro.backend.controller;

import com.collectpro.backend.dto.CreateSyncLogRequest;
import com.collectpro.backend.dto.SyncLogResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.SyncLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sync-logs")
@RequiredArgsConstructor
public class SyncLogController {

    private final SyncLogService syncLogService;

    @PostMapping
    @PreAuthorize("@securityAuth.hasRole(authentication, 'AGENT')")
    public ResponseEntity<SyncLogResponse> recordSyncAttempt(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateSyncLogRequest request) {
        SyncLogResponse response = syncLogService.recordSyncAttempt(request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'AGENT')")
    public ResponseEntity<List<SyncLogResponse>> getMySyncLogs(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(syncLogService.getSyncLogsForAgent(principal.getUser()));
    }

    /**
     * Fix #3 — pagination serveur.
     * Paramètres supportés : ?page=0&size=25&agentId=42
     * Défaut : page 0, taille 25, trié par createdAt DESC.
     */
    @GetMapping
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE', 'SUPERVISOR', 'SUPER_ADMIN')")
    public ResponseEntity<Page<SyncLogResponse>> getSyncLogs(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long agentId,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(syncLogService.getSyncLogsForRequester(principal.getUser(), agentId, pageable));
    }

}
