package com.collectpro.backend.service;

import com.collectpro.backend.dto.GenerateReportRequest;
import com.collectpro.backend.dto.ReportResponse;
import com.collectpro.backend.dto.report.ReportData;
import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Report;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.ReportType;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.MissionRepository;
import com.collectpro.backend.repository.OrganizationRepository;
import com.collectpro.backend.repository.ReportRepository;
import com.collectpro.backend.service.report.MissionReportGenerator;
import com.collectpro.backend.service.report.OrganizationReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportDataService reportDataService;

    @Mock
    private OrganizationReportGenerator organizationReportGenerator;

    @Mock
    private MissionReportGenerator missionReportGenerator;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReportService reportService;

    private Organization org;
    private User admin;
    private User superAdmin;
    private User otherAdmin;
    private Mission mission;

    @BeforeEach
    void setUp() {
        org = Organization.builder()
                .id(1L)
                .name("ONG Test")
                .build();

        Organization otherOrg = Organization.builder()
                .id(2L)
                .name("Autre ONG")
                .build();

        Role adminRole = Role.builder().name(RoleType.ADMIN_PRINCIPAL).build();
        Role superAdminRole = Role.builder().name(RoleType.SUPER_ADMIN).build();

        admin = User.builder()
                .id(10L)
                .firstName("Admin")
                .lastName("Local")
                .organization(org)
                .role(adminRole)
                .build();

        superAdmin = User.builder()
                .id(1L)
                .firstName("Super")
                .lastName("Admin")
                .organization(null)
                .role(superAdminRole)
                .build();

        otherAdmin = User.builder()
                .id(20L)
                .firstName("Autre")
                .lastName("Admin")
                .organization(otherOrg)
                .role(adminRole)
                .build();

        mission = Mission.builder()
                .id(100L)
                .name("Mission Paludisme")
                .organization(org)
                .build();
    }

    @Test
    @DisplayName("generateOrganizationReport - Succès pour admin de l'organisation")
    void generateOrganizationReport_Success() throws IOException {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(reportDataService.collectOrganizationReportData(eq(1L), any(), any()))
                .thenReturn(ReportData.builder().build());
        when(organizationReportGenerator.generate(any())).thenReturn(new byte[]{1, 2, 3});
        when(fileStorageService.saveReportBytes(eq(1L), any(byte[].class), anyString()))
                .thenReturn(new FileStorageService.StoredFile("rapport.pdf", "organizations/1/reports/uuid.pdf", "application/pdf", 3));

        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        ReportResponse response = reportService.generateOrganizationReport(admin, 1L, new GenerateReportRequest());

        assertNotNull(response);
        assertEquals(50L, response.getId());
        assertEquals(1L, response.getOrganizationId());
        assertEquals(ReportType.ORGANIZATION, response.getType());
        assertEquals("organizations/1/reports/uuid.pdf", response.getFilePath());

        verify(auditLogService).log(eq(admin), eq(org), eq(AuditAction.REPORT_GENERATED), eq("Report"), eq(50L), anyString());
    }

    @Test
    @DisplayName("generateOrganizationReport - Interdit pour admin d'une autre organisation")
    void generateOrganizationReport_ForbiddenForOtherOrg() {
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));

        assertThrows(AccessDeniedException.class, () ->
                reportService.generateOrganizationReport(otherAdmin, 1L, new GenerateReportRequest())
        );

        verifyNoInteractions(organizationReportGenerator);
        verifyNoInteractions(reportRepository);
    }

    @Test
    @DisplayName("generateMissionReport - Succès")
    void generateMissionReport_Success() throws IOException {
        when(missionRepository.findById(100L)).thenReturn(Optional.of(mission));
        when(reportDataService.collectMissionReportData(eq(100L), any(), any()))
                .thenReturn(ReportData.builder().build());
        when(missionReportGenerator.generate(any())).thenReturn(new byte[]{4, 5, 6});
        when(fileStorageService.saveReportBytes(eq(1L), any(byte[].class), anyString()))
                .thenReturn(new FileStorageService.StoredFile("rapport.pdf", "organizations/1/reports/uuid2.pdf", "application/pdf", 3));

        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> {
            Report r = inv.getArgument(0);
            r.setId(51L);
            return r;
        });

        ReportResponse response = reportService.generateMissionReport(admin, 100L, new GenerateReportRequest());

        assertNotNull(response);
        assertEquals(51L, response.getId());
        assertEquals(100L, response.getMissionId());
        assertEquals(ReportType.MISSION, response.getType());

        verify(auditLogService).log(eq(admin), eq(org), eq(AuditAction.REPORT_GENERATED), eq("Report"), eq(51L), anyString());
    }

    @Test
    @DisplayName("getOrganizationReports - Retourne la liste paginée")
    void getOrganizationReports_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Report report = Report.builder()
                .id(1L)
                .organization(org)
                .type(ReportType.ORGANIZATION)
                .filePath("path.pdf")
                .generatedBy(admin)
                .build();

        when(reportRepository.findByOrganizationIdOrderByGeneratedAtDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(report)));

        Page<ReportResponse> result = reportService.getOrganizationReports(admin, 1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
    }
}
