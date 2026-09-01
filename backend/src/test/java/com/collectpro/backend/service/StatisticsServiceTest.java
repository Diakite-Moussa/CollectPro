package com.collectpro.backend.service;

import com.collectpro.backend.dto.StatisticsResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.SupervisorAgent;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.FormStatus;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private FormRepository formRepository;
    @Mock private CollecteRepository collecteRepository;
    @Mock private SupervisorAgentRepository supervisorAgentRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private Organization orgA;
    private User superAdmin;
    private User adminPrincipal;
    private User supervisor;
    private User agent;

    @BeforeEach
    void setUp() {
        orgA = Organization.builder().id(1L).name("Org A").build();

        Role superAdminRole = Role.builder().id(1L).name(RoleType.SUPER_ADMIN).build();
        Role adminPrincipalRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();
        Role supervisorRole = Role.builder().id(3L).name(RoleType.SUPERVISOR).build();
        Role agentRole = Role.builder().id(4L).name(RoleType.AGENT).build();

        superAdmin = User.builder().id(1L).role(superAdminRole).build();
        adminPrincipal = User.builder().id(2L).organization(orgA).role(adminPrincipalRole).build();
        supervisor = User.builder().id(3L).organization(orgA).role(supervisorRole).build();
        agent = User.builder().id(4L).organization(orgA).role(agentRole).build();
    }

    @Test
    @DisplayName("getStatistics() - Super Admin obtient le scope GLOBAL")
    void getStatistics_SuperAdmin_ReturnsGlobalScope() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(organizationRepository.count()).thenReturn(5L);
        when(organizationRepository.countByStatus(OrganizationStatus.ACTIVE)).thenReturn(4L);
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByRole_Name(RoleType.AGENT)).thenReturn(30L);
        when(userRepository.countByRole_Name(RoleType.SUPERVISOR)).thenReturn(10L);
        when(userRepository.countByRole_Name(RoleType.ADMIN_PRINCIPAL)).thenReturn(5L);
        when(userRepository.countByRole_Name(RoleType.ADMIN_SECONDAIRE)).thenReturn(5L);
        when(formRepository.count()).thenReturn(20L);
        when(formRepository.countByStatus(FormStatus.PUBLISHED)).thenReturn(15L);
        when(collecteRepository.count()).thenReturn(200L);
        when(collecteRepository.countByStatus(CollecteStatus.PENDING_VALIDATION)).thenReturn(10L);
        when(collecteRepository.countByStatus(CollecteStatus.VALIDATED)).thenReturn(180L);
        when(collecteRepository.countByStatus(CollecteStatus.REJECTED)).thenReturn(10L);

        StatisticsResponse response = statisticsService.getStatistics(superAdmin, 30);

        assertEquals("GLOBAL", response.getScope());
        assertEquals(5L, response.getTotalOrganizations());
        assertEquals(4L, response.getActiveOrganizations());
        assertEquals(10L, response.getTotalAdmins()); // 5 principaux + 5 secondaires
        assertNull(response.getOrganizationId());
    }

    @Test
    @DisplayName("getStatistics() - Admin principal obtient le scope ORGANIZATION, limité à son organisation")
    void getStatistics_AdminPrincipal_ReturnsOrganizationScope() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminPrincipal));
        when(userRepository.countByOrganizationId(1L)).thenReturn(15L);
        when(userRepository.countByOrganizationIdAndRole_Name(1L, RoleType.AGENT)).thenReturn(10L);
        when(userRepository.countByOrganizationIdAndRole_Name(1L, RoleType.SUPERVISOR)).thenReturn(3L);
        when(userRepository.countByOrganizationIdAndRole_Name(1L, RoleType.ADMIN_PRINCIPAL)).thenReturn(1L);
        when(userRepository.countByOrganizationIdAndRole_Name(1L, RoleType.ADMIN_SECONDAIRE)).thenReturn(1L);
        when(formRepository.countByOrganization_Id(1L)).thenReturn(6L);
        when(formRepository.countByOrganization_IdAndStatus(1L, FormStatus.PUBLISHED)).thenReturn(4L);
        when(collecteRepository.countByAgent_OrganizationId(1L)).thenReturn(80L);
        when(collecteRepository.countByAgent_OrganizationIdAndStatus(1L, CollecteStatus.PENDING_VALIDATION)).thenReturn(5L);
        when(collecteRepository.countByAgent_OrganizationIdAndStatus(1L, CollecteStatus.VALIDATED)).thenReturn(70L);
        when(collecteRepository.countByAgent_OrganizationIdAndStatus(1L, CollecteStatus.REJECTED)).thenReturn(5L);

        StatisticsResponse response = statisticsService.getStatistics(adminPrincipal, 30);

        assertEquals("ORGANIZATION", response.getScope());
        assertEquals(1L, response.getOrganizationId());
        assertEquals(15L, response.getTotalUsers());
        // Aucune fuite de données globales dans un scope organisation
        assertNull(response.getTotalOrganizations());

        // Vérifie qu'aucune requête "globale" (non filtrée par organisation) n'a été appelée
        verify(userRepository, never()).count();
        verify(collecteRepository, never()).count();
    }

    @Test
    @DisplayName("getStatistics() - Admin principal sans organisation : erreur explicite")
    void getStatistics_AdminPrincipalWithoutOrganization_ThrowsResourceNotFound() {
        Role adminRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();
        User orphanAdmin = User.builder().id(99L).organization(null).role(adminRole).build();

        when(userRepository.findById(99L)).thenReturn(Optional.of(orphanAdmin));

        assertThrows(ResourceNotFoundException.class, () ->
                statisticsService.getStatistics(orphanAdmin, 30)
        );
    }

    @Test
    @DisplayName("getStatistics() - Superviseur obtient le scope TEAM, limité à ses agents")
    void getStatistics_Supervisor_ReturnsTeamScopeLimitedToOwnAgents() {
        SupervisorAgent link = SupervisorAgent.builder().supervisor(supervisor).agent(agent).build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(supervisor));
        when(supervisorAgentRepository.findBySupervisorId(3L)).thenReturn(List.of(link));
        when(formRepository.countByOrganization_Id(1L)).thenReturn(6L);
        when(formRepository.countByOrganization_IdAndStatus(1L, FormStatus.PUBLISHED)).thenReturn(4L);
        when(collecteRepository.countByAgentIn(List.of(agent))).thenReturn(25L);
        when(collecteRepository.countByAgentInAndStatus(List.of(agent), CollecteStatus.PENDING_VALIDATION)).thenReturn(3L);
        when(collecteRepository.countByAgentInAndStatus(List.of(agent), CollecteStatus.VALIDATED)).thenReturn(20L);
        when(collecteRepository.countByAgentInAndStatus(List.of(agent), CollecteStatus.REJECTED)).thenReturn(2L);

        StatisticsResponse response = statisticsService.getStatistics(supervisor, 30);

        assertEquals("TEAM", response.getScope());
        assertEquals(1L, response.getTotalAgents()); // Un seul agent supervisé
        assertEquals(25L, response.getTotalCollectes());

        // Sécurité : le superviseur n'a accès qu'aux collectes de SES agents, jamais à collecteRepository.count()
        verify(collecteRepository, never()).count();
    }

    @Test
    @DisplayName("getStatistics() - Échec : un Agent n'a accès à aucune statistique")
    void getStatistics_Agent_ThrowsResourceNotFound() {
        when(userRepository.findById(4L)).thenReturn(Optional.of(agent));

        assertThrows(ResourceNotFoundException.class, () ->
                statisticsService.getStatistics(agent, 30)
        );
    }
}