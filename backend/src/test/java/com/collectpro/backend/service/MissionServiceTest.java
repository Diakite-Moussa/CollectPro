package com.collectpro.backend.service;

import com.collectpro.backend.dto.MissionProgressResponse;
import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.MissionAgent;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.MissionStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.*;
import com.collectpro.backend.repository.projection.MissionAgentCountProjection;
import com.collectpro.backend.repository.projection.MissionProgressProjection;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock private MissionRepository missionRepository;
    @Mock private MissionAgentRepository missionAgentRepository;
    @Mock private FormRepository formRepository;
    @Mock private UserRepository userRepository;
    @Mock private CollecteRepository collecteRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private MissionService missionService;

    private Organization orgA;
    private Organization orgB;
    private User adminOrgA;
    private User adminOrgB;
    private User superAdmin;
    private Mission missionOrgA;

    @BeforeEach
    void setUp() {
        orgA = Organization.builder().id(1L).name("Org A").build();
        orgB = Organization.builder().id(2L).name("Org B").build();

        Role adminRole = Role.builder().id(1L).name(RoleType.ADMIN_PRINCIPAL).build();
        Role superAdminRole = Role.builder().id(2L).name(RoleType.SUPER_ADMIN).build();

        adminOrgA = User.builder().id(10L).organization(orgA).role(adminRole).build();
        adminOrgB = User.builder().id(20L).organization(orgB).role(adminRole).build();
        superAdmin = User.builder().id(1L).role(superAdminRole).build();

        missionOrgA = Mission.builder()
                .id(100L)
                .name("Mission Org A")
                .organization(orgA)
                .status(MissionStatus.ACTIVE)
                .expectedCollectesCount(10)
                .createdBy(adminOrgA)
                .build();
    }

    // ==========================================================
    // Sécurité RBAC — getMissionProgress (mission unique)
    // ==========================================================

    @Test
    @DisplayName("getMissionProgress() - Un admin d'une autre organisation ne peut pas consulter la progression")
    void getMissionProgress_AdminFromDifferentOrganization_ThrowsForbidden() {
        when(missionRepository.findById(100L)).thenReturn(Optional.of(missionOrgA));

        assertThrows(ForbiddenOperationException.class, () ->
                missionService.getMissionProgress(100L, adminOrgB)
        );
    }

    @Test
    @DisplayName("getMissionProgress() - Le SUPER_ADMIN peut consulter n'importe quelle mission")
    void getMissionProgress_SuperAdmin_AlwaysAllowed() {
        when(missionRepository.findById(100L)).thenReturn(Optional.of(missionOrgA));
        when(collecteRepository.findProgressByMissionIds(List.of(100L))).thenReturn(List.of());
        when(missionAgentRepository.countByMissionId(100L)).thenReturn(0L);

        MissionProgressResponse response = missionService.getMissionProgress(100L, superAdmin);

        assertEquals(100L, response.getMissionId());
        assertEquals(10, response.getExpectedCollectesCount());
    }

    @Test
    @DisplayName("getMissionProgress() - L'admin de la même organisation peut consulter")
    void getMissionProgress_SameOrganizationAdmin_Allowed() {
        when(missionRepository.findById(100L)).thenReturn(Optional.of(missionOrgA));
        when(collecteRepository.findProgressByMissionIds(List.of(100L))).thenReturn(List.of());
        when(missionAgentRepository.countByMissionId(100L)).thenReturn(0L);

        MissionProgressResponse response = missionService.getMissionProgress(100L, adminOrgA);

        assertEquals("Mission Org A", response.getMissionName());
    }

    @Test
    @DisplayName("getMissionProgress() - Mission inexistante renvoie une 404 explicite")
    void getMissionProgress_MissionNotFound_ThrowsResourceNotFound() {
        when(missionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                missionService.getMissionProgress(999L, adminOrgA)
        );
    }

    // ==========================================================
    // Sécurité RBAC — getMissionsProgress (vue liste)
    // ==========================================================

    @Test
    @DisplayName("getMissionsProgress() - Un admin ne voit jamais les missions d'une autre organisation")
    void getMissionsProgress_Admin_NeverSeesOtherOrganizationMissions() {
        // L'admin de l'organisation B ne doit obtenir QUE les missions de findByOrganization(orgB),
        // jamais la mission de l'organisation A même si elle existe en base.
        when(missionRepository.findByOrganization(orgB)).thenReturn(List.of());

        List<MissionProgressResponse> result = missionService.getMissionsProgress(adminOrgB);

        assertTrue(result.isEmpty());
        // Vérifie explicitement qu'on n'a jamais interrogé missionRepository.findAll(),
        // qui renverrait les missions de toutes les organisations sans filtrage.
        org.mockito.Mockito.verify(missionRepository, org.mockito.Mockito.never()).findAll();
    }

    @Test
    @DisplayName("getMissionsProgress() - SUPER_ADMIN voit toutes les missions, toutes organisations confondues")
    void getMissionsProgress_SuperAdmin_SeesAllOrganizations() {
        Mission missionOrgB = Mission.builder()
                .id(200L).name("Mission Org B").organization(orgB)
                .status(MissionStatus.ACTIVE).build();

        when(missionRepository.findAll()).thenReturn(List.of(missionOrgA, missionOrgB));
        when(collecteRepository.findProgressByMissionIds(anyList())).thenReturn(List.of());
        when(missionAgentRepository.countAssignedAgentsByMissionIds(anyList())).thenReturn(List.of());

        List<MissionProgressResponse> result = missionService.getMissionsProgress(superAdmin);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("getMissionsProgress() - Agrège correctement les compteurs par mission (pas de N+1)")
    void getMissionsProgress_AggregatesCountsCorrectly() {
        when(missionRepository.findByOrganization(orgA)).thenReturn(List.of(missionOrgA));

        MissionProgressProjection progressProjection = mockProgressProjection(100L, 4L, 2L);
        when(collecteRepository.findProgressByMissionIds(List.of(100L)))
                .thenReturn(List.of(progressProjection));

        MissionAgentCountProjection agentCountProjection = mockAgentCountProjection(100L, 3L);
        when(missionAgentRepository.countAssignedAgentsByMissionIds(List.of(100L)))
                .thenReturn(List.of(agentCountProjection));

        List<MissionProgressResponse> result = missionService.getMissionsProgress(adminOrgA);

        assertEquals(1, result.size());
        MissionProgressResponse response = result.get(0);
        assertEquals(4L, response.getReceivedCollectesCount());
        assertEquals(2L, response.getActiveAgentsCount());
        assertEquals(3L, response.getAssignedAgentsCount());
        assertEquals(40.0, response.getProgressPercent()); // 4/10 * 100
    }

    @Test
    @DisplayName("getMissionsProgress() - Une organisation sans mission renvoie une liste vide sans appel superflu")
    void getMissionsProgress_NoMissions_ReturnsEmptyWithoutFurtherCalls() {
        when(missionRepository.findByOrganization(orgA)).thenReturn(List.of());

        List<MissionProgressResponse> result = missionService.getMissionsProgress(adminOrgA);

        assertTrue(result.isEmpty());
        // Aucun appel groupé ne doit être fait s'il n'y a aucune mission à agréger.
        org.mockito.Mockito.verify(collecteRepository, org.mockito.Mockito.never())
                .findProgressByMissionIds(anyList());
    }

    // ==========================================================
    // Cas limites de calcul
    // ==========================================================

    @Test
    @DisplayName("getMissionProgress() - Objectif non défini : progressPercent est null, pas une erreur")
    void getMissionProgress_NoExpectedCount_ProgressPercentIsNull() {
        Mission missionWithoutTarget = Mission.builder()
                .id(101L).name("Mission Sans Objectif").organization(orgA)
                .status(MissionStatus.DRAFT).expectedCollectesCount(null)
                .build();

        when(missionRepository.findById(101L)).thenReturn(Optional.of(missionWithoutTarget));
        when(collecteRepository.findProgressByMissionIds(List.of(101L))).thenReturn(List.of());
        when(missionAgentRepository.countByMissionId(101L)).thenReturn(0L);

        MissionProgressResponse response = missionService.getMissionProgress(101L, adminOrgA);

        assertNull(response.getProgressPercent());
        assertNull(response.getExpectedCollectesCount());
    }

    @Test
    @DisplayName("getMissionProgress() - Collectes reçues au-delà de l'objectif : progressPercent plafonné à 100")
    void getMissionProgress_OverAchieved_ProgressPercentCappedAt100() {
        when(missionRepository.findById(100L)).thenReturn(Optional.of(missionOrgA)); // objectif = 10
        MissionProgressProjection projection = mockProgressProjection(100L, 15L, 3L); // 15 reçues > 10
        when(collecteRepository.findProgressByMissionIds(List.of(100L))).thenReturn(List.of(projection));
        when(missionAgentRepository.countByMissionId(100L)).thenReturn(3L);

        MissionProgressResponse response = missionService.getMissionProgress(100L, adminOrgA);

        assertEquals(100.0, response.getProgressPercent());
    }

    // ---- Utilitaires de mock pour les interfaces de projection ----

    private MissionProgressProjection mockProgressProjection(Long missionId, Long received, Long activeAgents) {
        MissionProgressProjection projection = org.mockito.Mockito.mock(MissionProgressProjection.class);
        org.mockito.Mockito.lenient().when(projection.getMissionId()).thenReturn(missionId);
        org.mockito.Mockito.lenient().when(projection.getReceivedCount()).thenReturn(received);
        org.mockito.Mockito.lenient().when(projection.getActiveAgentsCount()).thenReturn(activeAgents);
        return projection;
    }

    private MissionAgentCountProjection mockAgentCountProjection(Long missionId, Long count) {
        MissionAgentCountProjection projection = org.mockito.Mockito.mock(MissionAgentCountProjection.class);
        org.mockito.Mockito.lenient().when(projection.getMissionId()).thenReturn(missionId);
        org.mockito.Mockito.lenient().when(projection.getCount()).thenReturn(count);
        return projection;
    }
}