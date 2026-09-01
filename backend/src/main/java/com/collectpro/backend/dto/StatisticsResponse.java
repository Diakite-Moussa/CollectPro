package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class StatisticsResponse {

    private String scope; // "GLOBAL", "ORGANIZATION", "TEAM"

    // Contexte (null en scope GLOBAL)
    private Long organizationId;
    private String organizationName;

    // Organisations (uniquement rempli en scope GLOBAL)
    private Long totalOrganizations;
    private Long activeOrganizations;

    // Utilisateurs
    private Long totalUsers;
    private Long totalAgents;
    private Long totalSupervisors;
    private Long totalAdmins;

    // Formulaires
    private Long totalForms;
    private Long publishedForms;

    // Collectes
    private Long totalCollectes;
    private Long pendingCollectes;
    private Long validatedCollectes;
    private Long rejectedCollectes;

    // ---- Sprint 4 — Statistiques avancées ----

    /** Évolution du nombre de collectes reçues, jour par jour, sur la fenêtre demandée. */
    private List<DailyCountEntry> collecteTrend;

    /** Taux de rejet par agent (uniquement les agents ayant au moins une collecte). */
    private List<AgentRejectionRate> rejectionByAgent;

    /** Temps moyen entre soumission et validation, en heures. Null si aucune collecte validée. */
    private Double avgValidationTimeHours;

    @Getter
    @Builder
    public static class DailyCountEntry {
        private LocalDate date;
        private Long count;
    }

    @Getter
    @Builder
    public static class AgentRejectionRate {
        private Long agentId;
        private String firstName;
        private String lastName;
        private Long totalCount;
        private Long rejectedCount;
        private Double rejectionRatePercent;
    }
}