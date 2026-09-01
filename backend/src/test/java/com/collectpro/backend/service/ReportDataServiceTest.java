package com.collectpro.backend.service;

import com.collectpro.backend.dto.report.ReportData;
import com.collectpro.backend.entity.*;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.MissionStatus;
import com.collectpro.backend.enums.ReportType;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.MissionAgentRepository;
import com.collectpro.backend.repository.MissionRepository;
import com.collectpro.backend.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportDataServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private MissionAgentRepository missionAgentRepository;

    @Mock
    private CollecteRepository collecteRepository;

    @InjectMocks
    private ReportDataService reportDataService;

    private Organization org;
    private User agent;
    private Form form;
    private FormVersion formVersion;

    @BeforeEach
    void setUp() {
        org = Organization.builder()
                .id(1L)
                .name("ONG Santé")
                .description("Santé pour tous")
                .logoUrl("organizations/1/logo/abc.png")
                .build();

        Role agentRole = Role.builder().id(2L).name(RoleType.AGENT).build();
        agent = User.builder()
                .id(10L)
                .firstName("Jean")
                .lastName("Dupont")
                .email("jean@test.com")
                .organization(org)
                .role(agentRole)
                .build();

        form = Form.builder().id(5L).name("Formulaire Enquête").organization(org).build();
        formVersion = FormVersion.builder().id(50L).form(form).versionNumber(1).build();
    }

    @Test
    @DisplayName("collectOrganizationReportData - Agrège correctement les KPIs et l'activité")
    void collectOrganizationReportData_Success() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusDays(7);
        LocalDateTime end = now;

        Collecte c1 = Collecte.builder()
                .id(101L)
                .agent(agent)
                .formVersion(formVersion)
                .status(CollecteStatus.VALIDATED)
                .createdAt(now.minusDays(2))
                .validatedAt(now.minusDays(2).plusHours(2))
                .build();

        Collecte c2 = Collecte.builder()
                .id(102L)
                .agent(agent)
                .formVersion(formVersion)
                .status(CollecteStatus.REJECTED)
                .createdAt(now.minusDays(1))
                .build();

        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));
        when(collecteRepository.findByAgent_OrganizationIdAndCreatedAtBetween(1L, start, end))
                .thenReturn(List.of(c1, c2));

        ReportData data = reportDataService.collectOrganizationReportData(1L, start, end);

        assertNotNull(data);
        assertEquals(ReportType.ORGANIZATION, data.type());
        assertEquals("ONG Santé", data.organization().name());
        assertEquals("/files/organizations/1/logo", data.organization().logoUrl());
        assertEquals("organizations/1/logo/abc.png", data.organization().logoStoredFilename());
        assertNull(data.mission());
        assertEquals(2, data.kpis().totalCollectes());
        assertEquals(1, data.kpis().validatedCollectes());
        assertEquals(1, data.kpis().rejectedCollectes());
        assertEquals(50.0, data.kpis().validationRate());
        assertEquals(50.0, data.kpis().rejectionRate());
        assertEquals(2.0, data.kpis().avgValidationTimeHours());

        assertEquals(1, data.agentsActivity().size());
        assertEquals("Jean Dupont", data.agentsActivity().get(0).agentName());
        assertEquals(2, data.agentsActivity().get(0).total());

        assertEquals(1, data.formsActivity().size());
        assertEquals("Formulaire Enquête", data.formsActivity().get(0).formName());
        assertEquals(2, data.formsActivity().get(0).totalCollectes());
    }

    @Test
    @DisplayName("collectMissionReportData - Agrège correctement les KPIs avec informations de mission")
    void collectMissionReportData_Success() {
        LocalDateTime now = LocalDateTime.now();

        Mission mission = Mission.builder()
                .id(20L)
                .name("Campagne Vaccination")
                .description("Vaccination 2026")
                .organization(org)
                .status(MissionStatus.ACTIVE)
                .expectedCollectesCount(100)
                .startDate(now.minusDays(10))
                .endDate(now.plusDays(10))
                .build();

        Collecte c1 = Collecte.builder()
                .id(201L)
                .agent(agent)
                .formVersion(formVersion)
                .mission(mission)
                .status(CollecteStatus.VALIDATED)
                .createdAt(now.minusDays(1))
                .validatedAt(now.minusDays(1).plusHours(4))
                .build();

        when(missionRepository.findById(20L)).thenReturn(Optional.of(mission));
        when(missionAgentRepository.countByMissionId(20L)).thenReturn(5L);
        when(collecteRepository.findByMissionId(20L)).thenReturn(List.of(c1));

        ReportData data = reportDataService.collectMissionReportData(20L, null, null);

        assertNotNull(data);
        assertEquals(ReportType.MISSION, data.type());
        assertNotNull(data.mission());
        assertEquals("Campagne Vaccination", data.mission().name());
        assertEquals(100, data.mission().expectedCollectes());
        assertEquals(5L, data.mission().assignedAgentsCount());
        assertEquals(1, data.kpis().totalCollectes());
        assertEquals(1, data.kpis().validatedCollectes());
        assertEquals(100.0, data.kpis().validationRate());
        assertEquals(4.0, data.kpis().avgValidationTimeHours());
    }
}
