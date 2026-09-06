package com.collectpro.backend.security;

import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

/**
 * Vérifie que JwtAuthenticationFilter n'authentifie plus une requête sur la
 * seule base d'un JWT syntaxiquement valide : le statut du compte ET celui de
 * son organisation (s'il en a une) doivent aussi être actifs à l'instant de
 * la requête, pas seulement au moment du login. Sans ce filtre, un compte ou
 * une organisation désactivés entre-temps continuaient d'accéder à l'API
 * jusqu'à expiration du token.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final String TOKEN = "valid.jwt.token";
    private static final String EMAIL = "user@test.com";

    private Role agentRole;
    private Organization activeOrg;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        agentRole = Role.builder().id(1L).name(RoleType.AGENT).build();
        activeOrg = Organization.builder().id(1L).name("Org Test").status(OrganizationStatus.ACTIVE).build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + TOKEN);
        when(jwtService.extractEmail(TOKEN)).thenReturn(EMAIL);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authentifie normalement un utilisateur ACTIVE dont l'organisation est ACTIVE")
    void doFilter_ActiveUserActiveOrg_Authenticates() throws Exception {
        User user = User.builder()
                .id(10L).email(EMAIL).status(UserStatus.ACTIVE)
                .role(agentRole).organization(activeOrg)
                .build();
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(user));
        when(jwtService.isTokenValid(TOKEN, EMAIL)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("N'authentifie PAS un utilisateur désactivé (User.status = DISABLED) même avec un JWT encore valide")
    void doFilter_DisabledUser_DoesNotAuthenticate() throws Exception {
        User user = User.builder()
                .id(10L).email(EMAIL).status(UserStatus.DISABLED)
                .role(agentRole).organization(activeOrg)
                .build();
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(user));
        when(jwtService.isTokenValid(TOKEN, EMAIL)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // La requête continue quand même la chaîne : c'est anyRequest().authenticated()
        // en aval (SecurityFilterChain) qui rejettera en 401 faute de contexte peuplé.
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("N'authentifie PAS un utilisateur ACTIVE dont l'organisation est INACTIVE")
    void doFilter_ActiveUserInactiveOrg_DoesNotAuthenticate() throws Exception {
        Organization inactiveOrg = Organization.builder()
                .id(2L).name("Org Désactivée").status(OrganizationStatus.INACTIVE).build();
        User user = User.builder()
                .id(11L).email(EMAIL).status(UserStatus.ACTIVE)
                .role(agentRole).organization(inactiveOrg)
                .build();
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(user));
        when(jwtService.isTokenValid(TOKEN, EMAIL)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Non-régression : le Super Admin (sans organisation) est authentifié normalement")
    void doFilter_SuperAdminWithoutOrganization_Authenticates() throws Exception {
        Role superAdminRole = Role.builder().id(2L).name(RoleType.SUPER_ADMIN).build();
        User superAdmin = User.builder()
                .id(1L).email(EMAIL).status(UserStatus.ACTIVE)
                .role(superAdminRole).organization(null)
                .build();
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(superAdmin));
        when(jwtService.isTokenValid(TOKEN, EMAIL)).thenReturn(true);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("N'authentifie pas si le JWT lui-même est invalide/expiré (comportement existant préservé)")
    void doFilter_InvalidToken_DoesNotAuthenticate() throws Exception {
        User user = User.builder()
                .id(10L).email(EMAIL).status(UserStatus.ACTIVE)
                .role(agentRole).organization(activeOrg)
                .build();
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(new CustomUserDetails(user));
        when(jwtService.isTokenValid(TOKEN, EMAIL)).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Laisse passer sans tenter d'authentifier si aucun header Authorization n'est présent")
    void doFilter_NoAuthorizationHeader_SkipsAuthentication() throws Exception {
        reset(request);
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }
}
