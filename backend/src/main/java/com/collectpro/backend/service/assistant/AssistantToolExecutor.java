package com.collectpro.backend.service.assistant;

import com.collectpro.backend.dto.CollecteResponse;
import com.collectpro.backend.dto.MissionProgressResponse;
import com.collectpro.backend.dto.MissionResponse;
import com.collectpro.backend.dto.StatisticsResponse;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.service.CollecteService;
import com.collectpro.backend.service.MissionService;
import com.collectpro.backend.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.collectpro.backend.dto.ReportResponse;
import com.collectpro.backend.dto.RejectCollecteRequest;
import com.collectpro.backend.dto.ValidateCollecteRequest;
import com.collectpro.backend.dto.CreateUserRequest;
import com.collectpro.backend.dto.UpdateUserStatusRequest;
import com.collectpro.backend.dto.AssignSupervisorRequest;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.enums.UserStatus;
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
import com.collectpro.backend.dto.UpdateMissionRequest;
import com.collectpro.backend.enums.MissionStatus;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantToolExecutor {

    private final MissionService missionService;
    private final CollecteService collecteService;
    private final StatisticsService statisticsService;
    private final ObjectMapper objectMapper;
    private final ReportService reportService;
    private final PermissionService permissionService;
    private final AssistantConfirmationService confirmationService;
    private final UserService userService;
    private final FormService formService;
    private final AuditLogQueryService auditLogQueryService;
    private final SyncLogService syncLogService;

    private static final Map<String, Set<RoleType>> ROUTE_ROLES = Map.of(
            "organizations", Set.of(RoleType.SUPER_ADMIN),
            "users", Set.of(RoleType.SUPER_ADMIN, RoleType.ADMIN_PRINCIPAL, RoleType.ADMIN_SECONDAIRE),
            "forms", Set.of(RoleType.ADMIN_PRINCIPAL, RoleType.ADMIN_SECONDAIRE),
            "collectes", Set.of(RoleType.ADMIN_PRINCIPAL, RoleType.ADMIN_SECONDAIRE, RoleType.SUPERVISOR),
            "audit-logs", Set.of(RoleType.SUPER_ADMIN, RoleType.ADMIN_PRINCIPAL, RoleType.ADMIN_SECONDAIRE),
            "sync-logs", Set.of(RoleType.SUPER_ADMIN, RoleType.ADMIN_PRINCIPAL, RoleType.ADMIN_SECONDAIRE, RoleType.SUPERVISOR),
            "missions", Set.of(RoleType.ADMIN_PRINCIPAL, RoleType.ADMIN_SECONDAIRE, RoleType.SUPERVISOR)
    );

    public String execute(String toolName, Map<String, Object> arguments, User actor) {
        log.info("Exécution du tool IA '{}' par l'utilisateur '{}' (rôle: {})",
                toolName, actor.getEmail(), actor.getRole().getName());
        try {
            Map<String, Object> safeArgs = (arguments != null) ? arguments : Collections.emptyMap();
            Object result = switch (toolName) {
                case "lister_missions" -> executeListerMissions(actor);
                case "consulter_avancement_mission" -> executeConsulterAvancementMission(safeArgs, actor);
                case "lister_collectes_en_attente" -> executeListerCollectesEnAttente(actor);
                case "lister_collectes" -> executeListerCollectes(safeArgs, actor);
                case "consulter_statistiques" -> executeConsulterStatistiques(safeArgs, actor);
                case "lister_utilisateurs" -> executeListerUtilisateurs(safeArgs, actor);
                case "lister_formulaires" -> executeListerFormulaires(actor);
                case "consulter_journal_audit" -> executeConsulterJournalAudit(safeArgs, actor);
                case "consulter_etat_synchronisations" -> executeConsulterEtatSynchronisations(actor);
                case "creer_mission" -> executePrepareCreerMission(safeArgs, actor);
                case "valider_collecte" -> executePrepareValiderCollecte(safeArgs, actor);
                case "rejeter_collecte" -> executePrepareRejeterCollecte(safeArgs, actor);
                case "generer_rapport_organisation" -> executePrepareGenererRapportOrganisation(actor);
                case "generer_rapport_mission" -> executePrepareGenererRapportMission(safeArgs, actor);
                case "changer_statut_mission" -> executePrepareChangerStatutMission(safeArgs, actor);
                case "creer_utilisateur" -> executePrepareCreerUtilisateur(safeArgs, actor);
                case "supprimer_utilisateur" -> executePrepareSupprimerUtilisateur(safeArgs, actor);
                case "affecter_agent_superviseur" -> executePrepareAffecterAgentSuperviseur(safeArgs, actor);
                case "naviguer_page" -> executeNaviguerPage(safeArgs, actor);
                case "exporter_collectes" -> executeExporterCollectes(safeArgs, actor);
                default -> Map.of("error", "Outil non reconnu : " + toolName);
            };
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution du tool '{}'", toolName, e);
            return "{\"error\": \"" + escapeJson(e.getMessage() != null ? e.getMessage() : "Erreur interne") + "\"}";
        }
    }

    private Object executeListerMissions(User actor) {
        List<MissionResponse> missions = getVisibleMissions(actor);
        List<Map<String, Object>> summary = missions.stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("nom", m.getName());
                    map.put("statut", m.getStatus() != null ? m.getStatus().name() : null);
                    map.put("date_debut", m.getStartDate() != null ? m.getStartDate().toString() : null);
                    map.put("date_fin", m.getEndDate() != null ? m.getEndDate().toString() : null);
                    map.put("collectes_attendues", m.getExpectedCollectesCount());
                    map.put("nb_agents", m.getAgents() != null ? m.getAgents().size() : 0);
                    return map;
                })
                .toList();

        return Map.of(
                "total_missions", summary.size(),
                "missions", summary
        );
    }

    private Object executeConsulterAvancementMission(Map<String, Object> args, User actor) {
        String missionName = (String) args.get("mission_name");
        if (missionName != null && !missionName.isBlank()) {
            Long missionId = resolveMissionIdByName(missionName.trim(), actor);
            if (missionId == null) {
                return Map.of(
                        "error", "Aucune mission trouvée contenant '" + missionName + "' dans vos missions visibles."
                );
            }
            MissionProgressResponse progress = missionService.getMissionProgress(missionId, actor);
            return formatMissionProgress(progress);
        }

        List<MissionProgressResponse> allProgress = missionService.getMissionsProgress(actor);
        return Map.of(
                "total_missions", allProgress.size(),
                "avancements", allProgress.stream().map(this::formatMissionProgress).toList()
        );
    }

    private Object executeListerCollectesEnAttente(User actor) {
        RoleType role = actor.getRole().getName();
        if (role != RoleType.SUPERVISOR) {
            return Map.of(
                    "error", "Accès refusé : La consultation des collectes en attente de validation est strictement réservée aux superviseurs d'équipe."
            );
        }

        Page<CollecteResponse> page = collecteService.getPendingValidationForSupervisor(actor, PageRequest.of(0, 20));
        return formatCollectesPage(page);
    }

    private Object executeListerCollectes(Map<String, Object> args, User actor) {
        String missionName = (String) args.get("mission_name");
        Long missionId = null;
        if (missionName != null && !missionName.isBlank()) {
            missionId = resolveMissionIdByName(missionName.trim(), actor);
            if (missionId == null) {
                return Map.of("error", "Aucune mission trouvée pour le nom '" + missionName + "'");
            }
        }

        RoleType role = actor.getRole().getName();
        if (role == RoleType.AGENT) {
            List<CollecteResponse> agentCollectes = collecteService.getCollectesForAgent(actor);
            if (missionId != null) {
                final Long targetMissionId = missionId;
                agentCollectes = agentCollectes.stream()
                        .filter(c -> targetMissionId.equals(c.getMissionId()))
                        .toList();
            }
            return Map.of(
                    "total", agentCollectes.size(),
                    "collectes", agentCollectes.stream().map(this::formatCollecteSummary).toList()
            );
        }

        if (role == RoleType.SUPERVISOR) {
            Page<CollecteResponse> page = collecteService.getCollectesForSupervisor(actor, missionId, PageRequest.of(0, 20));
            return formatCollectesPage(page);
        }

        Page<CollecteResponse> page = collecteService.getCollectesForAdmin(actor, missionId, PageRequest.of(0, 20));
        return formatCollectesPage(page);
    }

    private Object executeConsulterStatistiques(Map<String, Object> args, User actor) {
        RoleType role = actor.getRole().getName();
        if (role == RoleType.AGENT) {
            return Map.of(
                    "error", "Accès refusé : Les statistiques d'activité ne sont accessibles qu'aux superviseurs et administrateurs."
            );
        }

        int trendDays = 30;
        Object rawDays = args.get("trend_days");
        if (rawDays instanceof Number num) {
            trendDays = num.intValue();
        } else if (rawDays instanceof String str && !str.isBlank()) {
            try {
                trendDays = Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {}
        }
        if (trendDays <= 0) {
            trendDays = 30;
        }

        StatisticsResponse stats = statisticsService.getStatistics(actor, trendDays);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("scope", stats.getScope());
        res.put("periode_jours", trendDays);
        res.put("total_collectes", stats.getTotalCollectes());
        res.put("collectes_en_attente", stats.getPendingCollectes());
        res.put("collectes_validees", stats.getValidatedCollectes());
        res.put("collectes_rejetees", stats.getRejectedCollectes());
        res.put("temps_moyen_validation_heures", stats.getAvgValidationTimeHours());
        res.put("total_utilisateurs", stats.getTotalUsers());
        res.put("total_formulaires", stats.getTotalForms());
        return res;
    }

    private List<MissionResponse> getVisibleMissions(User actor) {
        RoleType role = actor.getRole().getName();
        if (role == RoleType.SUPER_ADMIN) {
            return missionService.getAllMissions();
        }
        if (role == RoleType.AGENT) {
            return missionService.getMissionsForAgent(actor);
        }
        if (actor.getOrganization() != null) {
            return missionService.getMissionsForOrganization(actor.getOrganization());
        }
        return Collections.emptyList();
    }

    private Long resolveMissionIdByName(String name, User actor) {
        String query = name.toLowerCase().trim();
        List<MissionResponse> visible = getVisibleMissions(actor);
        for (MissionResponse m : visible) {
            if (m.getName() != null && m.getName().toLowerCase().contains(query)) {
                return m.getId();
            }
        }
        return null;
    }

    private Map<String, Object> formatMissionProgress(MissionProgressResponse p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getMissionId());
        map.put("nom", p.getMissionName());
        map.put("collectes_recues", p.getReceivedCollectesCount());
        map.put("collectes_attendues", p.getExpectedCollectesCount());
        map.put("taux_completion_pct", p.getProgressPercent());
        map.put("agents_assignes", p.getAssignedAgentsCount());
        map.put("agents_actifs", p.getActiveAgentsCount());
        return map;
    }

    private Map<String, Object> formatCollectesPage(Page<CollecteResponse> page) {
        List<Map<String, Object>> items = page.getContent().stream()
                .map(this::formatCollecteSummary)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_elements", page.getTotalElements());
        result.put("page", page.getNumber());
        result.put("total_pages", page.getTotalPages());
        result.put("collectes", items);
        return result;
    }

    private Map<String, Object> formatCollecteSummary(CollecteResponse c) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("statut", c.getStatus() != null ? c.getStatus().name() : null);
        map.put("mission_id", c.getMissionId());
        if (c.getAgent() != null) {
            map.put("agent_id", c.getAgent().getId());
            map.put("agent_nom", (c.getAgent().getFirstName() + " " + c.getAgent().getLastName()).trim());
        }
        map.put("date_creation", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        map.put("hors_zone", c.getOutsideMissionZone());
        map.put("commentaire_validation", c.getValidationComment());
        return map;
    }

    private String escapeJson(String input) {
        return input.replace("\"", "\\\"");
    }



    private Object executePrepareValiderCollecte(Map<String, Object> args, User actor) {
        if (actor.getRole().getName() != RoleType.SUPERVISOR) {
            return Map.of("error", "Accès refusé : seuls les superviseurs peuvent valider une collecte.");
        }
        Long collecteId = extractLong(args.get("collecte_id"));
        if (collecteId == null) {
            return Map.of("error", "Identifiant de collecte manquant ou invalide.");
        }

        String summary = "Valider la collecte #" + collecteId;
        confirmationService.create(actor.getId(), "valider_collecte", Map.of("collecte_id", collecteId), summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement cette action avant qu'elle soit exécutée."
        );
    }

    private Object executePrepareRejeterCollecte(Map<String, Object> args, User actor) {
        if (actor.getRole().getName() != RoleType.SUPERVISOR) {
            return Map.of("error", "Accès refusé : seuls les superviseurs peuvent rejeter une collecte.");
        }
        Long collecteId = extractLong(args.get("collecte_id"));
        Object motifObj = args.get("motif");
        String motif = motifObj instanceof String s ? s.trim() : null;

        if (collecteId == null) {
            return Map.of("error", "Identifiant de collecte manquant ou invalide.");
        }
        if (motif == null || motif.isBlank()) {
            return Map.of("error", "Un motif est obligatoire pour rejeter une collecte. Demande le motif à l'utilisateur avant de proposer l'action.");
        }

        String summary = "Rejeter la collecte #" + collecteId + " (motif : " + motif + ")";
        confirmationService.create(actor.getId(), "rejeter_collecte",
                Map.of("collecte_id", collecteId, "motif", motif), summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement cette action avant qu'elle soit exécutée."
        );
    }

    private Object executePrepareGenererRapportOrganisation(User actor) {
        if (!permissionService.hasPermission(actor, "GENERATE_REPORT")) {
            return Map.of("error", "Accès refusé : vous n'avez pas la permission de générer des rapports.");
        }
        if (actor.getOrganization() == null) {
            return Map.of("error", "Aucune organisation associée à votre compte.");
        }

        String summary = "Générer le rapport PDF complet de l'organisation " + actor.getOrganization().getName();
        confirmationService.create(actor.getId(), "generer_rapport_organisation", Map.of(), summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement cette action avant qu'elle soit exécutée."
        );
    }

    /**
     * Exécution RÉELLE, appelée uniquement par l'orchestrateur après confirmation positive.
     * Ne jamais appeler directement depuis execute() — bypass volontaire du LLM pour ce chemin.
     */
    public String executeConfirmed(AssistantConfirmationService.PendingAction action, User actor) {
        try {
            Object result = switch (action.toolName()) {
                case "valider_collecte" -> {
                    Long collecteId = extractLong(action.arguments().get("collecte_id"));
                    CollecteResponse response = collecteService.validateCollecte(
                            collecteId, actor, new ValidateCollecteRequest());
                    yield Map.of("statut", "exécuté", "collecte_id", response.getId(), "nouveau_statut", response.getStatus());
                }
                case "rejeter_collecte" -> {
                    Long collecteId = extractLong(action.arguments().get("collecte_id"));
                    String motif = (String) action.arguments().get("motif");
                    RejectCollecteRequest req = new RejectCollecteRequest();
                    req.setComment(motif);
                    CollecteResponse response = collecteService.rejectCollecte(collecteId, actor, req);
                    yield Map.of("statut", "exécuté", "collecte_id", response.getId(), "nouveau_statut", response.getStatus());
                }
                case "generer_rapport_organisation" -> {
                    ReportResponse response = reportService.generateOrganizationReport(
                            actor, actor.getOrganization().getId(), null);
                    yield Map.of("statut", "exécuté", "rapport_id", response.getId());
                }
                case "generer_rapport_mission" -> {
                    Long missionId = extractLong(action.arguments().get("mission_id"));
                    ReportResponse response = reportService.generateMissionReport(actor, missionId, null);
                    yield Map.of("statut", "exécuté", "rapport_id", response.getId(), "mission_id", missionId);
                }
                case "changer_statut_mission" -> {
                    Long missionId = extractLong(action.arguments().get("mission_id"));
                    MissionStatus targetStatus = MissionStatus.valueOf((String) action.arguments().get("target_status"));
                    MissionResponse response = switch (targetStatus) {
                        case ACTIVE -> missionService.activateMission(missionId, actor);
                        case CANCELLED -> missionService.cancelMission(missionId, actor);
                        case COMPLETED -> missionService.completeMission(missionId, actor);
                        default -> {
                            MissionResponse existing = missionService.getMission(missionId, actor);
                            UpdateMissionRequest req = new UpdateMissionRequest();
                            req.setName(existing.getName());
                            req.setDescription(existing.getDescription());
                            req.setStartDate(existing.getStartDate());
                            req.setEndDate(existing.getEndDate());
                            req.setLatitude(existing.getLatitude());
                            req.setLongitude(existing.getLongitude());
                            req.setRadiusMeters(existing.getRadiusMeters());
                            req.setExpectedCollectesCount(existing.getExpectedCollectesCount());
                            req.setStatus(targetStatus);
                            yield missionService.updateMission(missionId, req, actor);
                        }
                    };
                    yield Map.of("statut", "exécuté", "mission_id", response.getId(), "nom", response.getName(),
                            "nouveau_statut", response.getStatus().name(),
                            "message", "La mission '" + response.getName() + "' est désormais en statut " + response.getStatus().name());
                }
                case "creer_utilisateur" -> {
                    CreateUserRequest req = new CreateUserRequest();
                    req.setFirstName((String) action.arguments().get("first_name"));
                    req.setLastName((String) action.arguments().get("last_name"));
                    req.setEmail((String) action.arguments().get("email"));
                    req.setPhone((String) action.arguments().get("phone"));
                    req.setRoleType(RoleType.valueOf(((String) action.arguments().get("role_type")).toUpperCase().trim()));
                    UserResponse response = userService.createUser(actor, req);
                    yield Map.of("statut", "exécuté", "message", "Utilisateur créé avec succès et invitation envoyée",
                            "user_id", response.getId(), "email", response.getEmail(), "role", response.getRole());
                }
                case "supprimer_utilisateur" -> {
                    Long targetId = extractLong(action.arguments().get("user_id"));
                    UpdateUserStatusRequest req = new UpdateUserStatusRequest();
                    req.setStatus(UserStatus.DISABLED);
                    UserResponse response = userService.updateUserStatus(actor, targetId, req);
                    yield Map.of("statut", "exécuté", "message", "Compte désactivé avec succès",
                            "user_id", response.getId(), "nouveau_statut", response.getStatus());
                }
                case "affecter_agent_superviseur" -> {
                    Long agentId = extractLong(action.arguments().get("agent_id"));
                    Long supervisorId = extractLong(action.arguments().get("supervisor_id"));
                    AssignSupervisorRequest req = new AssignSupervisorRequest();
                    req.setAgentId(agentId);
                    req.setSupervisorId(supervisorId);
                    userService.assignAgentToSupervisor(actor, req);
                    yield Map.of("statut", "exécuté", "message", "Agent affecté avec succès au superviseur");
                }
                case "creer_mission" -> {
                    String nom = (String) action.arguments().get("nom");
                    String description = (String) action.arguments().get("description");
                    Integer expected = extractInteger(action.arguments().get("collectes_attendues"));
                    CreateMissionRequest req = new CreateMissionRequest();
                    req.setName(nom);
                    req.setDescription(description);
                    req.setExpectedCollectesCount(expected);
                    MissionResponse response = missionService.createMission(req, actor.getOrganization(), actor);
                    yield Map.of("statut", "exécuté", "mission_id", response.getId(), "nom", response.getName(),
                            "message", "Mission créée avec succès en statut DRAFT");
                }
                default -> Map.of("error", "Action confirmée inconnue : " + action.toolName());
            };
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution confirmée de l'action '{}'", action.toolName(), e);
            return "{\"error\": \"" + escapeJson(e.getMessage() != null ? e.getMessage() : "Erreur interne") + "\"}";
        }
    }

    private Object executePrepareCreerUtilisateur(Map<String, Object> args, User actor) {
        if (!permissionService.hasPermission(actor, "CREATE_USER")) {
            return Map.of("error", "Accès refusé : vous ne disposez pas de la permission de créer un compte utilisateur.");
        }
        if (actor.getOrganization() == null) {
            return Map.of("error", "Vous ne pouvez pas créer d'utilisateur sans être rattaché à une organisation.");
        }

        String firstName = extractString(args.get("first_name"), args.get("prenom"));
        String lastName = extractString(args.get("last_name"), args.get("nom"));
        String email = extractString(args.get("email"));
        String phone = extractString(args.get("phone"), args.get("telephone"));
        String roleStr = extractString(args.get("role_type"), args.get("role"));

        if (firstName == null || lastName == null || email == null || roleStr == null) {
            return Map.of("error", "Informations incomplètes. Prénom, nom, email et rôle (AGENT, SUPERVISOR ou ADMIN_SECONDAIRE) sont requis.");
        }

        RoleType roleType;
        try {
            roleType = RoleType.valueOf(roleStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return Map.of("error", "Rôle invalide '" + roleStr + "'. Les rôles autorisés sont : AGENT, SUPERVISOR, ADMIN_SECONDAIRE.");
        }

        if (roleType != RoleType.AGENT && roleType != RoleType.SUPERVISOR && roleType != RoleType.ADMIN_SECONDAIRE) {
            return Map.of("error", "Création vocale non autorisée pour le rôle " + roleType + ". Seuls AGENT, SUPERVISOR et ADMIN_SECONDAIRE sont permis.");
        }

        String summary = "Créer l'utilisateur " + firstName + " " + lastName + " (" + roleType + ", " + email + ")";
        Map<String, Object> pendingArgs = new HashMap<>();
        pendingArgs.put("first_name", firstName);
        pendingArgs.put("last_name", lastName);
        pendingArgs.put("email", email);
        if (phone != null) pendingArgs.put("phone", phone);
        pendingArgs.put("role_type", roleType.name());

        confirmationService.create(actor.getId(), "creer_utilisateur", pendingArgs, summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement la création de ce compte."
        );
    }

    private Object executePrepareSupprimerUtilisateur(Map<String, Object> args, User actor) {
        if (!permissionService.hasPermission(actor, "DISABLE_USER")) {
            return Map.of("error", "Accès refusé : vous ne disposez pas de la permission de désactiver un compte utilisateur.");
        }

        Long userId = extractLong(args.get("user_id"));
        String email = extractString(args.get("email"));
        String name = extractString(args.get("name"), args.get("nom"));

        Page<UserResponse> usersPage = userService.getUsers(actor, PageRequest.of(0, 100));
        UserResponse target = null;

        if (userId != null) {
            target = usersPage.getContent().stream()
                    .filter(u -> u.getId().equals(userId))
                    .findFirst().orElse(null);
        } else if (email != null) {
            target = usersPage.getContent().stream()
                    .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                    .findFirst().orElse(null);
        } else if (name != null) {
            String q = name.toLowerCase().trim();
            target = usersPage.getContent().stream()
                    .filter(u -> (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q))
                            || (u.getLastName() != null && u.getLastName().toLowerCase().contains(q))
                            || ((u.getFirstName() + " " + u.getLastName()).toLowerCase().contains(q)))
                    .findFirst().orElse(null);
        }

        if (target == null) {
            return Map.of("error", "Utilisateur introuvable dans votre organisation. Précisez l'identifiant ou l'email exact.");
        }

        if (target.getId().equals(actor.getId()) && actor.getRole().getName() == RoleType.ADMIN_PRINCIPAL) {
            return Map.of("error", "Un Administrateur principal ne peut pas désactiver son propre compte.");
        }

        String summary = "Désactiver le compte de " + target.getFirstName() + " " + target.getLastName() + " (" + target.getEmail() + ")";
        confirmationService.create(actor.getId(), "supprimer_utilisateur",
                Map.of("user_id", target.getId(), "email", target.getEmail()), summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement la désactivation de ce compte."
        );
    }

    private Object executePrepareAffecterAgentSuperviseur(Map<String, Object> args, User actor) {
        RoleType actorRole = actor.getRole().getName();
        if (actorRole != RoleType.ADMIN_PRINCIPAL && actorRole != RoleType.ADMIN_SECONDAIRE && actorRole != RoleType.SUPER_ADMIN) {
            return Map.of("error", "Accès refusé : seuls les administrateurs peuvent affecter un agent à un superviseur.");
        }

        Long agentId = extractLong(args.get("agent_id"));
        String agentQuery = extractString(args.get("agent_name_or_email"), args.get("agent"));
        Long supervisorId = extractLong(args.get("supervisor_id"));
        String supQuery = extractString(args.get("supervisor_name_or_email"), args.get("superviseur"));

        Page<UserResponse> usersPage = userService.getUsers(actor, PageRequest.of(0, 100));

        UserResponse agent = null;
        if (agentId != null) {
            agent = usersPage.getContent().stream().filter(u -> u.getId().equals(agentId)).findFirst().orElse(null);
        } else if (agentQuery != null) {
            String q = agentQuery.toLowerCase().trim();
            agent = usersPage.getContent().stream()
                    .filter(u -> "AGENT".equalsIgnoreCase(u.getRole()))
                    .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                            || (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q))
                            || (u.getLastName() != null && u.getLastName().toLowerCase().contains(q))
                            || ((u.getFirstName() + " " + u.getLastName()).toLowerCase().contains(q)))
                    .findFirst().orElse(null);
        }

        UserResponse supervisor = null;
        if (supervisorId != null) {
            supervisor = usersPage.getContent().stream().filter(u -> u.getId().equals(supervisorId)).findFirst().orElse(null);
        } else if (supQuery != null) {
            String q = supQuery.toLowerCase().trim();
            supervisor = usersPage.getContent().stream()
                    .filter(u -> "SUPERVISOR".equalsIgnoreCase(u.getRole()))
                    .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                            || (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q))
                            || (u.getLastName() != null && u.getLastName().toLowerCase().contains(q))
                            || ((u.getFirstName() + " " + u.getLastName()).toLowerCase().contains(q)))
                    .findFirst().orElse(null);
        }

        if (agent == null) {
            return Map.of("error", "Agent introuvable ou non reconnu dans votre organisation.");
        }
        if (supervisor == null) {
            return Map.of("error", "Superviseur introuvable ou non reconnu dans votre organisation.");
        }

        String summary = "Affecter l'agent " + agent.getFirstName() + " " + agent.getLastName()
                + " au superviseur " + supervisor.getFirstName() + " " + supervisor.getLastName();

        confirmationService.create(actor.getId(), "affecter_agent_superviseur",
                Map.of("agent_id", agent.getId(), "supervisor_id", supervisor.getId()), summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement cette affectation."
        );
    }

    private String extractString(Object... rawCandidates) {
        for (Object raw : rawCandidates) {
            if (raw instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
    }

    private Long extractLong(Object raw) {
        if (raw instanceof Number num) return num.longValue();
        if (raw instanceof String str) {
            try { return Long.parseLong(str.trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Integer extractInteger(Object raw) {
        if (raw instanceof Number num) return num.intValue();
        if (raw instanceof String str) {
            try { return Integer.parseInt(str.trim()); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private Object executeListerUtilisateurs(Map<String, Object> args, User actor) {
        RoleType role = actor.getRole().getName();
        if (role == RoleType.AGENT) {
            return Map.of("error", "Accès refusé : les agents de terrain ne peuvent pas lister les utilisateurs.");
        }
        Page<UserResponse> usersPage = userService.getUsers(actor, PageRequest.of(0, 50));
        String targetRole = args.get("role") != null ? ((String) args.get("role")).trim().toUpperCase() : null;
        String search = args.get("search") != null ? ((String) args.get("search")).trim().toLowerCase() : null;

        List<Map<String, Object>> filtered = usersPage.getContent().stream()
                .filter(u -> {
                    if (targetRole != null && !targetRole.isEmpty() && !targetRole.equalsIgnoreCase(u.getRole())) {
                        return false;
                    }
                    if (search != null && !search.isEmpty()) {
                        String fullName = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                                + (u.getLastName() != null ? u.getLastName() : "")).toLowerCase();
                        String email = u.getEmail() != null ? u.getEmail().toLowerCase() : "";
                        return fullName.contains(search) || email.contains(search);
                    }
                    return true;
                })
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("nom_complet", ((u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).trim());
                    m.put("email", u.getEmail());
                    m.put("role", u.getRole());
                    m.put("statut", u.getStatus());
                    if (u.getAssignedSupervisorName() != null) {
                        m.put("superviseur", u.getAssignedSupervisorName());
                    }
                    return m;
                })
                .toList();

        return Map.of(
                "total_utilisateurs_trouves", filtered.size(),
                "utilisateurs", filtered
        );
    }

    private Object executeListerFormulaires(User actor) {
        if (actor.getOrganization() == null && actor.getRole().getName() != RoleType.SUPER_ADMIN) {
            return Map.of("error", "Aucune organisation rattachée.");
        }
        List<FormResponse> forms = formService.getFormsForOrganization(actor.getOrganization());
        List<Map<String, Object>> items = forms.stream()
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", f.getId());
                    m.put("nom", f.getName());
                    m.put("description", f.getDescription());
                    m.put("statut", f.getStatus());
                    return m;
                })
                .toList();

        return Map.of(
                "total_formulaires", items.size(),
                "formulaires", items
        );
    }

    private Object executeConsulterJournalAudit(Map<String, Object> args, User actor) {
        RoleType role = actor.getRole().getName();
        if (role != RoleType.SUPER_ADMIN && role != RoleType.ADMIN_PRINCIPAL && role != RoleType.ADMIN_SECONDAIRE) {
            return Map.of("error", "Accès refusé : la consultation du journal d'audit est réservée aux administrateurs.");
        }
        int limit = 5;
        if (args.containsKey("limit")) {
            Integer requested = extractInteger(args.get("limit"));
            if (requested != null && requested > 0) {
                limit = Math.min(requested, 15);
            }
        }
        Page<AuditLogResponse> page = auditLogQueryService.getAuditLogsForUser(
                actor, PageRequest.of(0, limit));

        List<Map<String, Object>> items = page.getContent().stream()
                .map(log -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("action", log.getAction());
                    m.put("date", log.getCreatedAt());
                    m.put("entite", log.getEntityType());
                    m.put("auteur", log.getActor() != null ? (log.getActor().getFirstName() + " " + log.getActor().getLastName()) : "Système");
                    m.put("details", log.getDetails());
                    return m;
                })
                .toList();

        return Map.of(
                "total_evenements_recents", items.size(),
                "evenements", items
        );
    }

    private Object executeConsulterEtatSynchronisations(User actor) {
        RoleType role = actor.getRole().getName();
        if (role == RoleType.AGENT) {
            return Map.of("error", "Accès refusé aux agents.");
        }
        Page<SyncLogResponse> page = syncLogService.getSyncLogsForRequester(
                actor, null, PageRequest.of(0, 20));

        long errors = page.getContent().stream()
                .filter(s -> s.getResult() != null && !"SUCCESS".equalsIgnoreCase(s.getResult().name()))
                .count();

        List<Map<String, Object>> recentErrors = page.getContent().stream()
                .filter(s -> s.getResult() != null && !"SUCCESS".equalsIgnoreCase(s.getResult().name()))
                .limit(5)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("agent", s.getAgent() != null ? (s.getAgent().getFirstName() + " " + s.getAgent().getLastName()) : "Inconnu");
                    m.put("erreur", s.getErrorMessage());
                    m.put("date", s.getCreatedAt());
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_synchronisations_recemment_verifiees", page.getNumberOfElements());
        result.put("nombre_erreurs", errors);
        result.put("etat_general", errors == 0 ? "EXCELLENT : aucune erreur de synchronisation récente" : "ATTENTION : erreurs détectées");
        result.put("dernieres_erreurs", recentErrors);
        return result;
    }

    private Object executePrepareCreerMission(Map<String, Object> args, User actor) {
        RoleType role = actor.getRole().getName();
        if (role == RoleType.AGENT) {
            return Map.of("error", "Accès refusé : les agents ne peuvent pas créer de mission.");
        }
        if (actor.getOrganization() == null) {
            return Map.of("error", "Aucune organisation associée à votre compte.");
        }
        String nom = (String) args.get("nom");
        if (nom == null || nom.isBlank()) {
            return Map.of("error", "Le nom de la mission est obligatoire.");
        }
        String description = (String) args.get("description");
        Integer collectesAttendues = extractInteger(args.get("collectes_attendues"));

        String summary = "Créer la mission '" + nom.trim() + "'"
                + (collectesAttendues != null ? " avec " + collectesAttendues + " collectes attendues" : "");

        Map<String, Object> savedArgs = new HashMap<>();
        savedArgs.put("nom", nom.trim());
        if (description != null) savedArgs.put("description", description.trim());
        if (collectesAttendues != null) savedArgs.put("collectes_attendues", collectesAttendues);

        confirmationService.create(actor.getId(), "creer_mission", savedArgs, summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement la création de cette mission."
        );
    }

    private Object executeNaviguerPage(Map<String, Object> args, User actor) {
        String pageRaw = (String) args.get("page");
        if (pageRaw == null || pageRaw.isBlank()) {
            return Map.of("error", "Veuillez spécifier la page vers laquelle vous souhaitez naviguer.");
        }

        String page = pageRaw.toLowerCase().trim();
        String routeKey;
        String route;
        String pageName;

        if (page.contains("dash") || page.contains("accueil") || page.contains("tableau") || page.contains("bord")) {
            routeKey = "dashboard";
            route = "/dashboard";
            pageName = "Tableau de bord";
        } else if (page.contains("mission")) {
            routeKey = "missions";
            route = "/missions";
            pageName = "Missions";
        } else if (page.contains("collecte")) {
            routeKey = "collectes";
            route = "/collectes";
            pageName = "Collectes";
        } else if (page.contains("user") || page.contains("utilisat") || page.contains("equipe") || page.contains("membre")) {
            routeKey = "users";
            route = "/users";
            pageName = "Gestion des Utilisateurs";
        } else if (page.contains("form") || page.contains("questionnaire")) {
            routeKey = "forms";
            route = "/forms";
            pageName = "Formulaires";
        } else if (page.contains("audit") || page.contains("journal")) {
            routeKey = "audit-logs";
            route = "/audit-logs";
            pageName = "Journal d'audit";
        } else if (page.contains("sync") || page.contains("synchronis")) {
            routeKey = "sync-logs";
            route = "/sync-logs";
            pageName = "Synchronisations";
        } else if (page.contains("organis") || page.contains("entreprise")) {
            routeKey = "organizations";
            route = "/organizations";
            pageName = "Organisations";
        } else if (page.contains("profil") || page.contains("profile") || page.contains("compte")) {
            routeKey = "profile";
            route = "/profile";
            pageName = "Mon Profil";
        } else {
            return Map.of("error", "Page '" + pageRaw + "' non reconnue. Les pages autorisées sont : dashboard, organizations, users, forms, collectes, missions, sync-logs, audit-logs, profile.");
        }

        RoleType userRole = (actor.getRole() != null) ? actor.getRole().getName() : null;
        Set<RoleType> allowedRoles = ROUTE_ROLES.get(routeKey);
        if (allowedRoles != null && (userRole == null || !allowedRoles.contains(userRole))) {
            return Map.of("error", "Accès refusé : votre rôle (" + (userRole != null ? userRole.name() : "non défini")
                    + ") ne vous permet pas d'accéder à la page '" + pageName + "'.");
        }

        return Map.of(
                "status", "success",
                "route", route,
                "page_cible", pageName,
                "message", "Navigation vers la page " + pageName
        );
    }

    private Object executeExporterCollectes(Map<String, Object> args, User actor) {
        if (!permissionService.hasPermission(actor, "VIEW_COLLECTE")) {
            return Map.of("error", "Accès refusé : vous n'avez pas la permission d'exporter les données de collecte.");
        }

        String formatRaw = (String) args.get("format");
        String format = (formatRaw != null && formatRaw.toLowerCase().contains("csv")) ? "csv" : "excel";

        String missionName = extractString(args.get("mission_name"), args.get("mission"));
        Long missionId = null;
        String missionLabel = "";
        if (missionName != null && !missionName.isBlank()) {
            missionId = resolveMissionIdByName(missionName, actor);
            if (missionId != null) {
                try {
                    MissionResponse m = missionService.getMission(missionId, actor);
                    missionLabel = " pour la mission '" + m.getName() + "'";
                } catch (Exception ignored) {
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("format", format);
        if (missionId != null) {
            result.put("mission_id", missionId);
        }
        result.put("message", "Export des collectes" + missionLabel + " prêt au téléchargement au format "
                + ("excel".equals(format) ? "Excel (.xlsx)" : "CSV (.csv)") + ".");
        return result;
    }

    private Object executePrepareGenererRapportMission(Map<String, Object> args, User actor) {
        if (!permissionService.hasPermission(actor, "GENERATE_REPORT")) {
            return Map.of("error", "Accès refusé : vous ne disposez pas de la permission de générer des rapports.");
        }
        Long missionId = extractLong(args.get("mission_id"));
        String missionName = extractString(args.get("mission_name"), args.get("mission"));

        if (missionId == null && (missionName == null || missionName.isBlank())) {
            return Map.of("error", "Veuillez spécifier le nom ou l'identifiant de la mission pour laquelle générer le rapport.");
        }

        if (missionId == null) {
            missionId = resolveMissionIdByName(missionName, actor);
            if (missionId == null) {
                return Map.of("error", "Aucune mission trouvée correspondant à '" + missionName + "'.");
            }
        }

        MissionResponse mission = missionService.getMission(missionId, actor);
        String summary = "Générer le rapport PDF complet de la mission '" + mission.getName() + "'";
        confirmationService.create(actor.getId(), "generer_rapport_mission",
                Map.of("mission_id", missionId, "mission_name", mission.getName()), summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement la génération de ce rapport de mission (oui/non)."
        );
    }

    private Object executePrepareChangerStatutMission(Map<String, Object> args, User actor) {
        RoleType role = actor.getRole().getName();
        if (role == RoleType.AGENT) {
            return Map.of("error", "Accès refusé : les agents de terrain ne peuvent pas modifier le statut d'une mission.");
        }

        Long missionId = extractLong(args.get("mission_id"));
        String missionName = extractString(args.get("mission_name"), args.get("nom"), args.get("mission"));
        String actionStr = extractString(args.get("action"), args.get("statut"), args.get("nouveau_statut"));

        if (missionId == null && (missionName == null || missionName.isBlank())) {
            return Map.of("error", "Veuillez spécifier le nom ou l'identifiant de la mission.");
        }

        if (actionStr == null || actionStr.isBlank()) {
            return Map.of("error", "Veuillez préciser l'action souhaitée : lancer, clôturer ou annuler la mission.");
        }

        if (missionId == null) {
            missionId = resolveMissionIdByName(missionName, actor);
            if (missionId == null) {
                return Map.of("error", "Aucune mission trouvée correspondant à '" + missionName + "'.");
            }
        }

        MissionResponse mission = missionService.getMission(missionId, actor);
        MissionStatus currentStatus = mission.getStatus();

        String actionLower = actionStr.toLowerCase().trim();
        MissionStatus targetStatus;
        String actionVerb;

        if (actionLower.contains("lanc") || actionLower.contains("demarr") || actionLower.contains("activ")) {
            targetStatus = MissionStatus.ACTIVE;
            actionVerb = "Lancer";
        } else if (actionLower.contains("clot") || actionLower.contains("termin") || actionLower.contains("complet") || actionLower.contains("fin")) {
            targetStatus = MissionStatus.COMPLETED;
            actionVerb = "Clôturer";
        } else if (actionLower.contains("annul") || actionLower.contains("cancel")) {
            targetStatus = MissionStatus.CANCELLED;
            actionVerb = "Annuler";
        } else {
            return Map.of("error", "Action '" + actionStr + "' non reconnue. Vous pouvez 'lancer' (ACTIVE), 'clôturer' (COMPLETED) ou 'annuler' (CANCELLED) la mission.");
        }

        if (currentStatus == targetStatus) {
            return Map.of("error", "La mission '" + mission.getName() + "' est déjà en statut " + currentStatus + ".");
        }

        boolean isValid = switch (currentStatus) {
            case DRAFT -> targetStatus == MissionStatus.ACTIVE || targetStatus == MissionStatus.CANCELLED;
            case ACTIVE -> targetStatus == MissionStatus.COMPLETED || targetStatus == MissionStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!isValid) {
            return Map.of("error", "Transition impossible : une mission en statut " + currentStatus
                    + " ne peut pas passer en statut " + targetStatus + ".");
        }

        String summary = actionVerb + " la mission '" + mission.getName() + "' (passage de " + currentStatus + " à " + targetStatus + ")";
        confirmationService.create(actor.getId(), "changer_statut_mission",
                Map.of("mission_id", missionId, "target_status", targetStatus.name(), "mission_name", mission.getName()), summary);

        return Map.of(
                "pending_confirmation", true,
                "summary", summary,
                "instruction_pour_toi", "Demande à l'utilisateur de confirmer explicitement cette action sur la mission (oui/non)."
        );
    }
}


