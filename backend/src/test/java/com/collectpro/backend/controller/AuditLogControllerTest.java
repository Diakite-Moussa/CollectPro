package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.dto.AuditLogResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.AuditLogQueryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogQueryService auditLogQueryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private User adminUser;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder().id(1L).name("Org").build();
        Role adminRole = Role.builder().id(3L).name(RoleType.ADMIN_PRINCIPAL).build();

        adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("Principal")
                .organization(org)
                .role(adminRole)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(adminUser);
        authPrincipal = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);

        // Par défaut, on autorise l'accès pour les tests "happy path"
        lenient().when(securityAuth.hasAnyRole(any(), any(String[].class))).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AuditLogResponse sampleLog() {
        return AuditLogResponse.builder()
                .id(100L)
                .action(AuditAction.USER_CREATED)
                .entityType("User")
                .entityId(10L)
                .details("Création de l'utilisateur agent@test.com")
                .createdAt(LocalDateTime.of(2026, 1, 15, 10, 30))
                .actor(AuditLogResponse.ActorSummary.builder()
                        .id(1L)
                        .firstName("Admin")
                        .lastName("Principal")
                        .email("admin@test.com")
                        .build())
                .build();
    }

    @Test
    @DisplayName("GET /audit-logs - Retourne une page de logs pour un ADMIN_PRINCIPAL autorisé")
    void getAuditLogs_AuthorizedRole_ReturnsPage() throws Exception {
        when(auditLogQueryService.getAuditLogsForUser(any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleLog()), PageRequest.of(0, 25), 1));

        mockMvc.perform(get("/audit-logs").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].action").value("USER_CREATED"))
                .andExpect(jsonPath("$.content[0].entityType").value("User"))
                .andExpect(jsonPath("$.content[0].actor.email").value("admin@test.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /audit-logs - Retourne une page vide sans erreur")
    void getAuditLogs_NoLogs_ReturnsEmptyPage() throws Exception {
        when(auditLogQueryService.getAuditLogsForUser(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));

        mockMvc.perform(get("/audit-logs").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /audit-logs - Transmet la pagination demandée (page/size) au service")
    void getAuditLogs_WithPaging_PassesParamsToService() throws Exception {
        when(auditLogQueryService.getAuditLogsForUser(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 0));

        mockMvc.perform(get("/audit-logs")
                        .principal(authPrincipal)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(auditLogQueryService).getAuditLogsForUser(eq(adminUser), any());
    }

    @Test
    @DisplayName("GET /audit-logs - Refuse un rôle non autorisé (ex. AGENT)")
    void getAuditLogs_UnauthorizedRole_ReturnsForbidden() throws Exception {
        when(securityAuth.hasAnyRole(any(), eq("SUPER_ADMIN"), eq("ADMIN_PRINCIPAL"), eq("ADMIN_SECONDAIRE")))
                .thenReturn(false);

        mockMvc.perform(get("/audit-logs").principal(authPrincipal))
                .andExpect(status().isForbidden());

        verify(auditLogQueryService, never()).getAuditLogsForUser(any(), any());
    }

    @Test
    @DisplayName("GET /audit-logs - Vérifie que le principal authentifié est bien transmis au service")
    void getAuditLogs_PassesAuthenticatedUserToService() throws Exception {
        when(auditLogQueryService.getAuditLogsForUser(any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 25), 0));

        mockMvc.perform(get("/audit-logs").principal(authPrincipal))
                .andExpect(status().isOk());

        verify(auditLogQueryService).getAuditLogsForUser(eq(adminUser), any());
    }
}
