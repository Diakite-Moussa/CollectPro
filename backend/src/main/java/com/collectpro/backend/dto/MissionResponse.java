package com.collectpro.backend.dto;

import com.collectpro.backend.enums.MissionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MissionResponse {
    private Long id;
    private String name;
    private String description;
    private Long organizationId;
    private MissionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Double latitude;
    private Double longitude;
    private Double radiusMeters;
    private Integer expectedCollectesCount;
    private CreatorSummary createdBy;
    private List<FormSummary> forms;
    private List<AgentSummary> agents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @Getter
    @Builder
    public static class CreatorSummary {
        private Long id;
        private String firstName;
        private String lastName;
    }

    @Getter
    @Builder
    public static class FormSummary {
        private Long id;
        private String name;
    }

    @Getter
    @Builder
    public static class AgentSummary {
        private Long id;
        private String firstName;
        private String lastName;
    }
}