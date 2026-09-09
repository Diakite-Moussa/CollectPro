package com.collectpro.backend.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AssistantChatRequest {
    @NotBlank(message = "Le message ne peut pas être vide")
    private String message;

    /**
     * Chemin de confirmation EXPLICITE (bouton UI, tests Postman) uniquement.
     * Le flux vocal naturel n'a pas besoin de les renseigner : le backend retrouve
     * l'action en attente via l'utilisateur authentifié (voir AssistantConfirmationService).
     * Les deux restent optionnels (null lors d'un message normal).
     */
    private Boolean confirm;

    private String pendingActionId;

    /**
     * Historique récent de la conversation (optionnel) pour la mémoire multi-tours.
     */
    private java.util.List<HistoryMessage> history;

    @Getter @Setter
    public static class HistoryMessage {
        private String sender; // "user" ou "assistant"
        private String text;
    }
}