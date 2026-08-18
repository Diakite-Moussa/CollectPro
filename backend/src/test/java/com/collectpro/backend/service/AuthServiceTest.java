package com.collectpro.backend.service;

import com.collectpro.backend.dto.AuthResponse;
import com.collectpro.backend.dto.LoginRequest;
import com.collectpro.backend.dto.RefreshTokenRequest;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.RefreshToken;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.InvalidCredentialsException;
import com.collectpro.backend.repository.ActivationTokenRepository;
import com.collectpro.backend.repository.RefreshTokenRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private User inactiveUser;
    private Role agentRole;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 30L);

        testOrg = Organization.builder().id(1L).name("Test Org").build();
        agentRole = Role.builder().id(1L).name(RoleType.AGENT).build();

        activeUser = User.builder()
                .id(10L)
                .email("agent@test.com")
                .password("encoded_pass")
                .firstName("Jean")
                .lastName("Dupont")
                .status(UserStatus.ACTIVE)
                .role(agentRole)
                .organization(testOrg)
                .build();

        inactiveUser = User.builder()
                .id(11L)
                .email("inactive@test.com")
                .password("encoded_pass")
                .firstName("Pierre")
                .lastName("Durand")
                .status(UserStatus.INVITED)
                .role(agentRole)
                .organization(testOrg)
                .build();
    }

    @Test
    @DisplayName("login() - Succès : retourne AuthResponse avec jetons")
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("agent@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);
        when(jwtService.generateToken("agent@test.com")).thenReturn("mocked_jwt_token");
        when(tokenUtil.generateRawToken()).thenReturn("mocked_raw_refresh");
        when(tokenUtil.hashToken("mocked_raw_refresh")).thenReturn("mocked_hash_refresh");

        AuthResponse response = authService.login(request, null);

        assertNotNull(response);
        assertEquals("mocked_jwt_token", response.getAccessToken());
        assertEquals("mocked_raw_refresh", response.getRefreshToken());
        assertEquals("agent@test.com", response.getUser().getEmail());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login() - Échec : utilisateur non trouvé")
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, null));
    }

    @Test
    @DisplayName("login() - Échec : compte non actif")
    void login_UserInactive_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("inactive@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("inactive@test.com")).thenReturn(Optional.of(inactiveUser));

        assertThrows(BusinessRuleException.class, () -> authService.login(request, null));
    }

    @Test
    @DisplayName("login() - Échec : mot de passe incorrect")
    void login_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("agent@test.com");
        request.setPassword("wrong_pass");

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong_pass", "encoded_pass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request, null));
    }

    @Test
    @DisplayName("login() - Échec : un AGENT ne peut pas se connecter depuis le web")
    void login_AgentOnWeb_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("agent@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> authService.login(request, "WEB"));
    }

    @Test
    @DisplayName("login() - Échec : un ADMIN_PRINCIPAL ne peut pas se connecter depuis le mobile")
    void login_AdminOnMobile_ThrowsException() {
        Role adminRole = Role.builder().id(2L).name(RoleType.ADMIN_PRINCIPAL).build();
        User adminUser = User.builder()
                .id(20L)
                .email("admin@test.com")
                .password("encoded_pass")
                .firstName("Awa")
                .lastName("Traore")
                .status(UserStatus.ACTIVE)
                .role(adminRole)
                .organization(testOrg)
                .build();

        LoginRequest request = new LoginRequest();
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("password123", "encoded_pass")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> authService.login(request, "MOBILE"));
    }

    @Test
    @DisplayName("refresh() - Succès : rafraîchissement des jetons avec rotation")
    void refresh_Success() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("raw_refresh_123");

        RefreshToken storedToken = RefreshToken.builder()
                .id(100L)
                .user(activeUser)
                .tokenHash("hash_123")
                .expiresAt(LocalDateTime.now().plusDays(5))
                .build();

        when(tokenUtil.hashToken("raw_refresh_123")).thenReturn("hash_123");
        when(refreshTokenRepository.findByTokenHash("hash_123")).thenReturn(Optional.of(storedToken));
        when(jwtService.generateToken("agent@test.com")).thenReturn("new_jwt_token");
        when(tokenUtil.generateRawToken()).thenReturn("new_raw_refresh");
        when(tokenUtil.hashToken("new_raw_refresh")).thenReturn("new_hash_refresh");

        AuthResponse response = authService.refresh(request, null);

        assertNotNull(response);
        assertEquals("new_jwt_token", response.getAccessToken());
        assertEquals("new_raw_refresh", response.getRefreshToken());
        assertNotNull(storedToken.getRevokedAt());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
