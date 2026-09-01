package com.collectpro.backend.controller;

import com.collectpro.backend.dto.*;
import com.collectpro.backend.entity.CollecteAttachment;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
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
    private final com.collectpro.backend.service.CollecteExportService exportService;

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
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long missionId) {
        return ResponseEntity.ok(collecteService.getCollectesForSupervisor(principal.getUser(), missionId));
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
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long missionId) {
        return ResponseEntity.ok(collecteService.getCollectesForAdmin(principal.getUser(), missionId));
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

    @PutMapping("/{id}/resubmit")
    @PreAuthorize("@securityAuth.hasRole(authentication, 'AGENT')")
    public ResponseEntity<CollecteResponse> resubmitCollecte(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ResubmitCollecteRequest request) {
        return ResponseEntity.ok(collecteService.resubmitCollecte(id, principal.getUser(), request));
    }

    @GetMapping("/export/csv")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_COLLECTE')")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long missionId,
            @RequestParam(required = false) Long formId,
            @RequestParam(required = false) CollecteStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(required = false) String search) {

        CollecteExportFilter filter = CollecteExportFilter.builder()
                .missionId(missionId)
                .formId(formId)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .search(search)
                .build();

        byte[] csv = exportService.exportCsv(principal.getUser(), organizationId, filter);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"collectes_export.csv\"")
                .body(csv);
    }

    @GetMapping("/export/excel")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_COLLECTE')")
    public ResponseEntity<byte[]> exportExcel(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long missionId,
            @RequestParam(required = false) Long formId,
            @RequestParam(required = false) CollecteStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime endDate,
            @RequestParam(required = false) String search) throws java.io.IOException {

        CollecteExportFilter filter = CollecteExportFilter.builder()
                .missionId(missionId)
                .formId(formId)
                .status(status)
                .startDate(startDate)
                .endDate(endDate)
                .search(search)
                .build();

        byte[] excel = exportService.exportExcel(principal.getUser(), organizationId, filter);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"collectes_export.xlsx\"")
                .body(excel);
    }
}
