package com.collectpro.backend.controller;

import com.collectpro.backend.config.MethodSecurityConfig;
import com.collectpro.backend.dto.CreateOrganizationRequest;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.security.CustomUserDetailsService;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.SecurityAuthorizationService;
import com.collectpro.backend.service.OrganizationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(MethodSecurityConfig.class)
class OrganizationControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "securityAuth")
    private SecurityAuthorizationService securityAuth;

    private UsernamePasswordAuthenticationToken adminPrincipalAuth;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder().id(1L).name("Org").build();
        Role adminRole = Role.builder().id(3L).name(RoleType.ADMIN_PRINCIPAL).build();
        User adminPrincipal = User.builder().id(2L).organization(org).role(adminRole).build();

        CustomUserDetails details = new CustomUserDetails(adminPrincipal);
        adminPrincipalAuth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(adminPrincipalAuth);

        when(securityAuth.isSuperAdmin(any())).thenReturn(false);
    }

    @Test
    @DisplayName("POST /organizations - Refuse un ADMIN_PRINCIPAL (seul SUPER_ADMIN peut créer une organisation)")
    void createOrganization_NotSuperAdmin_ReturnsForbidden() throws Exception {
        CreateOrganizationRequest request = new CreateOrganizationRequest();
        request.setName("Nouvelle Org");
        request.setAdminFirstName("Admin");
        request.setAdminLastName("Org");
        request.setAdminEmail("admin@org.com");

        mockMvc.perform(post("/organizations")
                        .principal(adminPrincipalAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(organizationService, never()).createOrganizationWithPrincipalAdmin(any(), any());
    }

    @Test
    @DisplayName("GET /organizations - Refuse un ADMIN_PRINCIPAL (liste globale réservée au SUPER_ADMIN)")
    void getOrganizations_NotSuperAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/organizations").principal(adminPrincipalAuth))
                .andExpect(status().isForbidden());

        verify(organizationService, never()).getAllOrganizations();
    }
}
