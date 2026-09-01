package com.collectpro.backend.dto;

import com.collectpro.backend.enums.CollecteStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CollecteResponse {
    private Long id;
    private AgentSummary agent;
    private Long formVersionId;
    private String dataJson;
    private List<CollecteAttachmentResponse> attachments;
    private Double latitude;
    private Double longitude;
    private CollecteStatus status;
    private String validationComment;
    private AgentSummary validatedBy;
    private LocalDateTime validatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long missionId;
    private Boolean outsideMissionZone;

    @Getter
    @Builder
    public static class AgentSummary {
        private Long id;
        private String firstName;
        private String lastName;
    }
}