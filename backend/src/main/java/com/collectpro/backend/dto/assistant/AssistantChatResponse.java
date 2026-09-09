package com.collectpro.backend.dto.assistant;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class AssistantChatResponse {
    private String reply;

    /** true uniquement quand une action sensible attend une confirmation. */
    @Builder.Default
    private boolean requiresConfirmation = false;

    /** Présent uniquement quand requiresConfirmation = true. Sert au chemin de confirmation explicite (UI). */
    private String pendingActionId;

    /** ID d'un rapport généré, permettant le téléchargement direct côté client. */
    private Long reportId;

    /** Route de navigation dans l'application (ex: "/collectes"). */
    private String navigateTo;

    /** Format d'export de données demandé ("excel" ou "csv"). */
    private String exportFormat;
}