package com.collectpro.backend.service;

import com.collectpro.backend.dto.CollecteResponse;
import com.collectpro.backend.dto.RejectCollecteRequest;
import com.collectpro.backend.dto.ValidateCollecteRequest;
import com.collectpro.backend.entity.Collecte;
import com.collectpro.backend.entity.FormVersion;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.SupervisorAgent;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.CollecteAttachmentRepository;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.repository.ValidationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollecteServiceTest {

    @Mock
    private CollecteRepository collecteRepository;

    @Mock
    private SupervisorAgentRepository supervisorAgentRepository;

    @Mock
    private FormVersionService formVersionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CollecteAttachmentService attachmentService;

    @Mock
    private CollecteAttachmentRepository attachmentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ValidationRepository validationRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CollecteService collecteService;

    private User agent;
    private User supervisor;
    private User otherSupervisor;
    private Organization organization;
    private Collecte pendingCollecte;

    @BeforeEach
    void setUp() {
        organization = Organization.builder().id(1L).name("Org Test").build();

        Role agentRole = Role.builder().id(1L).name(RoleType.AGENT).build();
        Role supervisorRole = Role.builder().id(2L).name(RoleType.SUPERVISOR).build();

        agent = User.builder()
                .id(10L)
                .firstName("Agent")
                .lastName("Un")
                .organization(organization)
                .role(agentRole)
                .build();

        supervisor = User.builder()
                .id(20L)
                .firstName("Superviseur")
                .lastName("Chef")
                .organization(organization)
                .role(supervisorRole)
                .build();

        otherSupervisor = User.builder()
                .id(30L)
                .firstName("Superviseur")
                .lastName("Autre")
                .organization(organization)
                .role(supervisorRole)
                .build();

        FormVersion formVersion = FormVersion.builder().id(1L).versionNumber(1).build();

        pendingCollecte = Collecte.builder()
                .id(100L)
                .agent(agent)
                .formVersion(formVersion)
                .dataJson("{\"q1\":\"val1\"}")
                .status(CollecteStatus.PENDING_VALIDATION)
                .build();
    }

    @Test
    @DisplayName("validateCollecte() - Succès par le superviseur attitré")
    void validateCollecte_Success() {
        SupervisorAgent sa = SupervisorAgent.builder().agent(agent).supervisor(supervisor).build();

        when(collecteRepository.findById(100L)).thenReturn(Optional.of(pendingCollecte));
        when(supervisorAgentRepository.findByAgentId(10L)).thenReturn(Optional.of(sa));
        when(collecteRepository.save(any(Collecte.class))).thenAnswer(inv -> inv.getArgument(0));

        ValidateCollecteRequest request = new ValidateCollecteRequest();
        request.setComment("Très bonne collecte");

        CollecteResponse response = collecteService.validateCollecte(100L, supervisor, request);

        assertNotNull(response);
        assertEquals(CollecteStatus.VALIDATED, response.getStatus());
        assertEquals("Très bonne collecte", response.getValidationComment());
        verify(collecteRepository, times(1)).save(any(Collecte.class));
    }

    @Test
    @DisplayName("rejectCollecte() - Succès avec enregistrement du motif")
    void rejectCollecte_Success() {
        SupervisorAgent sa = SupervisorAgent.builder().agent(agent).supervisor(supervisor).build();

        when(collecteRepository.findById(100L)).thenReturn(Optional.of(pendingCollecte));
        when(supervisorAgentRepository.findByAgentId(10L)).thenReturn(Optional.of(sa));
        when(collecteRepository.save(any(Collecte.class))).thenAnswer(inv -> inv.getArgument(0));

        RejectCollecteRequest request = new RejectCollecteRequest();
        request.setComment("Photo floue, à refaire");

        CollecteResponse response = collecteService.rejectCollecte(100L, supervisor, request);

        assertNotNull(response);
        assertEquals(CollecteStatus.REJECTED, response.getStatus());
        assertEquals("Photo floue, à refaire", response.getValidationComment());
    }

    @Test
    @DisplayName("validateCollecte() - Échec : tentative par un superviseur non attitré")
    void validateCollecte_NotSupervisor_ThrowsForbidden() {
        SupervisorAgent sa = SupervisorAgent.builder().agent(agent).supervisor(supervisor).build();

        when(collecteRepository.findById(100L)).thenReturn(Optional.of(pendingCollecte));
        when(supervisorAgentRepository.findByAgentId(10L)).thenReturn(Optional.of(sa));

        ValidateCollecteRequest request = new ValidateCollecteRequest();

        assertThrows(ForbiddenOperationException.class, () ->
                collecteService.validateCollecte(100L, otherSupervisor, request)
        );
    }

    @Test
    @DisplayName("validateCollecte() - Échec : collecte déjà traitée")
    void validateCollecte_AlreadyProcessed_ThrowsBusinessRuleException() {
        pendingCollecte.setStatus(CollecteStatus.VALIDATED);
        when(collecteRepository.findById(100L)).thenReturn(Optional.of(pendingCollecte));

        ValidateCollecteRequest request = new ValidateCollecteRequest();

        assertThrows(BusinessRuleException.class, () ->
                collecteService.validateCollecte(100L, supervisor, request)
        );
    }

    @Test
    @DisplayName("validateCollecte() - Échec : collecte inexistante")
    void validateCollecte_NotFound_ThrowsException() {
        when(collecteRepository.findById(999L)).thenReturn(Optional.empty());

        ValidateCollecteRequest request = new ValidateCollecteRequest();

        assertThrows(ResourceNotFoundException.class, () ->
                collecteService.validateCollecte(999L, supervisor, request)
        );
    }
}
