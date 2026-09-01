package com.collectpro.backend.service;

import com.collectpro.backend.dto.CollecteExportFilter;
import com.collectpro.backend.entity.*;
import com.collectpro.backend.enums.CollecteStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollecteExportServiceTest {

    @Mock
    private CollecteRepository collecteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupervisorAgentRepository supervisorAgentRepository;

    @InjectMocks
    private CollecteExportService exportService;

    private Organization org;
    private User admin;
    private User otherAdmin;
    private Collecte c1;

    @BeforeEach
    void setUp() {
        org = Organization.builder().id(1L).name("ONG Santé").build();
        Organization otherOrg = Organization.builder().id(2L).name("Autre ONG").build();

        Role adminRole = Role.builder().name(RoleType.ADMIN_PRINCIPAL).build();
        User agent = User.builder().id(10L).firstName("Moussa").lastName("Diop").email("moussa@test.com").organization(org).build();

        admin = User.builder().id(1L).firstName("Admin").lastName("Principal").organization(org).role(adminRole).build();
        otherAdmin = User.builder().id(2L).firstName("Other").lastName("Admin").organization(otherOrg).role(adminRole).build();

        Form form = Form.builder().id(100L).name("Dépistage").organization(org).build();
        FormVersion version = FormVersion.builder().id(1000L).form(form).versionNumber(1).build();
        Mission mission = Mission.builder().id(50L).name("Mission Palu").organization(org).build();

        c1 = Collecte.builder()
                .id(501L)
                .agent(agent)
                .formVersion(version)
                .mission(mission)
                .dataJson("{\"fievre\":\"oui\"}")
                .latitude(14.7167)
                .longitude(-17.4677)
                .status(CollecteStatus.VALIDATED)
                .createdAt(LocalDateTime.now().minusDays(1))
                .validatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("exportCsv - Génère un CSV avec BOM UTF-8 et en-têtes corrects")
    void exportCsv_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(collecteRepository.findByAgent_OrganizationId(eq(1L))).thenReturn(List.of(c1));

        CollecteExportFilter filter = CollecteExportFilter.builder().build();
        byte[] csvBytes = exportService.exportCsv(admin, 1L, filter);

        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);

        String csvString = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csvString.startsWith("\uFEFF"));
        assertTrue(csvString.contains("ID;Formulaire;Version;Agent"));
        assertTrue(csvString.contains("Dépistage"));
        assertTrue(csvString.contains("Moussa Diop"));
        assertTrue(csvString.contains("Mission Palu"));
    }

    @Test
    @DisplayName("exportExcel - Génère un classeur Excel .xlsx valide")
    void exportExcel_Success() throws IOException {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(collecteRepository.findByAgent_OrganizationId(eq(1L))).thenReturn(List.of(c1));

        CollecteExportFilter filter = CollecteExportFilter.builder().build();
        byte[] excelBytes = exportService.exportExcel(admin, 1L, filter);

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);
        // Signature ZIP / XLSX (PK..)
        assertEquals('P', (char) excelBytes[0]);
        assertEquals('K', (char) excelBytes[1]);
    }

    @Test
    @DisplayName("exportCsv - Refuse l'accès à une autre organisation")
    void exportCsv_ForbiddenForOtherOrg() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));

        assertThrows(AccessDeniedException.class, () ->
                exportService.exportCsv(otherAdmin, 1L, CollecteExportFilter.builder().build())
        );
    }

    @Test
    @DisplayName("exportCsv - Neutralise les injections de formules OWASP (=, +, -, @)")
    void exportCsv_NeutralizesFormulaInjection() {
        Collecte injectionCollecte = Collecte.builder()
                .id(999L)
                .agent(User.builder().id(10L).firstName("=CMD|").lastName("calc").email("@evil.com").organization(org).build())
                .formVersion(c1.getFormVersion())
                .mission(c1.getMission())
                .validationComment("+malicious_comment")
                .dataJson("=HYPERLINK(\"http://evil.com\")")
                .status(CollecteStatus.PENDING_VALIDATION)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(collecteRepository.findByAgent_OrganizationId(eq(1L))).thenReturn(List.of(injectionCollecte));

        byte[] csvBytes = exportService.exportCsv(admin, 1L, CollecteExportFilter.builder().build());
        String csvString = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csvString.contains("'=CMD|"));
        assertTrue(csvString.contains("'@evil.com"));
        assertTrue(csvString.contains("'+malicious_comment"));
        assertTrue(csvString.contains("'=HYPERLINK"));
    }
}
