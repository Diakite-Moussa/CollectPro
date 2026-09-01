package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionProgressResponse {
    private Long missionId;
    private String missionName;
    private Integer expectedCollectesCount;   // null si non défini sur la mission
    private Long receivedCollectesCount;
    private Double progressPercent;           // null si expectedCollectesCount non défini
    private Long activeAgentsCount;           // agents ayant soumis ≥1 collecte
    private Long assignedAgentsCount;         // agents assignés à la mission
}
