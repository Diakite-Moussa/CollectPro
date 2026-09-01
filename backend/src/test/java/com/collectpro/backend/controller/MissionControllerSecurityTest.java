package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.dto.AssignAgentsToMissionRequest;
import com.collectpro.backend.dto.AssignFormsToMissionRequest;
import com.collectpro.backend.dto.CreateMissionRequest;
import com.collectpro.backend.dto.MissionProgressResponse;
import com.collectpro.backend.dto.MissionResponse;
import com.collectpro.backend.dto.UpdateMissionRequest;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.MissionStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.MissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MissionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class MissionControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private MissionService missionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private UsernamePasswordAuthenticationToken adminAuth;
    private UsernamePasswordAuthenticationToken agentAuth;
    private UsernamePasswordAuthenticationToken supervisorAuth;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder().id(1L).name("Org").build();

        Role adminRole = Role.builder().id(1L).name(RoleType.ADMIN_PRINCIPAL).build();
        Role agentRole = Role.builder().id(2L).name(RoleType.AGENT).build();
        Role supervisorRole = Role.builder().id(3L).name(RoleType.SUPERVISOR).build();

        User admin = User.builder().id(10L).organization(org).role(adminRole).build();
        User agentUser = User.builder().id(20L).organization(org).role(agentRole).build();
        User supervisor = User.builder().id(30L).organization(org).role(supervisorRole).build();

        CustomUserDetails adminDetails = new CustomUserDetails(admin);
        adminAuth = new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities());

        CustomUserDetails agentDetails = new CustomUserDetails(agentUser);
        agentAuth = new UsernamePasswordAuthenticationToken(agentDetails, null, agentDetails.getAuthorities());

        CustomUserDetails supervisorDetails = new CustomUserDetails(supervisor);
        supervisorAuth = new UsernamePasswordAuthenticationToken(supervisorDetails, null, supervisorDetails.getAuthorities());
    }

    // ---------- CREATE_MISSION ----------

    @Test
    @DisplayName("POST /missions - Autorisé si CREATE_MISSION accordé")
    void createMission_WithPermission_ReturnsCreated() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("CREATE_MISSION"))).thenReturn(true);
        when(missionService.createMission(any(), any(), any()))
                .thenReturn(MissionResponse.builder().id(1L).name("Mission").status(MissionStatus.DRAFT).build());

        CreateMissionRequest request = new CreateMissionRequest();
        request.setName("Mission");

        mockMvc.perform(post("/missions")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /missions - Refusé si CREATE_MISSION non accordé (ex: AGENT)")
    void createMission_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("CREATE_MISSION"))).thenReturn(false);

        CreateMissionRequest request = new CreateMissionRequest();
        request.setName("Mission");

        mockMvc.perform(post("/missions")
                        .principal(agentAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(missionService, never()).createMission(any(), any(), any());
    }

    // ---------- VIEW_MISSION ----------

    @Test
    @DisplayName("GET /missions - Autorisé si VIEW_MISSION accordé, agent -> missions assignées")
    void getMissions_AsAgent_ReturnsAssignedMissions() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("VIEW_MISSION"))).thenReturn(true);
        when(missionService.getMissionsForAgent(any())).thenReturn(List.of());

        mockMvc.perform(get("/missions").principal(agentAuth))
                .andExpect(status().isOk());

        verify(missionService).getMissionsForAgent(any());
        verify(missionService, never()).getMissionsForOrganization(any());
    }

    @Test
    @DisplayName("GET /missions - Refusé si VIEW_MISSION non accordé")
    void getMissions_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("VIEW_MISSION"))).thenReturn(false);

        mockMvc.perform(get("/missions").principal(adminAuth))
                .andExpect(status().isForbidden());

        verify(missionService, never()).getMissionsForOrganization(any());
        verify(missionService, never()).getMissionsForAgent(any());
    }

    @Test
    @DisplayName("GET /missions/{id} - Refusé si VIEW_MISSION non accordé")
    void getMission_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("VIEW_MISSION"))).thenReturn(false);

        mockMvc.perform(get("/missions/100").principal(agentAuth))
                .andExpect(status().isForbidden());

        verify(missionService, never()).getMission(anyLong(), any());
    }

    @Test
    @DisplayName("GET /missions/{id} - Autorisé si VIEW_MISSION accordé")
    void getMission_WithPermission_ReturnsOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("VIEW_MISSION"))).thenReturn(true);
        when(missionService.getMission(anyLong(), any()))
                .thenReturn(MissionResponse.builder().id(100L).name("Mission").build());

        mockMvc.perform(get("/missions/100").principal(agentAuth))
                .andExpect(status().isOk());

        verify(missionService).getMission(anyLong(), any());
    }

    @Test
    @DisplayName("GET /missions/progress - Autorisé si VIEW_MISSION accordé")
    void getMissionsProgress_WithPermission_ReturnsOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("VIEW_MISSION"))).thenReturn(true);
        when(missionService.getMissionsProgress(any())).thenReturn(List.of());

        mockMvc.perform(get("/missions/progress").principal(adminAuth))
                .andExpect(status().isOk());

        verify(missionService).getMissionsProgress(any());
    }

    @Test
    @DisplayName("GET /missions/{id}/progress - Autorisé si VIEW_MISSION accordé")
    void getMissionProgress_WithPermission_ReturnsOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("VIEW_MISSION"))).thenReturn(true);
        when(missionService.getMissionProgress(anyLong(), any()))
                .thenReturn(MissionProgressResponse.builder().missionId(100L).missionName("Mission").build());

        mockMvc.perform(get("/missions/100/progress").principal(adminAuth))
                .andExpect(status().isOk());

        verify(missionService).getMissionProgress(anyLong(), any());
    }

    @Test
    @DisplayName("GET /missions - SUPER_ADMIN reçoit la vue globale (toutes organisations)")
    void getMissions_AsSuperAdmin_ReturnsAllMissions() throws Exception {
        Role superAdminRole = Role.builder().id(4L).name(RoleType.SUPER_ADMIN).build();
        User superAdminUser = User.builder().id(99L).role(superAdminRole).build();
        CustomUserDetails superAdminDetails = new CustomUserDetails(superAdminUser);
        UsernamePasswordAuthenticationToken superAdminAuth =
                new UsernamePasswordAuthenticationToken(superAdminDetails, null, superAdminDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(superAdminAuth);

        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("VIEW_MISSION"))).thenReturn(true);
        when(missionService.getAllMissions()).thenReturn(List.of());

        mockMvc.perform(get("/missions").principal(superAdminAuth))
                .andExpect(status().isOk());

        verify(missionService).getAllMissions();
        verify(missionService, never()).getMissionsForOrganization(any());
    }

    // ---------- UPDATE_MISSION ----------

    @Test
    @DisplayName("PUT /missions/{id} - Refusé si UPDATE_MISSION non accordé (ex: SUPERVISOR)")
    void updateMission_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(supervisorAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("UPDATE_MISSION"))).thenReturn(false);

        UpdateMissionRequest request = new UpdateMissionRequest();
        request.setName("Mission modifiée");

        mockMvc.perform(put("/missions/100")
                        .principal(supervisorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(missionService, never()).updateMission(anyLong(), any(), any());
    }

    @Test
    @DisplayName("PUT /missions/{id} - Autorisé si UPDATE_MISSION accordé")
    void updateMission_WithPermission_ReturnsOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("UPDATE_MISSION"))).thenReturn(true);
        when(missionService.updateMission(anyLong(), any(), any()))
                .thenReturn(MissionResponse.builder().id(100L).name("Mission modifiée").build());

        UpdateMissionRequest request = new UpdateMissionRequest();
        request.setName("Mission modifiée");

        mockMvc.perform(put("/missions/100")
                        .principal(adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---------- CANCEL_MISSION ----------

    @Test
    @DisplayName("PATCH /missions/{id}/cancel - Refusé si CANCEL_MISSION non accordé")
    void cancelMission_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("CANCEL_MISSION"))).thenReturn(false);

        mockMvc.perform(patch("/missions/100/cancel").principal(agentAuth))
                .andExpect(status().isForbidden());

        verify(missionService, never()).cancelMission(anyLong(), any());
    }

    @Test
    @DisplayName("PATCH /missions/{id}/cancel - Autorisé si CANCEL_MISSION accordé")
    void cancelMission_WithPermission_ReturnsOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("CANCEL_MISSION"))).thenReturn(true);
        when(missionService.cancelMission(anyLong(), any()))
                .thenReturn(MissionResponse.builder().id(100L).status(MissionStatus.CANCELLED).build());

        mockMvc.perform(patch("/missions/100/cancel").principal(adminAuth))
                .andExpect(status().isOk());
    }

    // ---------- ASSIGN_MISSION (agents) ----------

    @Test
    @DisplayName("POST /missions/{id}/agents - Refusé si ASSIGN_MISSION non accordé")
    void assignAgents_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("ASSIGN_MISSION"))).thenReturn(false);

        AssignAgentsToMissionRequest request = new AssignAgentsToMissionRequest();
        request.setAgentIds(List.of(20L));

        mockMvc.perform(post("/missions/100/agents")
                        .principal(agentAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(missionService, never()).assignAgents(anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST /missions/{id}/agents - Autorisé si ASSIGN_MISSION accordé (ex: SUPERVISOR)")
    void assignAgents_WithPermission_ReturnsOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(supervisorAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("ASSIGN_MISSION"))).thenReturn(true);
        when(missionService.assignAgents(anyLong(), any(), any()))
                .thenReturn(MissionResponse.builder().id(100L).build());

        AssignAgentsToMissionRequest request = new AssignAgentsToMissionRequest();
        request.setAgentIds(List.of(20L));

        mockMvc.perform(post("/missions/100/agents")
                        .principal(supervisorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /missions/{id}/agents/{agentId} - Refusé si ASSIGN_MISSION non accordé")
    void unassignAgent_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("ASSIGN_MISSION"))).thenReturn(false);

        mockMvc.perform(delete("/missions/100/agents/20").principal(agentAuth))
                .andExpect(status().isForbidden());

        verify(missionService, never()).unassignAgent(anyLong(), anyLong(), any());
    }

    // ---------- ASSIGN_MISSION (forms) ----------

    @Test
    @DisplayName("POST /missions/{id}/forms - Refusé si ASSIGN_MISSION non accordé")
    void assignForms_WithoutPermission_ReturnsForbidden() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(agentAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("ASSIGN_MISSION"))).thenReturn(false);

        AssignFormsToMissionRequest request = new AssignFormsToMissionRequest();
        request.setFormIds(List.of(5L));

        mockMvc.perform(post("/missions/100/forms")
                        .principal(agentAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(missionService, never()).assignForms(anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST /missions/{id}/forms - Autorisé si ASSIGN_MISSION accordé")
    void assignForms_WithPermission_ReturnsOk() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(supervisorAuth);
        when(securityAuth.hasPermission(any(), org.mockito.ArgumentMatchers.eq("ASSIGN_MISSION"))).thenReturn(true);
        when(missionService.assignForms(anyLong(), any(), any()))
                .thenReturn(MissionResponse.builder().id(100L).build());

        AssignFormsToMissionRequest request = new AssignFormsToMissionRequest();
        request.setFormIds(List.of(5L));

        mockMvc.perform(post("/missions/100/forms")
                        .principal(supervisorAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}