package com.collectpro.backend.service.report;

import com.collectpro.backend.dto.report.ReportData;
import com.collectpro.backend.enums.ReportType;
import com.collectpro.backend.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionReportGeneratorTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private MissionReportGenerator generator;

    @Test
    @DisplayName("generate - Génère un PDF de mission valide avec objectif et agents")
    void generate_MissionReport_ReturnsPdfBytes() throws IOException {
        ReportData.OrganizationReportInfo org = ReportData.OrganizationReportInfo.builder()
                .id(1L)
                .name("ONG Santé Sénégal")
                .logoUrl("/files/organizations/1/logo")
                .logoStoredFilename("organizations/1/logo/sample.png")
                .build();

        ReportData.MissionReportInfo mission = ReportData.MissionReportInfo.builder()
                .id(10L)
                .name("Mission Paludisme 2026")
                .description("Campagne de dépistage")
                .status("ACTIVE")
                .expectedCollectes(500)
                .assignedAgentsCount(12L)
                .startDate(LocalDateTime.now().minusDays(15))
                .endDate(LocalDateTime.now().plusDays(15))
                .build();

        ReportData.ReportKpis kpis = ReportData.ReportKpis.builder()
                .totalCollectes(350)
                .validatedCollectes(300)
                .rejectedCollectes(40)
                .pendingCollectes(10)
                .validationRate(85.7)
                .rejectionRate(11.4)
                .avgValidationTimeHours(1.8)
                .build();

        ReportData.AgentActivitySummary agent = ReportData.AgentActivitySummary.builder()
                .agentId(101L)
                .agentName("Awa Ndiaye")
                .total(50)
                .validated(45)
                .rejected(5)
                .pending(0)
                .build();

        ReportData data = ReportData.builder()
                .type(ReportType.MISSION)
                .organization(org)
                .mission(mission)
                .kpis(kpis)
                .agentsActivity(List.of(agent))
                .formsActivity(List.of())
                .dailyTrend(List.of())
                .build();

        when(fileStorageService.resolvePath(anyString())).thenReturn(Path.of("non/existent/path.png"));

        byte[] pdfBytes = generator.generate(data);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }

    @Test
    @DisplayName("generate - Génère un PDF de mission valide avec données vides et sans dates")
    void generate_EmptyMissionReport_ReturnsPdfBytes() throws IOException {
        ReportData.OrganizationReportInfo org = ReportData.OrganizationReportInfo.builder()
                .id(1L)
                .name("ONG")
                .build();

        ReportData.MissionReportInfo mission = ReportData.MissionReportInfo.builder()
                .id(10L)
                .name("Mission Vide")
                .assignedAgentsCount(0L)
                .build();

        ReportData.ReportKpis kpis = ReportData.ReportKpis.builder()
                .totalCollectes(0)
                .validatedCollectes(0)
                .rejectedCollectes(0)
                .pendingCollectes(0)
                .validationRate(0.0)
                .rejectionRate(0.0)
                .avgValidationTimeHours(0.0)
                .build();

        ReportData data = ReportData.builder()
                .type(ReportType.MISSION)
                .organization(org)
                .mission(mission)
                .kpis(kpis)
                .agentsActivity(List.of())
                .formsActivity(List.of())
                .dailyTrend(List.of())
                .build();

        byte[] pdfBytes = generator.generate(data);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals('%', (char) pdfBytes[0]);
    }
}
