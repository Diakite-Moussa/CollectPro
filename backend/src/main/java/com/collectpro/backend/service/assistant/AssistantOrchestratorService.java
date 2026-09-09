package com.collectpro.backend.service.assistant;

import com.collectpro.backend.dto.assistant.AssistantChatRequest;
import com.collectpro.backend.dto.assistant.AssistantChatResponse;
import com.collectpro.backend.dto.assistant.ollama.OllamaMessage;
import com.collectpro.backend.dto.assistant.ollama.OllamaTool;
import com.collectpro.backend.dto.assistant.ollama.OllamaToolCall;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssistantOrchestratorService {

    private final OllamaClient ollamaClient;
    private final AssistantToolRegistry toolRegistry;
    private final AssistantToolExecutor toolExecutor;
    private final AssistantConfirmationService confirmationService;
    private final AuditLogService auditLogService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    @Value("${app.assistant.enable-history:${assistant.enable-history:false}}")
    @Setter
    private boolean enableHistory = false;

    public AssistantChatResponse chat(User principal, AssistantChatRequest request) {
        try {
            AssistantConfirmationService.PendingAction pending = confirmationService.getValidPending(principal.getId());

            // Chemin explicite (bouton UI / tests) : confirm + pendingActionId fournis directement.
            if (pending != null && Boolean.TRUE.equals(request.getConfirm())) {
                if (request.getPendingActionId() != null && !confirmationService.matches(principal.getId(), request.getPendingActionId())) {
                    return AssistantChatResponse.builder()
                            .reply("Cette action a expiré ou n'existe plus. Reformulez votre demande.")
                            .build();
                }
                return executeConfirmedAction(principal, pending);
            }
            if (pending != null && Boolean.FALSE.equals(request.getConfirm())) {
                confirmationService.clear(principal.getId());
                return AssistantChatResponse.builder().reply("Action annulée.").build();
            }

            // Chemin vocal naturel : une action est en attente, on classifie l'intention via Ollama (sans tools).
            if (pending != null) {
                return handlePendingConfirmationViaNaturalLanguage(principal, request, pending);
            }

            // Flux normal (aucune confirmation en attente).
            return runNormalFlow(principal, request);
        } catch (Exception e) {
            log.error("Erreur lors du traitement du chat IA pour l'utilisateur '{}' : ",
                    (principal != null ? principal.getEmail() : "inconnu"), e);
            String detail = (e.getMessage() != null && !e.getMessage().isBlank()) ? e.getMessage() : "Erreur de connexion au service IA";
            if (detail.contains("400") || detail.contains("Bad Request")) {
                detail = "Requête invalide ou contexte dépassé";
            } else if (detail.contains("Timeout") || detail.contains("timed out")) {
                detail = "Délai d'attente dépassé pour le service IA";
            } else if (detail.contains("Connection refused") || detail.contains("ConnectException")) {
                detail = "Serveur Ollama inaccessible";
            }
            return AssistantChatResponse.builder()
                    .reply("Désolé, une erreur est survenue lors de l'appel au service IA (" + detail + "). Vérifiez que le service Ollama est bien accessible et configuré.")
                    .build();
        }
    }

    private AssistantChatResponse handlePendingConfirmationViaNaturalLanguage(
            User principal, AssistantChatRequest request, AssistantConfirmationService.PendingAction pending) {

        String classificationPrompt = "Une action est en attente de confirmation : \"" + pending.humanSummary() + "\".\n"
                + "L'utilisateur vient de répondre : \"" + request.getMessage() + "\".\n"
                + "Détermine son intention. Réponds STRICTEMENT avec un objet JSON, sans aucun autre texte : "
                + "{\"intent\": \"CONFIRM\"} ou {\"intent\": \"CANCEL\"} ou {\"intent\": \"UNCLEAR\"}";

        List<OllamaMessage> messages = List.of(
                new OllamaMessage("system", "Tu es un classificateur d'intention. Tu réponds uniquement en JSON strict, jamais en texte libre."),
                new OllamaMessage("user", classificationPrompt)
        );

        String intent;
        try {
            OllamaMessage response = ollamaClient.chat(messages, null);
            intent = extractIntent(stripThinking(response.getContent()));
        } catch (Exception e) {
            log.warn("Échec de la classification de confirmation, traité comme UNCLEAR", e);
            intent = "UNCLEAR";
        }

        return switch (intent) {
            case "CONFIRM" -> executeConfirmedAction(principal, pending);
            case "CANCEL" -> {
                confirmationService.clear(principal.getId());
                yield AssistantChatResponse.builder().reply("Action annulée.").build();
            }
            default -> runNormalFlow(principal, request); // UNCLEAR : le pending reste actif jusqu'au TTL
        };
    }

    private String extractIntent(String content) {
        if (content == null) return "UNCLEAR";
        String upper = content.toUpperCase();
        if (upper.contains("\"CONFIRM\"")) return "CONFIRM";
        if (upper.contains("\"CANCEL\"")) return "CANCEL";
        return "UNCLEAR";
    }

    private AssistantChatResponse executeConfirmedAction(User principal, AssistantConfirmationService.PendingAction pending) {
        String resultJson = toolExecutor.executeConfirmed(pending, principal);
        confirmationService.clear(principal.getId());

        auditLogService.log(
                principal,
                principal.getOrganization(),
                AuditAction.ASSISTANT_ACTION_EXECUTED,
                "AssistantAction",
                null,
                "Action IA confirmée et exécutée : " + pending.humanSummary()
        );

        Long reportId = null;
        String extra = "";
        if (("generer_rapport_organisation".equals(pending.toolName()) || "generer_rapport_mission".equals(pending.toolName())) && resultJson != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(resultJson);
                if (node.has("rapport_id")) {
                    reportId = node.get("rapport_id").asLong();
                    extra = " Le rapport #" + reportId + " est prêt. Vous pouvez le télécharger directement ci-dessous ou le retrouver dans la section 'Rapports PDF' du tableau de bord.";
                }
            } catch (Exception ignored) {
            }
        } else if ("creer_mission".equals(pending.toolName()) && resultJson != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(resultJson);
                if (node.has("mission_id")) {
                    extra = " La mission a été enregistrée en statut brouillon (ID #" + node.get("mission_id").asLong() + ").";
                }
            } catch (Exception ignored) {
            }
        } else if ("changer_statut_mission".equals(pending.toolName()) && resultJson != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(resultJson);
                if (node.has("nouveau_statut")) {
                    extra = " Nouveau statut de la mission : " + node.get("nouveau_statut").asText() + ".";
                }
            } catch (Exception ignored) {
            }
        }

        return AssistantChatResponse.builder()
                .reply("C'est fait : " + pending.humanSummary() + "." + extra)
                .reportId(reportId)
                .build();
    }

    private AssistantChatResponse runNormalFlow(User principal, AssistantChatRequest request) {
        String systemPrompt = buildSystemPrompt(principal);

        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(new OllamaMessage("system", systemPrompt));

        if (enableHistory && request.getHistory() != null && !request.getHistory().isEmpty()) {
            int start = Math.max(0, request.getHistory().size() - 6);
            for (int i = start; i < request.getHistory().size(); i++) {
                AssistantChatRequest.HistoryMessage item = request.getHistory().get(i);
                if (item != null && item.getText() != null && !item.getText().isBlank()) {
                    String role = "user".equalsIgnoreCase(item.getSender()) ? "user" : "assistant";
                    messages.add(new OllamaMessage(role, item.getText()));
                }
            }
        }

        messages.add(new OllamaMessage("user", request.getMessage()));

        com.collectpro.backend.enums.RoleType role = (principal != null && principal.getRole() != null) ? principal.getRole().getName() : null;
        List<OllamaTool> tools = toolRegistry.getToolsForRole(role);
        log.info("Envoi du prompt utilisateur à Ollama avec {} tools disponibles (rôle: {})", tools.size(), role);
        OllamaMessage response = ollamaClient.chat(messages, tools);

        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            log.info("Ollama a déclenché {} tool call(s)", response.getToolCalls().size());
            messages.add(response);

            String navigateTo = null;
            String exportFormat = null;

            for (OllamaToolCall toolCall : response.getToolCalls()) {
                if (toolCall.getFunction() != null) {
                    String toolName = toolCall.getFunction().getName();
                    Map<String, Object> arguments = toolCall.getFunction().getArguments();
                    String toolResult = toolExecutor.execute(toolName, arguments, principal);

                    if ("naviguer_page".equals(toolName) && toolResult != null) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(toolResult);
                            if (node.has("route")) {
                                navigateTo = node.get("route").asText();
                            }
                        } catch (Exception ignored) {}
                    } else if ("exporter_collectes".equals(toolName) && toolResult != null) {
                        try {
                            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(toolResult);
                            if (node.has("format")) {
                                exportFormat = node.get("format").asText();
                            }
                        } catch (Exception ignored) {}
                    }

                    messages.add(OllamaMessage.builder()
                            .role("tool")
                            .content(toolResult)
                            .build());
                }
            }

            OllamaMessage finalReply = ollamaClient.chat(messages, null);
            AssistantConfirmationService.PendingAction newlyCreated = confirmationService.getValidPending(principal.getId());
            return AssistantChatResponse.builder()
                    .reply(stripThinking(finalReply.getContent()))
                    .requiresConfirmation(newlyCreated != null)
                    .pendingActionId(newlyCreated != null ? newlyCreated.id() : null)
                    .navigateTo(navigateTo)
                    .exportFormat(exportFormat)
                    .build();
        }

        return AssistantChatResponse.builder()
                .reply(stripThinking(response.getContent()))
                .build();
    }

    private String stripThinking(String content) {
        if (content == null) return null;
        int closeIndex = content.lastIndexOf("</think>");
        String cleaned = (closeIndex == -1) ? content : content.substring(closeIndex + "</think>".length());
        cleaned = cleaned.replaceAll("<\\|[a-zA-Z0-9_:]+\\|>", "").trim();
        if (cleaned.isEmpty()) {
            log.warn("Réponse Ollama vide après nettoyage");
            return "Désolé, je n'ai pas pu formuler de réponse claire. Pouvez-vous reformuler votre demande ?";
        }
        return cleaned;
    }

    private String buildSystemPrompt(User principal) {
        String roleName = principal.getRole() != null ? principal.getRole().getName().name() : "UTILISATEUR";
        String orgName = principal.getOrganization() != null ? principal.getOrganization().getName() : "Plateforme globale";
        String userName = ((principal.getFirstName() != null ? principal.getFirstName() : "")
                + " " + (principal.getLastName() != null ? principal.getLastName() : "")).trim();
        if (userName.isBlank()) {
            userName = principal.getEmail();
        }

        return "Tu es l'assistant vocal intelligent de CollectPro. Tu t'adresses à "
                + userName + ", dont le rôle est " + roleName + " au sein de l'organisation '" + orgName + "'.\n"
                + "Consignes impératives :\n"
                + "- Réponds toujours en français.\n"
                + "- Tes réponses doivent être claires, concises et parfaitement adaptées à une synthèse vocale (Text-to-Speech).\n"
                + "- Dès que l'utilisateur demande une action ou opération (ex: générer un rapport, valider ou rejeter une collecte, créer ou désactiver un compte, affecter un agent) ou des informations (missions, collectes, avancement, statistiques), appelle TOUJOURS et IMMÉDIATEMENT la fonction/outil approprié via un tool call.\n"
                + "- Ne demande JAMAIS de confirmation toi-même dans ta réponse textuelle avant d'avoir appelé l'outil. C'est l'outil qui prépare l'action et enregistre la demande de confirmation.\n"
                + "- Si le résultat d'un outil indique un refus d'accès (rôle ou permission insuffisante), explique-le poliment sans jargon technique.\n"
                + "- Si le résultat d'un outil indique pending_confirmation=true, résume clairement à l'utilisateur ce qui va être fait et demande-lui s'il confirme (oui/non).\n"
                + "- Ne divulgue aucune donnée fictive et n'invente jamais de chiffres ou d'identifiants non fournis par les outils.";
    }
}