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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationReportGeneratorTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private OrganizationReportGenerator generator;

    @Test
    @DisplayName("generate - Génère un PDF valide avec données complètes")
    void generate_FullData_ReturnsPdfBytes() throws IOException {
        ReportData.OrganizationReportInfo org = ReportData.OrganizationReportInfo.builder()
                .id(1L)
                .name("ONG Santé Sénégal")
                .description("Santé publique")
                .logoUrl("/files/organizations/1/logo")
                .logoStoredFilename("organizations/1/logo/sample.png")
                .build();

        ReportData.ReportKpis kpis = ReportData.ReportKpis.builder()
                .totalCollectes(100)
                .validatedCollectes(80)
                .rejectedCollectes(15)
                .pendingCollectes(5)
                .validationRate(80.0)
                .rejectionRate(15.0)
                .avgValidationTimeHours(2.5)
                .build();

        ReportData.AgentActivitySummary agent = ReportData.AgentActivitySummary.builder()
                .agentId(10L)
                .agentName("Moussa Diop")
                .total(50)
                .validated(40)
                .rejected(8)
                .pending(2)
                .build();

        ReportData.DailyTrendPoint trend = ReportData.DailyTrendPoint.builder()
                .date(LocalDate.now())
                .collectesCount(25)
                .build();

        ReportData data = ReportData.builder()
                .type(ReportType.ORGANIZATION)
                .organization(org)
                .period(ReportData.ReportPeriod.builder()
                        .start(LocalDateTime.now().minusDays(30))
                        .end(LocalDateTime.now())
                        .build())
                .kpis(kpis)
                .agentsActivity(List.of(agent))
                .formsActivity(List.of())
                .dailyTrend(List.of(trend))
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
    @DisplayName("generate - Génère un PDF valide avec données vides et sans logo")
    void generate_EmptyDataNoLogo_ReturnsPdfBytes() throws IOException {
        ReportData.OrganizationReportInfo org = ReportData.OrganizationReportInfo.builder()
                .id(2L)
                .name("Structure Sans Logo")
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
                .type(ReportType.ORGANIZATION)
                .organization(org)
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
