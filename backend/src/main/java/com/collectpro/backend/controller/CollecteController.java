package com.collectpro.backend.controller;

import com.collectpro.backend.dto.*;
import com.collectpro.backend.entity.CollecteAttachment;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.CollecteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/collectes")
@RequiredArgsConstructor
public class CollecteController {

    private final CollecteService collecteService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@securityAuth.hasRole(authentication, 'AGENT')")
    public ResponseEntity<CollecteResponse> createCollecte(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateCollecteRequest request) {
        CollecteResponse response = collecteService.createCollecte(request, principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@securityAuth.hasRole(authentication, 'AGENT')")
    public ResponseEntity<CollecteResponse> createCollecteWithFiles(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestPart("data") @Valid CreateCollecteRequest request,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        CollecteResponse response = collecteService.createCollecte(
                request, principal.getUser(), files != null ? files : List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'AGENT')")
    public ResponseEntity<List<CollecteResponse>> getMyCollectes(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(collecteService.getCollectesForAgent(principal.getUser()));
    }

    @GetMapping("/team")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'SUPERVISOR')")
    public ResponseEntity<List<CollecteResponse>> getTeamCollectes(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(collecteService.getCollectesForSupervisor(principal.getUser()));
    }

    @GetMapping("/team/pending-validation")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'SUPERVISOR')")
    public ResponseEntity<List<CollecteResponse>> getPendingValidation(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(collecteService.getPendingValidationForSupervisor(principal.getUser()));
    }

    @GetMapping("/organization")
    @PreAuthorize("@securityAuth.hasAnyRole(authentication, 'ADMIN_PRINCIPAL', 'ADMIN_SECONDAIRE', 'SUPER_ADMIN')")
    public ResponseEntity<List<CollecteResponse>> getOrganizationCollectes(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(collecteService.getCollectesForAdmin(principal.getUser()));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'SUPERVISOR')")
    public ResponseEntity<CollecteResponse> validateCollecte(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody(required = false) ValidateCollecteRequest request) {
        ValidateCollecteRequest body = request != null ? request : new ValidateCollecteRequest();
        return ResponseEntity.ok(collecteService.validateCollecte(id, principal.getUser(), body));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'SUPERVISOR')")
    public ResponseEntity<CollecteResponse> rejectCollecte(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody RejectCollecteRequest request) {
        return ResponseEntity.ok(collecteService.rejectCollecte(id, principal.getUser(), request));
    }

    @GetMapping("/{id}/validations")
    public ResponseEntity<List<ValidationResponse>> getValidationHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "user") User requester) {
        return ResponseEntity.ok(collecteService.getValidationHistory(id, requester));
    }
}
