package com.collectpro.backend.service.assistant;

import com.collectpro.backend.dto.assistant.ollama.OllamaFunctionDefinition;
import com.collectpro.backend.dto.assistant.ollama.OllamaTool;
import com.collectpro.backend.enums.RoleType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class AssistantToolRegistry {

    public List<OllamaTool> getToolsForRole(RoleType role) {
        if (role == null) {
            return getAllTools();
        }
        return switch (role) {
            case AGENT -> List.of(
                    buildListerMissionsTool(),
                    buildListerCollectesTool(),
                    buildListerFormulairesTool(),
                    buildNaviguerPageTool()
            );
            case SUPERVISOR -> List.of(
                    buildListerMissionsTool(),
                    buildConsulterAvancementMissionTool(),
                    buildListerCollectesEnAttenteTool(),
                    buildListerCollectesTool(),
                    buildConsulterStatistiquesTool(),
                    buildListerUtilisateursTool(),
                    buildListerFormulairesTool(),
                    buildConsulterEtatSynchronisationsTool(),
                    buildCreerMissionTool(),
                    buildValiderCollecteTool(),
                    buildRejeterCollecteTool(),
                    buildGenererRapportMissionTool(),
                    buildChangerStatutMissionTool(),
                    buildExporterCollectesTool(),
                    buildNaviguerPageTool()
            );
            case ADMIN_PRINCIPAL, ADMIN_SECONDAIRE, SUPER_ADMIN -> List.of(
                    buildListerMissionsTool(),
                    buildConsulterAvancementMissionTool(),
                    buildListerCollectesEnAttenteTool(),
                    buildListerCollectesTool(),
                    buildConsulterStatistiquesTool(),
                    buildListerUtilisateursTool(),
                    buildListerFormulairesTool(),
                    buildConsulterJournalAuditTool(),
                    buildConsulterEtatSynchronisationsTool(),
                    buildCreerMissionTool(),
                    buildValiderCollecteTool(),
                    buildRejeterCollecteTool(),
                    buildGenererRapportOrganisationTool(),
                    buildGenererRapportMissionTool(),
                    buildChangerStatutMissionTool(),
                    buildCreerUtilisateurTool(),
                    buildSupprimerUtilisateurTool(),
                    buildAffecterAgentSuperviseurTool(),
                    buildExporterCollectesTool(),
                    buildNaviguerPageTool()
            );
        };
    }

    public List<OllamaTool> getAllTools() {
        return List.of(
                buildListerMissionsTool(),
                buildConsulterAvancementMissionTool(),
                buildListerCollectesEnAttenteTool(),
                buildListerCollectesTool(),
                buildConsulterStatistiquesTool(),
                buildListerUtilisateursTool(),
                buildListerFormulairesTool(),
                buildConsulterJournalAuditTool(),
                buildConsulterEtatSynchronisationsTool(),
                buildCreerMissionTool(),
                buildValiderCollecteTool(),
                buildRejeterCollecteTool(),
                buildGenererRapportOrganisationTool(),
                buildGenererRapportMissionTool(),
                buildChangerStatutMissionTool(),
                buildCreerUtilisateurTool(),
                buildSupprimerUtilisateurTool(),
                buildAffecterAgentSuperviseurTool(),
                buildExporterCollectesTool(),
                buildNaviguerPageTool()
        );
    }

    private OllamaTool buildListerMissionsTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("lister_missions")
                        .description("Liste les missions accessibles pour l'utilisateur connecté selon son rôle "
                                + "(ses missions assignées pour un agent, les missions de son organisation pour un superviseur ou admin).")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Collections.emptyMap()
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildConsulterAvancementMissionTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("consulter_avancement_mission")
                        .description("Consulte le taux de progression, les collectes reçues et les agents d'une mission spécifique par nom ou de l'ensemble des missions visibles.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "mission_name", Map.of(
                                                "type", "string",
                                                "description", "Nom ou intitulé partiel de la mission. Si non renseigné, renvoie l'avancement de toutes les missions."
                                        )
                                )
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildListerCollectesEnAttenteTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("lister_collectes_en_attente")
                        .description("Liste les collectes en attente de validation pour le superviseur. Strictement réservé aux superviseurs d'équipe.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Collections.emptyMap()
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildListerCollectesTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("lister_collectes")
                        .description("Liste les collectes enregistrées. Pour un agent, liste ses propres collectes. Pour un superviseur ou admin, liste les collectes supervisées avec filtre optionnel par mission.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "mission_name", Map.of(
                                                "type", "string",
                                                "description", "Nom optionnel de la mission pour filtrer les collectes."
                                        )
                                )
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildConsulterStatistiquesTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("consulter_statistiques")
                        .description("Consulte les indicateurs statistiques (taux de rejet, volume de collectes, temps moyen de validation). Interdit aux agents terrain.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "trend_days", Map.of(
                                                "type", "integer",
                                                "description", "Nombre de jours d'historique (ex: 7, 30, 90). Défaut à 30."
                                        )
                                )
                        ))
                        .build())
                .build();
    }


    private OllamaTool buildValiderCollecteTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("valider_collecte")
                        .description("Valide une collecte en attente de validation via son identifiant.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "collecte_id", Map.of(
                                                "type", "integer",
                                                "description", "Identifiant numérique de la collecte à valider."
                                        )
                                ),
                                "required", List.of("collecte_id")
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildRejeterCollecteTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("rejeter_collecte")
                        .description("Rejette une collecte en attente via son identifiant avec un motif obligatoire.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "collecte_id", Map.of(
                                                "type", "integer",
                                                "description", "Identifiant numérique de la collecte à rejeter."
                                        ),
                                        "motif", Map.of(
                                                "type", "string",
                                                "description", "Motif du rejet, obligatoire (ex: photo illisible, données incohérentes)."
                                        )
                                ),
                                "required", List.of("collecte_id", "motif")
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildGenererRapportOrganisationTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("generer_rapport_organisation")
                        .description("Génère le rapport PDF complet de l'organisation de l'utilisateur connecté.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Collections.emptyMap()
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildCreerUtilisateurTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("creer_utilisateur")
                        .description("Crée un nouvel utilisateur (Agent, Superviseur ou Admin Secondaire) pour l'organisation.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "first_name", Map.of("type", "string", "description", "Prénom de l'utilisateur."),
                                        "last_name", Map.of("type", "string", "description", "Nom de famille de l'utilisateur."),
                                        "email", Map.of("type", "string", "description", "Adresse email unique de l'utilisateur."),
                                        "role_type", Map.of("type", "string", "description", "Rôle attribué. Valeurs permises : AGENT, SUPERVISOR, ADMIN_SECONDAIRE."),
                                        "phone", Map.of("type", "string", "description", "Numéro de téléphone optionnel.")
                                ),
                                "required", List.of("first_name", "last_name", "email", "role_type")
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildSupprimerUtilisateurTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("supprimer_utilisateur")
                        .description("Désactive le compte d'un utilisateur de l'organisation.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "user_id", Map.of("type", "integer", "description", "Identifiant numérique de l'utilisateur à désactiver."),
                                        "email", Map.of("type", "string", "description", "Adresse email de l'utilisateur si son identifiant n'est pas connu."),
                                        "name", Map.of("type", "string", "description", "Nom ou prénom de l'utilisateur si son identifiant/email n'est pas connu.")
                                )
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildAffecterAgentSuperviseurTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("affecter_agent_superviseur")
                        .description("Affecte un agent de collecte à un superviseur au sein de l'organisation.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "agent_id", Map.of("type", "integer", "description", "Identifiant numérique de l'agent."),
                                        "agent_name_or_email", Map.of("type", "string", "description", "Nom ou email de l'agent si son identifiant n'est pas connu."),
                                        "supervisor_id", Map.of("type", "integer", "description", "Identifiant numérique du superviseur."),
                                        "supervisor_name_or_email", Map.of("type", "string", "description", "Nom ou email du superviseur si son identifiant n'est pas connu.")
                                )
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildListerUtilisateursTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("lister_utilisateurs")
                        .description("Recherche ou liste les utilisateurs de l'organisation avec filtre optionnel par rôle ou mot-clé.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "role", Map.of(
                                                "type", "string",
                                                "description", "Filtre optionnel par rôle : AGENT, SUPERVISOR, ADMIN_SECONDAIRE."
                                        ),
                                        "search", Map.of(
                                                "type", "string",
                                                "description", "Terme de recherche optionnel (nom, prénom ou email)."
                                        )
                                )
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildListerFormulairesTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("lister_formulaires")
                        .description("Liste les formulaires et questionnaires de collecte de l'organisation.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Collections.emptyMap()
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildConsulterJournalAuditTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("consulter_journal_audit")
                        .description("Consulte les dernières actions enregistrées dans le journal d'audit de l'organisation.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "limit", Map.of(
                                                "type", "integer",
                                                "description", "Nombre maximum d'événements récents à renvoyer (défaut : 5, max : 15)."
                                        )
                                )
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildConsulterEtatSynchronisationsTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("consulter_etat_synchronisations")
                        .description("Consulte l'état des dernières synchronisations mobiles et détecte d'éventuelles erreurs terrain.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Collections.emptyMap()
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildCreerMissionTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("creer_mission")
                        .description("Crée une nouvelle mission de collecte pour l'organisation.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "nom", Map.of(
                                                "type", "string",
                                                "description", "Nom ou intitulé de la mission (obligatoire)."
                                        ),
                                        "description", Map.of(
                                                "type", "string",
                                                "description", "Description ou contexte de la mission."
                                        ),
                                        "collectes_attendues", Map.of(
                                                "type", "integer",
                                                "description", "Nombre cible de collectes attendues."
                                        )
                                ),
                                "required", List.of("nom")
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildNaviguerPageTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("naviguer_page")
                        .description("Navigue vers un écran ou une page spécifique de l'application selon la demande de l'utilisateur.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "page", Map.of(
                                                "type", "string",
                                                "description", "Page demandée : 'dashboard' (ou 'accueil'), 'missions', 'collectes', 'users' (ou 'utilisateurs'), 'forms' (ou 'formulaires'), 'audit-logs' (ou 'audit'), 'sync-logs' (ou 'synchronisations'), 'profile' (ou 'profil')."
                                        )
                                ),
                                "required", List.of("page")
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildExporterCollectesTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("exporter_collectes")
                        .description("Exporte et télécharge les données de collecte au format Excel (.xlsx) ou CSV.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "format", Map.of(
                                                "type", "string",
                                                "description", "Format de fichier souhaité : 'excel' (défaut) ou 'csv'."
                                        )
                                )
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildGenererRapportMissionTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("generer_rapport_mission")
                        .description("Génère le rapport PDF complet d'une mission de collecte spécifique à partir de son nom ou identifiant.")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "mission_name", Map.of(
                                                "type", "string",
                                                "description", "Nom ou intitulé (même partiel) de la mission concernée."
                                        ),
                                        "mission_id", Map.of(
                                                "type", "integer",
                                                "description", "Identifiant numérique optionnel de la mission."
                                        )
                                ),
                                "required", List.of("mission_name")
                        ))
                        .build())
                .build();
    }

    private OllamaTool buildChangerStatutMissionTool() {
        return OllamaTool.builder()
                .type("function")
                .function(OllamaFunctionDefinition.builder()
                        .name("changer_statut_mission")
                        .description("Change le statut d'une mission de collecte : lancer (DRAFT vers ACTIVE), clôturer/terminer (ACTIVE vers COMPLETED) ou annuler (vers CANCELLED).")
                        .parameters(Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "mission_name", Map.of(
                                                "type", "string",
                                                "description", "Nom ou intitulé de la mission concernée."
                                        ),
                                        "action", Map.of(
                                                "type", "string",
                                                "description", "Action demandée : 'lancer' (ACTIVE), 'cloturer' (COMPLETED) ou 'annuler' (CANCELLED)."
                                        )
                                ),
                                "required", List.of("mission_name", "action")
                        ))
                        .build())
                .build();
    }
}
