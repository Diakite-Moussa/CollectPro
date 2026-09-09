package com.collectpro.backend.service;

import com.collectpro.backend.dto.MissionResponse;
import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.MissionStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.repository.MissionAgentRepository;
import com.collectpro.backend.repository.MissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceActivationTest {

    @Mock private MissionRepository missionRepository;
    @Mock private MissionAgentRepository missionAgentRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private MissionService missionService;

    private Organization orgA;
    private Organization orgB;
    private User adminOrgA;
    private User adminOrgB;
    private Mission draftMission;
    private Mission activeMission;

    @BeforeEach
    void setUp() {
        orgA = Organization.builder().id(1L).name("Santé Plus").build();
        orgB = Organization.builder().id(2L).name("Autre Org").build();

        Role adminRole = Role.builder().id(1L).name(RoleType.ADMIN_PRINCIPAL).build();
        adminOrgA = User.builder().id(10L).organization(orgA).role(adminRole).firstName("Alice").lastName("Dupont").build();
        adminOrgB = User.builder().id(20L).organization(orgB).role(adminRole).firstName("Bob").lastName("Martin").build();

        draftMission = Mission.builder()
                .id(100L)
                .name("Mission Recensement")
                .organization(orgA)
                .status(MissionStatus.DRAFT)
                .createdBy(adminOrgA)
                .build();

        activeMission = Mission.builder()
                .id(101L)
                .name("Mission Vaccination")
                .organization(orgA)
                .status(MissionStatus.ACTIVE)
                .createdBy(adminOrgA)
                .build();
    }

    @Test
    @DisplayName("activateMission() - Passage réussi de DRAFT à ACTIVE")
    void activateMission_FromDraft_Success() {
        when(missionRepository.findById(100L)).thenReturn(Optional.of(draftMission));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(missionAgentRepository.findByMissionId(100L)).thenReturn(Collections.emptyList());

        MissionResponse response = missionService.activateMission(100L, adminOrgA);

        assertNotNull(response);
        assertEquals(MissionStatus.ACTIVE, response.getStatus());
        assertEquals(MissionStatus.ACTIVE, draftMission.getStatus());

        verify(auditLogService).log(
                eq(adminOrgA),
                eq(orgA),
                eq(AuditAction.MISSION_UPDATED),
                eq("Mission"),
                eq(100L),
                contains("Activation de la mission")
        );
    }

    @Test
    @DisplayName("activateMission() - Échoue si la mission est déjà ACTIVE (transition invalide)")
    void activateMission_AlreadyActive_ThrowsBusinessRuleException() {
        when(missionRepository.findById(101L)).thenReturn(Optional.of(activeMission));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                missionService.activateMission(101L, adminOrgA)
        );

        assertTrue(ex.getMessage().contains("Transition de statut invalide"));
        verify(missionRepository, never()).save(any());
    }

    @Test
    @DisplayName("activateMission() - Échoue si l'utilisateur est d'une autre organisation")
    void activateMission_DifferentOrganization_ThrowsForbidden() {
        when(missionRepository.findById(100L)).thenReturn(Optional.of(draftMission));

        assertThrows(ForbiddenOperationException.class, () ->
                missionService.activateMission(100L, adminOrgB)
        );

        verify(missionRepository, never()).save(any());
    }

    @Test
    @DisplayName("completeMission() - Passage réussi de ACTIVE à COMPLETED")
    void completeMission_FromActive_Success() {
        when(missionRepository.findById(101L)).thenReturn(Optional.of(activeMission));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(missionAgentRepository.findByMissionId(101L)).thenReturn(Collections.emptyList());

        MissionResponse response = missionService.completeMission(101L, adminOrgA);

        assertNotNull(response);
        assertEquals(MissionStatus.COMPLETED, response.getStatus());
        assertEquals(MissionStatus.COMPLETED, activeMission.getStatus());

        verify(auditLogService).log(
                eq(adminOrgA),
                eq(orgA),
                eq(AuditAction.MISSION_UPDATED),
                eq("Mission"),
                eq(101L),
                contains("Clôture de la mission")
        );
    }
}
