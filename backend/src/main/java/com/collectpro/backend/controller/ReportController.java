package com.collectpro.backend.controller;

import com.collectpro.backend.dto.GenerateReportRequest;
import com.collectpro.backend.dto.ReportResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/organizations/{id}")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'GENERATE_REPORT')")
    public ResponseEntity<ReportResponse> generateOrganizationReport(
            @PathVariable Long id,
            @RequestBody(required = false) GenerateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reportService.generateOrganizationReport(principal.getUser(), id, request));
    }

    @PostMapping("/missions/{id}")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'GENERATE_REPORT')")
    public ResponseEntity<ReportResponse> generateMissionReport(
            @PathVariable Long id,
            @RequestBody(required = false) GenerateReportRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reportService.generateMissionReport(principal.getUser(), id, request));
    }

    @GetMapping("/organizations/{id}")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<Page<ReportResponse>> getOrganizationReports(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reportService.getOrganizationReports(principal.getUser(), id, pageable));
    }

    @GetMapping("/missions/{id}")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<Page<ReportResponse>> getMissionReports(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(reportService.getMissionReports(principal.getUser(), id, pageable));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_REPORT')")
    public ResponseEntity<Resource> downloadReport(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ReportService.ReportFile reportFile = reportService.loadReportFile(principal.getUser(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + reportFile.filename() + "\"")
                .body(reportFile.resource());
    }
}
