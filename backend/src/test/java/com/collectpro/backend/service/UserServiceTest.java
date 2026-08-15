package com.collectpro.backend.service;

import com.collectpro.backend.dto.CreateUserRequest;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
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
}
