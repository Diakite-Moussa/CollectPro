package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class ReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private UsernamePasswordAuthenticationToken agentAuth;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder().id(1L).name("Org").build();
        Role agentRole = Role.builder().id(4L).name(RoleType.AGENT).build();
        User agent = User.builder().id(15L).organization(org).role(agentRole).build();

        CustomUserDetails details = new CustomUserDetails(agent);
        agentAuth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(agentAuth);

        when(securityAuth.hasPermission(any(), eq("GENERATE_REPORT"))).thenReturn(false);
        when(securityAuth.hasPermission(any(), eq("VIEW_REPORT"))).thenReturn(false);
    }

    @Test
    @DisplayName("POST /reports/organizations/1 - Refusé sans permission GENERATE_REPORT")
    void generateOrgReport_WithoutPermission_Forbidden() throws Exception {
        mockMvc.perform(post("/reports/organizations/1").principal(agentAuth))
                .andExpect(status().isForbidden());

        verify(reportService, never()).generateOrganizationReport(any(), any(), any());
    }

    @Test
    @DisplayName("GET /reports/organizations/1 - Refusé sans permission VIEW_REPORT")
    void getOrgReports_WithoutPermission_Forbidden() throws Exception {
        mockMvc.perform(get("/reports/organizations/1").principal(agentAuth))
                .andExpect(status().isForbidden());

        verify(reportService, never()).getOrganizationReports(any(), any(), any());
    }
}
