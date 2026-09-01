package com.collectpro.backend.service;

import com.collectpro.backend.dto.GenerateReportRequest;
import com.collectpro.backend.dto.ReportResponse;
import com.collectpro.backend.dto.report.ReportData;
import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Report;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.ReportType;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.MissionRepository;
import com.collectpro.backend.repository.OrganizationRepository;
import com.collectpro.backend.repository.ReportRepository;
import com.collectpro.backend.service.report.MissionReportGenerator;
import com.collectpro.backend.service.report.OrganizationReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final OrganizationRepository organizationRepository;
    private final MissionRepository missionRepository;
    private final ReportRepository reportRepository;
    private final ReportDataService reportDataService;
    private final OrganizationReportGenerator organizationReportGenerator;
    private final MissionReportGenerator missionReportGenerator;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;

    @Transactional
    public ReportResponse generateOrganizationReport(User actor, Long organizationId, GenerateReportRequest request) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : ID " + organizationId));

        validateAccess(actor, organizationId);

        try {
            ReportData data = reportDataService.collectOrganizationReportData(
                    organizationId,
                    request != null ? request.getPeriodStart() : null,
                    request != null ? request.getPeriodEnd() : null
            );

            byte[] pdfBytes = organizationReportGenerator.generate(data);

            String filename = "rapport-org-" + organizationId + ".pdf";
            FileStorageService.StoredFile stored = fileStorageService.saveReportBytes(organizationId, pdfBytes, filename);

            Report report = Report.builder()
                    .organization(organization)
                    .type(ReportType.ORGANIZATION)
                    .filePath(stored.storedFilename())
                    .generatedBy(actor)
                    .periodStart(request != null ? request.getPeriodStart() : null)
                    .periodEnd(request != null ? request.getPeriodEnd() : null)
                    .build();

            Report saved = reportRepository.save(report);

            auditLogService.log(
                    actor,
                    organization,
                    AuditAction.REPORT_GENERATED,
                    "Report",
                    saved.getId(),
                    "Génération du rapport d'organisation #" + organizationId
            );

            return toResponse(saved);
        } catch (IOException e) {
            log.error("Erreur lors de la génération du rapport PDF pour l'organisation {}", organizationId, e);
            throw new BusinessRuleException("Échec de génération du rapport PDF : " + e.getMessage());
        }
    }

    @Transactional
    public ReportResponse generateMissionReport(User actor, Long missionId, GenerateReportRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : ID " + missionId));

        Organization organization = mission.getOrganization();
        validateAccess(actor, organization.getId());

        try {
            ReportData data = reportDataService.collectMissionReportData(
                    missionId,
                    request != null ? request.getPeriodStart() : null,
                    request != null ? request.getPeriodEnd() : null
            );

            byte[] pdfBytes = missionReportGenerator.generate(data);

            String filename = "rapport-mission-" + missionId + ".pdf";
            FileStorageService.StoredFile stored = fileStorageService.saveReportBytes(organization.getId(), pdfBytes, filename);

            Report report = Report.builder()
                    .organization(organization)
                    .mission(mission)
                    .type(ReportType.MISSION)
                    .filePath(stored.storedFilename())
                    .generatedBy(actor)
                    .periodStart(request != null ? request.getPeriodStart() : null)
                    .periodEnd(request != null ? request.getPeriodEnd() : null)
                    .build();

            Report saved = reportRepository.save(report);

            auditLogService.log(
                    actor,
                    organization,
                    AuditAction.REPORT_GENERATED,
                    "Report",
                    saved.getId(),
                    "Génération du rapport de mission #" + missionId + " (" + mission.getName() + ")"
            );

            return toResponse(saved);
        } catch (IOException e) {
            log.error("Erreur lors de la génération du rapport PDF pour la mission {}", missionId, e);
            throw new BusinessRuleException("Échec de génération du rapport PDF : " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getOrganizationReports(User actor, Long organizationId, Pageable pageable) {
        validateAccess(actor, organizationId);
        return reportRepository.findByOrganizationIdOrderByGeneratedAtDesc(organizationId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> getMissionReports(User actor, Long missionId, Pageable pageable) {
        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable : ID " + missionId));
        validateAccess(actor, mission.getOrganization().getId());
        return reportRepository.findByMissionIdOrderByGeneratedAtDesc(missionId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReportFile loadReportFile(User actor, Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport introuvable : ID " + reportId));

        validateAccess(actor, report.getOrganization().getId());

        try {
            Path path = fileStorageService.resolvePath(report.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("Fichier rapport introuvable sur le disque");
            }

            String entitySuffix = report.getType() == ReportType.MISSION && report.getMission() != null
                    ? "mission-" + report.getMission().getId()
                    : "org-" + report.getOrganization().getId();
            String filename = "rapport-" + entitySuffix + "-" + report.getId() + ".pdf";

            return new ReportFile(resource, filename);
        } catch (MalformedURLException e) {
            throw new BusinessRuleException("Chemin de fichier rapport invalide");
        }
    }

    private void validateAccess(User actor, Long organizationId) {
        boolean isSuperAdmin = actor.getRole() != null && actor.getRole().getName() == RoleType.SUPER_ADMIN;
        if (!isSuperAdmin && (actor.getOrganization() == null || !actor.getOrganization().getId().equals(organizationId))) {
            throw new AccessDeniedException("Accès non autorisé à cette organisation");
        }
    }

    private ReportResponse toResponse(Report report) {
        ReportResponse.GeneratedBySummary userSummary = null;
        if (report.getGeneratedBy() != null) {
            userSummary = ReportResponse.GeneratedBySummary.builder()
                    .id(report.getGeneratedBy().getId())
                    .firstName(report.getGeneratedBy().getFirstName())
                    .lastName(report.getGeneratedBy().getLastName())
                    .build();
        }

        return ReportResponse.builder()
                .id(report.getId())
                .organizationId(report.getOrganization().getId())
                .missionId(report.getMission() != null ? report.getMission().getId() : null)
                .type(report.getType())
                .filePath(report.getFilePath())
                .generatedBy(userSummary)
                .generatedAt(report.getGeneratedAt())
                .periodStart(report.getPeriodStart())
                .periodEnd(report.getPeriodEnd())
                .build();
    }

    public record ReportFile(Resource resource, String filename) {}
}
