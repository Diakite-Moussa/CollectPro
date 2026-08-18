package com.collectpro.backend.dto;

import com.collectpro.backend.enums.SyncResult;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SyncLogResponse {
    private Long id;
    private AgentSummary agent;   // nouveau
    private String localReference;
    private Long collecteId;
    private SyncResult result;
    private String errorMessage;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class AgentSummary {
        private Long id;
        private String firstName;
        private String lastName;
    }
}
