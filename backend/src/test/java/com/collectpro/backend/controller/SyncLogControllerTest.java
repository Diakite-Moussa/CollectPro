package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.dto.CreateSyncLogRequest;
import com.collectpro.backend.dto.SyncLogResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.SyncResult;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.SyncLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SyncLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class SyncLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SyncLogService syncLogService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private User agentUser;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        Organization organization = Organization.builder().id(1L).name("Org").build();
        Role agentRole = Role.builder().id(5L).name(RoleType.AGENT).build();

        agentUser = User.builder()
                .id(1L)
                .email("agent@test.com")
                .firstName("Agent")
                .lastName("Terrain")
                .organization(organization)
                .role(agentRole)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(agentUser);
        authPrincipal = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);

        // Par défaut, tout est autorisé ; chaque test restreint ce qu'il veut vérifier.
        lenient().when(securityAuth.hasRole(any(), any())).thenReturn(true);
        lenient().when(securityAuth.hasAnyRole(any(), any(String[].class))).thenReturn(true);
        lenient().when(securityAuth.hasPermission(any(), any())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SyncLogResponse.AgentSummary agentSummary() {
        return SyncLogResponse.AgentSummary.builder()
                .id(1L).firstName("Agent").lastName("Terrain").build();
    }

    // --- POST /sync-logs ---

    @Test
    @DisplayName("POST /sync-logs - Enregistre une tentative de synchro et retourne 201")
    void recordSyncAttempt_ReturnsCreated() throws Exception {
        CreateSyncLogRequest request = new CreateSyncLogRequest();
        request.setLocalReference("local-ref-001");
        request.setCollecteId(10L);
        request.setResult(SyncResult.SUCCESS);

        SyncLogResponse response = SyncLogResponse.builder()
                .id(1L)
                .agent(agentSummary())
                .localReference("local-ref-001")
                .collecteId(10L)
                .result(SyncResult.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(syncLogService.recordSyncAttempt(any(), any())).thenReturn(response);

        mockMvc.perform(post("/sync-logs")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.localReference").value("local-ref-001"))
                .andExpect(jsonPath("$.result").value("SUCCESS"));
    }

    @Test
    @DisplayName("POST /sync-logs - Refuse une localReference vide (validation)")
    void recordSyncAttempt_BlankLocalReference_ReturnsBadRequest() throws Exception {
        CreateSyncLogRequest request = new CreateSyncLogRequest();
        request.setLocalReference("");
        request.setResult(SyncResult.ERROR);

        mockMvc.perform(post("/sync-logs")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(syncLogService, never()).recordSyncAttempt(any(), any());
    }

    @Test
    @DisplayName("POST /sync-logs - Refuse sans le rôle AGENT")
    void recordSyncAttempt_WithoutAgentRole_ReturnsForbidden() throws Exception {
        when(securityAuth.hasRole(any(), eq("AGENT"))).thenReturn(false);

        CreateSyncLogRequest request = new CreateSyncLogRequest();
        request.setLocalReference("local-ref-001");
        request.setResult(SyncResult.SUCCESS);

        mockMvc.perform(post("/sync-logs")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(syncLogService, never()).recordSyncAttempt(any(), any());
    }

    // --- GET /sync-logs/mine ---

    @Test
    @DisplayName("GET /sync-logs/mine - Retourne les logs de l'agent connecté")
    void getMySyncLogs_ReturnsList() throws Exception {
        SyncLogResponse log = SyncLogResponse.builder()
                .id(1L)
                .agent(agentSummary())
                .localReference("local-ref-001")
                .result(SyncResult.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(syncLogService.getSyncLogsForAgent(any())).thenReturn(List.of(log));

        mockMvc.perform(get("/sync-logs/mine").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].localReference").value("local-ref-001"));
    }

    @Test
    @DisplayName("GET /sync-logs/mine - Refuse sans le rôle AGENT")
    void getMySyncLogs_WithoutAgentRole_ReturnsForbidden() throws Exception {
        when(securityAuth.hasRole(any(), eq("AGENT"))).thenReturn(false);

        mockMvc.perform(get("/sync-logs/mine").principal(authPrincipal))
                .andExpect(status().isForbidden());

        verify(syncLogService, never()).getSyncLogsForAgent(any());
    }

    // --- GET /sync-logs (paginé) ---

    @Test
    @DisplayName("GET /sync-logs - Retourne une page de logs pour un superviseur")
    void getSyncLogs_ReturnsPage() throws Exception {
        SyncLogResponse log = SyncLogResponse.builder()
                .id(1L)
                .agent(agentSummary())
                .localReference("local-ref-001")
                .result(SyncResult.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        when(syncLogService.getSyncLogsForRequester(any(), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/sync-logs").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /sync-logs - Filtre par agentId et transmet la pagination demandée")
    void getSyncLogs_WithAgentIdAndPaging_PassesParamsToService() throws Exception {
        when(syncLogService.getSyncLogsForRequester(any(), eq(42L), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get("/sync-logs")
                        .principal(authPrincipal)
                        .param("agentId", "42")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(syncLogService).getSyncLogsForRequester(any(), eq(42L), any());
    }

    @Test
    @DisplayName("GET /sync-logs - Refuse un rôle non habilité (ex: AGENT)")
    void getSyncLogs_WithoutAllowedRole_ReturnsForbidden() throws Exception {
        when(securityAuth.hasAnyRole(any(), any(String[].class))).thenReturn(false);

        mockMvc.perform(get("/sync-logs").principal(authPrincipal))
                .andExpect(status().isForbidden());

        verify(syncLogService, never()).getSyncLogsForRequester(any(), any(), any());
    }
}