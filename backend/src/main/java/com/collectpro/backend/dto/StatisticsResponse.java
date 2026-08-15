package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

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
}