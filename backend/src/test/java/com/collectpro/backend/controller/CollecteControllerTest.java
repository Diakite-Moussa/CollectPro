package com.collectpro.backend.controller;

import com.collectpro.backend.dto.CollecteResponse;
import com.collectpro.backend.dto.RejectCollecteRequest;
import com.collectpro.backend.dto.ValidateCollecteRequest;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.CollecteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.collectpro.backend.config.MethodSecurityConfig;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CollecteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class CollecteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CollecteService collecteService;

    @MockitoBean
    private com.collectpro.backend.service.CollecteExportService exportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private User supervisorUser;
    private CustomUserDetails customUserDetails;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder().id(1L).name("Org").build();
        Role supervisorRole = Role.builder().id(2L).name(RoleType.SUPERVISOR).build();

        supervisorUser = User.builder()
                .id(20L)
                .email("sup@test.com")
                .firstName("Super")
                .lastName("Visor")
                .organization(org)
                .role(supervisorRole)
                .build();

        customUserDetails = new CustomUserDetails(supervisorUser);
        authPrincipal = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);

        // Autoriser systématiquement les vérifications @PreAuthorize("@securityAuth...") pour les tests controller
        lenient().when(securityAuth.hasRole(any(), any())).thenReturn(true);
        lenient().when(securityAuth.hasAnyRole(any(), any(String[].class))).thenReturn(true);
        lenient().when(securityAuth.hasPermission(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /collectes/team/pending-validation - Succès")
    void getPendingValidation_ReturnsList() throws Exception {
        CollecteResponse resp = CollecteResponse.builder()
                .id(100L)
                .status(CollecteStatus.PENDING_VALIDATION)
                .dataJson("{\"key\":\"val\"}")
                .build();

        when(collecteService.getPendingValidationForSupervisor(any())).thenReturn(List.of(resp));

        mockMvc.perform(get("/collectes/team/pending-validation").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].status").value("PENDING_VALIDATION"));
    }

    @Test
    @DisplayName("POST /collectes/{id}/validate - Succès")
    void validateCollecte_ReturnsValidatedResponse() throws Exception {
        ValidateCollecteRequest request = new ValidateCollecteRequest();
        request.setComment("OK");

        CollecteResponse resp = CollecteResponse.builder()
                .id(100L)
                .status(CollecteStatus.VALIDATED)
                .validationComment("OK")
                .build();

        when(collecteService.validateCollecte(eq(100L), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/collectes/100/validate")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @DisplayName("POST /collectes/{id}/reject - Succès avec motif de rejet")
    void rejectCollecte_ReturnsRejectedResponse() throws Exception {
        RejectCollecteRequest request = new RejectCollecteRequest();
        request.setComment("Incomplet");

        CollecteResponse resp = CollecteResponse.builder()
                .id(100L)
                .status(CollecteStatus.REJECTED)
                .validationComment("Incomplet")
                .build();

        when(collecteService.rejectCollecte(eq(100L), any(), any())).thenReturn(resp);

        mockMvc.perform(post("/collectes/100/reject")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.validationComment").value("Incomplet"));
    }

    @Test
    @DisplayName("POST /collectes/{id}/validate - Refuse un utilisateur sans le rôle SUPERVISOR")
    void validateCollecte_WithoutRole_ReturnsForbidden() throws Exception {
        when(securityAuth.hasRole(any(), eq("SUPERVISOR"))).thenReturn(false);

        ValidateCollecteRequest request = new ValidateCollecteRequest();
        request.setComment("OK");

        mockMvc.perform(post("/collectes/100/validate")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(collecteService, never()).validateCollecte(any(), any(), any());
    }

    @Test
    @DisplayName("POST /collectes/{id}/reject - Refuse un utilisateur sans le rôle SUPERVISOR")
    void rejectCollecte_WithoutRole_ReturnsForbidden() throws Exception {
        when(securityAuth.hasRole(any(), eq("SUPERVISOR"))).thenReturn(false);

        RejectCollecteRequest request = new RejectCollecteRequest();
        request.setComment("Incomplet");

        mockMvc.perform(post("/collectes/100/reject")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(collecteService, never()).rejectCollecte(any(), any(), any());
    }
}
