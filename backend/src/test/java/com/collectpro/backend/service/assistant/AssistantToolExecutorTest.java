package com.collectpro.backend.service.assistant;

import com.collectpro.backend.dto.MissionProgressResponse;
import com.collectpro.backend.dto.MissionResponse;
import com.collectpro.backend.dto.StatisticsResponse;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.MissionStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.service.CollecteService;
import com.collectpro.backend.service.MissionService;
import com.collectpro.backend.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.collectpro.backend.dto.AssignSupervisorRequest;
import com.collectpro.backend.dto.CreateUserRequest;
import com.collectpro.backend.dto.UpdateUserStatusRequest;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.service.PermissionService;
import com.collectpro.backend.service.ReportService;
import com.collectpro.backend.service.UserService;
import com.collectpro.backend.service.FormService;
import com.collectpro.backend.service.AuditLogQueryService;
import com.collectpro.backend.service.SyncLogService;
import com.collectpro.backend.dto.FormResponse;
import com.collectpro.backend.dto.AuditLogResponse;
import com.collectpro.backend.dto.SyncLogResponse;
import com.collectpro.backend.dto.CreateMissionRequest;
import com.collectpro.backend.dto.ReportResponse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantToolExecutorTest {

    @Mock private MissionService missionService;
    @Mock private CollecteService collecteService;
    @Mock private StatisticsService statisticsService;
    @Mock private ReportService reportService;
    @Mock private PermissionService permissionService;
    @Mock private AssistantConfirmationService confirmationService;
    @Mock private UserService userService;
    @Mock private FormService formService;
    @Mock private AuditLogQueryService auditLogQueryService;
    @Mock private SyncLogService syncLogService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AssistantToolExecutor toolExecutor;

    private User agent;
    private User supervisor;
    private User admin;
    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = Organization.builder()
                .id(10L)
                .name("ONG Solidarité")
                .build();

        agent = User.builder()
                .id(1L)
                .email("agent@collectpro.com")
                .firstName("Amadou")
                .lastName("Diallo")
                .role(Role.builder().name(RoleType.AGENT).build())
                .organization(organization)
                .build();

        supervisor = User.builder()
                .id(2L)
                .email("supervisor@collectpro.com")
                .firstName("Awa")
                .lastName("Kone")
                .role(Role.builder().name(RoleType.SUPERVISOR).build())
                .organization(organization)
                .build();

        admin = User.builder()
                .id(3L)
                .email("admin@collectpro.com")
                .firstName("Moussa")
                .lastName("Diakite")
                .role(Role.builder().name(RoleType.ADMIN_PRINCIPAL).build())
                .organization(organization)
                .build();
    }

    @Test
    @DisplayName("lister_missions pour un agent appelle getMissionsForAgent")
    void testListerMissionsAgent() {
        MissionResponse m = MissionResponse.builder()
                .id(100L)
                .name("Recensement Dakar")
                .status(MissionStatus.ACTIVE)
                .expectedCollectesCount(500)
                .agents(Collections.emptyList())
                .build();

        when(missionService.getMissionsForAgent(agent)).thenReturn(List.of(m));

        String json = toolExecutor.execute("lister_missions", Collections.emptyMap(), agent);

        assertTrue(json.contains("Recensement Dakar"));
        assertTrue(json.contains("ACTIVE"));
        verify(missionService).getMissionsForAgent(agent);
    }

    @Test
    @DisplayName("lister_collectes_en_attente est refusé pour un agent (RBAC)")
    void testListerCollectesEnAttenteRefusAgent() {
        String json = toolExecutor.execute("lister_collectes_en_attente", Collections.emptyMap(), agent);

        assertTrue(json.contains("Accès refusé"));
        assertTrue(json.contains("superviseurs d'équipe"));
    }

    @Test
    @DisplayName("lister_collectes_en_attente est refusé pour un Admin Principal (RBAC)")
    void testListerCollectesEnAttenteRefusAdmin() {
        User admin = User.builder()
                .id(3L)
                .email("admin@collectpro.com")
                .role(Role.builder().name(RoleType.ADMIN_PRINCIPAL).build())
                .organization(organization)
                .build();

        String json = toolExecutor.execute("lister_collectes_en_attente", Collections.emptyMap(), admin);

        assertTrue(json.contains("Accès refusé"));
        assertTrue(json.contains("superviseurs d'équipe"));
    }

    @Test
    @DisplayName("lister_collectes_en_attente est refusé pour un Super Admin (RBAC)")
    void testListerCollectesEnAttenteRefusSuperAdmin() {
        User superAdmin = User.builder()
                .id(99L)
                .email("superadmin@collectpro.com")
                .role(Role.builder().name(RoleType.SUPER_ADMIN).build())
                .build();

        String json = toolExecutor.execute("lister_collectes_en_attente", Collections.emptyMap(), superAdmin);

        assertTrue(json.contains("Accès refusé"));
        assertTrue(json.contains("superviseurs d'équipe"));
    }

    @Test
    @DisplayName("lister_collectes_en_attente fonctionne pour un superviseur")
    void testListerCollectesEnAttenteSupervisor() {
        when(collecteService.getPendingValidationForSupervisor(eq(supervisor), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        String json = toolExecutor.execute("lister_collectes_en_attente", Collections.emptyMap(), supervisor);

        assertTrue(json.contains("total_elements"));
        verify(collecteService).getPendingValidationForSupervisor(eq(supervisor), any(Pageable.class));
    }

    @Test
    @DisplayName("consulter_statistiques est refusé pour un agent")
    void testConsulterStatistiquesAgent() {
        String json = toolExecutor.execute("consulter_statistiques", Map.of("trend_days", 30), agent);

        assertTrue(json.contains("Accès refusé"));
        assertFalse(json.contains("total_collectes"));
    }

    @Test
    @DisplayName("consulter_statistiques fonctionne pour un superviseur")
    void testConsulterStatistiquesSupervisor() {
        StatisticsResponse stats = StatisticsResponse.builder()
                .scope("TEAM")
                .totalCollectes(42L)
                .pendingCollectes(5L)
                .validatedCollectes(35L)
                .rejectedCollectes(2L)
                .build();

        when(statisticsService.getStatistics(supervisor, 30)).thenReturn(stats);

        String json = toolExecutor.execute("consulter_statistiques", Map.of("trend_days", 30), supervisor);

        assertTrue(json.contains("42"));
        assertTrue(json.contains("TEAM"));
        verify(statisticsService).getStatistics(supervisor, 30);
    }

    @Test
    @DisplayName("consulter_avancement_mission avec nom de mission résout l'id et appelle getMissionProgress")
    void testConsulterAvancementMissionParNom() {
        MissionResponse m = MissionResponse.builder()
                .id(101L)
                .name("Enquête Santé")
                .build();

        MissionProgressResponse progress = MissionProgressResponse.builder()
                .missionId(101L)
                .missionName("Enquête Santé")
                .expectedCollectesCount(100)
                .receivedCollectesCount(60L)
                .progressPercent(60.0)
                .build();

        when(missionService.getMissionsForOrganization(organization)).thenReturn(List.of(m));
        when(missionService.getMissionProgress(101L, supervisor)).thenReturn(progress);

        String json = toolExecutor.execute("consulter_avancement_mission", Map.of("mission_name", "santé"), supervisor);

        assertTrue(json.contains("Enquête Santé"));
        assertTrue(json.contains("60"));
        verify(missionService).getMissionProgress(101L, supervisor);
    }

    @Test
    @DisplayName("creer_utilisateur prépare la confirmation avec succès pour admin")
    void testCreerUtilisateurPrepare() {
        when(permissionService.hasPermission(admin, "CREATE_USER")).thenReturn(true);

        Map<String, Object> args = Map.of(
                "first_name", "Fatoumata",
                "last_name", "Diallo",
                "email", "f.diallo@collectpro.com",
                "role_type", "AGENT"
        );

        String json = toolExecutor.execute("creer_utilisateur", args, admin);

        assertTrue(json.contains("pending_confirmation"));
        assertTrue(json.contains("Fatoumata Diallo"));
        verify(confirmationService).create(eq(admin.getId()), eq("creer_utilisateur"), anyMap(), anyString());
    }

    @Test
    @DisplayName("creer_utilisateur refusé si agent sans permission")
    void testCreerUtilisateurRefuseAgent() {
        when(permissionService.hasPermission(agent, "CREATE_USER")).thenReturn(false);

        Map<String, Object> args = Map.of(
                "first_name", "Fatoumata",
                "last_name", "Diallo",
                "email", "f.diallo@collectpro.com",
                "role_type", "AGENT"
        );

        String json = toolExecutor.execute("creer_utilisateur", args, agent);

        assertTrue(json.contains("Accès refusé"));
    }

    @Test
    @DisplayName("supprimer_utilisateur prépare la confirmation avec résolution de nom")
    void testSupprimerUtilisateurPrepare() {
        when(permissionService.hasPermission(admin, "DISABLE_USER")).thenReturn(true);
        UserResponse target = UserResponse.builder()
                .id(42L)
                .firstName("Jean")
                .lastName("Kone")
                .email("jean.kone@collectpro.com")
                .build();

        when(userService.getUsers(eq(admin), any())).thenReturn(new PageImpl<>(List.of(target)));

        String json = toolExecutor.execute("supprimer_utilisateur", Map.of("name", "Jean"), admin);

        assertTrue(json.contains("pending_confirmation"));
        assertTrue(json.contains("Jean Kone"));
        verify(confirmationService).create(eq(admin.getId()), eq("supprimer_utilisateur"), anyMap(), anyString());
    }

    @Test
    @DisplayName("affecter_agent_superviseur prépare la confirmation avec succès")
    void testAffecterAgentSuperviseurPrepare() {
        UserResponse a = UserResponse.builder().id(1L).firstName("Amadou").lastName("Diallo").role("AGENT").build();
        UserResponse s = UserResponse.builder().id(2L).firstName("Awa").lastName("Kone").role("SUPERVISOR").build();

        when(userService.getUsers(eq(admin), any())).thenReturn(new PageImpl<>(List.of(a, s)));

        Map<String, Object> args = Map.of(
                "agent_name_or_email", "Amadou",
                "supervisor_name_or_email", "Awa"
        );

        String json = toolExecutor.execute("affecter_agent_superviseur", args, admin);

        assertTrue(json.contains("pending_confirmation"));
        assertTrue(json.contains("Affecter l'agent"));
        verify(confirmationService).create(eq(admin.getId()), eq("affecter_agent_superviseur"), anyMap(), anyString());
    }

    @Test
    @DisplayName("executeConfirmed exécute creer_utilisateur avec UserService")
    void testExecuteConfirmedCreerUtilisateur() {
        AssistantConfirmationService.PendingAction action = new AssistantConfirmationService.PendingAction(
                "uuid-123",
                admin.getId(),
                "creer_utilisateur",
                Map.of("first_name", "Fatou", "last_name", "Ndiaye", "email", "fatou@test.com", "role_type", "AGENT"),
                "Créer l'utilisateur Fatou Ndiaye",
                null
        );

        UserResponse created = UserResponse.builder()
                .id(88L)
                .email("fatou@test.com")
                .role("AGENT")
                .build();

        when(userService.createUser(eq(admin), any(CreateUserRequest.class))).thenReturn(created);

        String json = toolExecutor.executeConfirmed(action, admin);

        assertTrue(json.contains("exécuté"));
        assertTrue(json.contains("88"));
        verify(userService).createUser(eq(admin), any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("lister_utilisateurs : refusé pour un agent, autorisé pour un superviseur")
    void testListerUtilisateursRoleCheck() {
        String agentJson = toolExecutor.execute("lister_utilisateurs", Map.of(), agent);
        assertTrue(agentJson.contains("Accès refusé"));

        UserResponse u = UserResponse.builder()
                .id(1L)
                .firstName("Amadou")
                .lastName("Diallo")
                .email("amadou@collectpro.com")
                .role("AGENT")
                .status("ACTIVE")
                .build();
        when(userService.getUsers(eq(supervisor), any())).thenReturn(new PageImpl<>(List.of(u)));

        String supervisorJson = toolExecutor.execute("lister_utilisateurs", Map.of("role", "AGENT"), supervisor);
        assertTrue(supervisorJson.contains("total_utilisateurs_trouves"));
        assertTrue(supervisorJson.contains("Amadou Diallo"));
    }

    @Test
    @DisplayName("lister_formulaires renvoie la liste des formulaires de l'organisation")
    void testListerFormulaires() {
        FormResponse f = FormResponse.builder()
                .id(5L)
                .name("Enquête Ménages")
                .description("Formulaire pilote")
                .status(com.collectpro.backend.enums.FormStatus.PUBLISHED)
                .build();
        when(formService.getFormsForOrganization(eq(organization))).thenReturn(List.of(f));

        String json = toolExecutor.execute("lister_formulaires", Map.of(), supervisor);
        assertTrue(json.contains("Enquête Ménages"));
        assertTrue(json.contains("PUBLISHED"));
    }

    @Test
    @DisplayName("consulter_journal_audit : réservé aux administrateurs")
    void testConsulterJournalAudit() {
        String supervisorJson = toolExecutor.execute("consulter_journal_audit", Map.of(), supervisor);
        assertTrue(supervisorJson.contains("Accès refusé"));

        AuditLogResponse logItem = AuditLogResponse.builder()
                .id(1L)
                .action(com.collectpro.backend.enums.AuditAction.USER_CREATED)
                .entityType("User")
                .details("Création de compte")
                .actor(AuditLogResponse.ActorSummary.builder().firstName("Admin").lastName("Principal").build())
                .build();
        when(auditLogQueryService.getAuditLogsForUser(eq(admin), any())).thenReturn(new PageImpl<>(List.of(logItem)));

        String adminJson = toolExecutor.execute("consulter_journal_audit", Map.of("limit", 5), admin);
        assertTrue(adminJson.contains("USER_CREATED"));
        assertTrue(adminJson.contains("Admin Principal"));
    }

    @Test
    @DisplayName("consulter_etat_synchronisations : analyse les logs de synchronisation")
    void testConsulterEtatSynchronisations() {
        SyncLogResponse syncItem = SyncLogResponse.builder()
                .id(10L)
                .result(com.collectpro.backend.enums.SyncResult.SUCCESS)
                .build();
        when(syncLogService.getSyncLogsForRequester(eq(supervisor), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(syncItem)));

        String json = toolExecutor.execute("consulter_etat_synchronisations", Map.of(), supervisor);
        assertTrue(json.contains("EXCELLENT"));
        assertTrue(json.contains("total_synchronisations_recemment_verifiees"));
    }

    @Test
    @DisplayName("creer_mission : prépare l'action avec confirmation, puis executeConfirmed la crée")
    void testCreerMissionFlow() {
        Map<String, Object> args = Map.of(
                "nom", "Recensement 2026",
                "description", "Campagne nationale",
                "collectes_attendues", 50
        );

        String prepareJson = toolExecutor.execute("creer_mission", args, supervisor);
        assertTrue(prepareJson.contains("pending_confirmation"));
        assertTrue(prepareJson.contains("Recensement 2026"));

        AssistantConfirmationService.PendingAction action = new AssistantConfirmationService.PendingAction(
                "uuid-mission",
                supervisor.getId(),
                "creer_mission",
                args,
                "Créer la mission 'Recensement 2026'",
                null
        );

        MissionResponse missionCreated = MissionResponse.builder()
                .id(99L)
                .name("Recensement 2026")
                .status(MissionStatus.DRAFT)
                .build();
        when(missionService.createMission(any(), eq(organization), eq(supervisor))).thenReturn(missionCreated);

        String confirmedJson = toolExecutor.executeConfirmed(action, supervisor);
        assertTrue(confirmedJson.contains("exécuté"));
        assertTrue(confirmedJson.contains("99"));
        assertTrue(confirmedJson.contains("Recensement 2026"));
    }

    @Test
    @DisplayName("naviguer_page renvoie la route et le nom de page appropriés")
    void testNaviguerPageSuccess() {
        String jsonMissions = toolExecutor.execute("naviguer_page", Map.of("page", "missions"), agent);
        assertTrue(jsonMissions.contains("/missions"));
        assertTrue(jsonMissions.contains("Missions"));

        String jsonUsers = toolExecutor.execute("naviguer_page", Map.of("page", "utilisateurs"), supervisor);
        assertTrue(jsonUsers.contains("/users"));
        assertTrue(jsonUsers.contains("Gestion des Utilisateurs"));

        String jsonUnknown = toolExecutor.execute("naviguer_page", Map.of("page", "introuvable"), agent);
        assertTrue(jsonUnknown.contains("non reconnue"));
    }

    @Test
    @DisplayName("exporter_collectes vérifie la permission et renvoie le format")
    void testExporterCollectes() {
        when(permissionService.hasPermission(supervisor, "VIEW_COLLECTE")).thenReturn(true);
        String jsonExcel = toolExecutor.execute("exporter_collectes", Map.of("format", "excel"), supervisor);
        assertTrue(jsonExcel.contains("excel"));
        assertTrue(jsonExcel.contains("success"));

        when(permissionService.hasPermission(agent, "VIEW_COLLECTE")).thenReturn(false);
        String jsonRefus = toolExecutor.execute("exporter_collectes", Map.of("format", "excel"), agent);
        assertTrue(jsonRefus.contains("Accès refusé"));
    }

    @Test
    @DisplayName("generer_rapport_mission : prépare confirmation et exécute")
    void testGenererRapportMissionFlow() {
        when(permissionService.hasPermission(supervisor, "GENERATE_REPORT")).thenReturn(true);
        MissionResponse m = MissionResponse.builder().id(200L).name("Campagne Vaccination").build();
        when(missionService.getMissionsForOrganization(organization)).thenReturn(List.of(m));
        when(missionService.getMission(200L, supervisor)).thenReturn(m);

        String prepareJson = toolExecutor.execute("generer_rapport_mission", Map.of("mission_name", "Vaccination"), supervisor);
        assertTrue(prepareJson.contains("pending_confirmation"));
        assertTrue(prepareJson.contains("Campagne Vaccination"));

        AssistantConfirmationService.PendingAction action = new AssistantConfirmationService.PendingAction(
                "action-rep", supervisor.getId(), "generer_rapport_mission",
                Map.of("mission_id", 200L), "Générer rapport", null
        );

        ReportResponse rep = ReportResponse.builder().id(77L).build();
        when(reportService.generateMissionReport(supervisor, 200L, null)).thenReturn(rep);

        String confirmedJson = toolExecutor.executeConfirmed(action, supervisor);
        assertTrue(confirmedJson.contains("77"));
        assertTrue(confirmedJson.contains("exécuté"));
    }

    @Test
    @DisplayName("changer_statut_mission : prépare lancement DRAFT -> ACTIVE puis exécute")
    void testChangerStatutMissionFlow() {
        MissionResponse m = MissionResponse.builder()
                .id(300L)
                .name("Recensement 2026")
                .status(MissionStatus.DRAFT)
                .build();
        when(missionService.getMissionsForOrganization(organization)).thenReturn(List.of(m));
        when(missionService.getMission(300L, supervisor)).thenReturn(m);

        String prepareJson = toolExecutor.execute("changer_statut_mission",
                Map.of("mission_name", "Recensement", "action", "lancer"), supervisor);
        assertTrue(prepareJson.contains("pending_confirmation"));
        assertTrue(prepareJson.contains("Lancer"));

        AssistantConfirmationService.PendingAction action = new AssistantConfirmationService.PendingAction(
                "action-status", supervisor.getId(), "changer_statut_mission",
                Map.of("mission_id", 300L, "target_status", "ACTIVE"), "Lancer mission", null
        );

        MissionResponse updated = MissionResponse.builder()
                .id(300L)
                .name("Recensement 2026")
                .status(MissionStatus.ACTIVE)
                .build();
        when(missionService.updateMission(eq(300L), any(), eq(supervisor))).thenReturn(updated);

        String confirmedJson = toolExecutor.executeConfirmed(action, supervisor);
        assertTrue(confirmedJson.contains("ACTIVE"));
        assertTrue(confirmedJson.contains("exécuté"));
    }

    @Test
    @DisplayName("changer_statut_mission : refuse transition invalide (ex: DRAFT -> COMPLETED)")
    void testChangerStatutMissionTransitionInvalide() {
        MissionResponse m = MissionResponse.builder()
                .id(301L)
                .name("Mission Test")
                .status(MissionStatus.DRAFT)
                .build();
        when(missionService.getMissionsForOrganization(organization)).thenReturn(List.of(m));
        when(missionService.getMission(301L, supervisor)).thenReturn(m);

        String json = toolExecutor.execute("changer_statut_mission",
                Map.of("mission_name", "Mission Test", "action", "cloturer"), supervisor);
        assertTrue(json.contains("Transition impossible"));
    }
}


