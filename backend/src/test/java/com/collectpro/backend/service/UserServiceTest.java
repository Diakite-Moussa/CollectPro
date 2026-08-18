package com.collectpro.backend.service;

import com.collectpro.backend.dto.ChangePasswordRequest;
import com.collectpro.backend.dto.CreateUserRequest;
import com.collectpro.backend.dto.UpdateProfileRequest;
import com.collectpro.backend.dto.UpdateUserStatusRequest;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.InvalidCredentialsException;
import com.collectpro.backend.repository.RoleRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.security.TokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TokenUtil tokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserInvitationService userInvitationService;

    @Mock
    private SupervisorAgentRepository supervisorAgentRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private UserService userService;

    private Organization org;
    private User adminUser;
    private Role agentRole;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(1L).name("Test Org").build();
        Role adminRole = Role.builder().id(1L).name(RoleType.ADMIN_PRINCIPAL).build();
        agentRole = Role.builder().id(2L).name(RoleType.AGENT).build();

        adminUser = User.builder()
                .id(10L)
                .firstName("Admin")
                .lastName("Test")
                .email("admin@test.com")
                .organization(org)
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .build();

        lenient().when(permissionService.hasPermission(any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("createUser - Succès avec émission d'invitation et log d'audit")
    void createUser_Success() {
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName("Jean");
        request.setLastName("Dupont");
        request.setEmail("jean.dupont@test.com");
        request.setPhone("+221770000000");
        request.setRoleType(RoleType.AGENT);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByEmail("jean.dupont@test.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.AGENT)).thenReturn(Optional.of(agentRole));
        when(tokenUtil.generateRawToken()).thenReturn("raw-token");
        when(passwordEncoder.encode("raw-token")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        UserResponse response = userService.createUser(adminUser, request);

        assertNotNull(response);
        assertEquals("jean.dupont@test.com", response.getEmail());
        verify(userInvitationService).sendInvitation(any(User.class));
        verify(auditLogService).log(
                eq(adminUser),
                eq(org),
                eq(AuditAction.USER_CREATED),
                eq("User"),
                eq(20L),
                anyString()
        );
    }

    @Test
    @DisplayName("createUser - Échec si l'email existe déjà")
    void createUser_DuplicateEmail_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("admin@test.com");
        request.setRoleType(RoleType.AGENT);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> userService.createUser(adminUser, request));
        verify(userRepository, never()).save(any());
        verify(auditLogService, never()).log(any(), any(), any(), any(), any(), any());
    }

    // -----------------------------------------------------------------------
    // RF-PROFILE-01 : getCurrentUser
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getCurrentUser - Retourne un UserResponse depuis le principal sans requête BDD")
    void getCurrentUser_ReturnsMappedResponse() {
        UserResponse response = userService.getCurrentUser(adminUser);

        assertNotNull(response);
        assertEquals(adminUser.getEmail(), response.getEmail());
        assertEquals(adminUser.getFirstName(), response.getFirstName());
        // Aucune requête BDD ne doit avoir été effectuée
        verify(userRepository, never()).findById(any());
    }

    // -----------------------------------------------------------------------
    // RF-PROFILE-02 : updateProfile
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateProfile - Succès : met à jour firstName, lastName et phone")
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Nouveau");
        request.setLastName("Nom");
        request.setPhone("+221771234567");

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateProfile(adminUser, request);

        assertNotNull(response);
        assertEquals("Nouveau", response.getFirstName());
        assertEquals("Nom", response.getLastName());
        assertEquals("+221771234567", response.getPhone());
        verify(userRepository).save(any(User.class));
    }

    // -----------------------------------------------------------------------
    // RF-PROFILE-03 : changePassword
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("changePassword - Succès avec bon mot de passe actuel")
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123");
        request.setNewPassword("NewPass456");

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("OldPass123", adminUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("encoded-new-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> userService.changePassword(adminUser, request));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword - Échec si le mot de passe actuel est incorrect")
    void changePassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPass");
        request.setNewPassword("NewPass456");

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("WrongPass", adminUser.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.changePassword(adminUser, request));
        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // RF-USER-11 : updateUserStatus
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateUserStatus - Désactivation réussie d'un agent par un admin")
    void updateUserStatus_DisableAgent_Success() {
        Role agentRoleLocal = Role.builder().id(3L).name(RoleType.AGENT).build();
        User agent = User.builder()
                .id(20L)
                .email("agent@test.com")
                .firstName("Agent")
                .lastName("Test")
                .role(agentRoleLocal)
                .organization(org)
                .status(UserStatus.ACTIVE)
                .build();

        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.DISABLED);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(20L)).thenReturn(Optional.of(agent));
        when(permissionService.hasPermission(adminUser, "DISABLE_USER")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserStatus(adminUser, 20L, request);

        assertNotNull(response);
        assertEquals("DISABLED", response.getStatus());
        verify(userRepository).save(any(User.class));
        verify(auditLogService).log(any(), any(), eq(AuditAction.USER_DISABLED), any(), any(), anyString());
    }

    @Test
    @DisplayName("updateUserStatus - Réactivation réussie d'un compte DISABLED")
    void updateUserStatus_ReactivateUser_Success() {
        Role agentRoleLocal = Role.builder().id(3L).name(RoleType.AGENT).build();
        User disabledAgent = User.builder()
                .id(21L)
                .email("disabled@test.com")
                .firstName("Disabled")
                .lastName("Agent")
                .role(agentRoleLocal)
                .organization(org)
                .status(UserStatus.DISABLED)
                .build();

        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(21L)).thenReturn(Optional.of(disabledAgent));
        when(permissionService.hasPermission(adminUser, "DISABLE_USER")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUserStatus(adminUser, 21L, request);

        assertNotNull(response);
        assertEquals("ACTIVE", response.getStatus());
        verify(auditLogService).log(any(), any(), eq(AuditAction.USER_REACTIVATED), any(), any(), anyString());
    }

    @Test
    @DisplayName("updateUserStatus - Échec si l'ADMIN_PRINCIPAL essaie de se désactiver lui-même")
    void updateUserStatus_AdminPrincipalSelfDisable_ThrowsException() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.DISABLED);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(permissionService.hasPermission(adminUser, "DISABLE_USER")).thenReturn(true);

        // adminUser.id == 10L == targetId
        assertThrows(BusinessRuleException.class,
                () -> userService.updateUserStatus(adminUser, 10L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserStatus - Échec si la cible est dans une autre organisation")
    void updateUserStatus_DifferentOrg_ThrowsException() {
        Organization otherOrg = Organization.builder().id(99L).name("Autre Org").build();
        Role agentRoleLocal = Role.builder().id(3L).name(RoleType.AGENT).build();
        User foreignAgent = User.builder()
                .id(30L)
                .email("foreign@test.com")
                .role(agentRoleLocal)
                .organization(otherOrg)
                .status(UserStatus.ACTIVE)
                .build();

        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        request.setStatus(UserStatus.DISABLED);

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(30L)).thenReturn(Optional.of(foreignAgent));
        when(permissionService.hasPermission(adminUser, "DISABLE_USER")).thenReturn(true);

        assertThrows(ForbiddenOperationException.class,
                () -> userService.updateUserStatus(adminUser, 30L, request));
        verify(userRepository, never()).save(any());
    }
}
