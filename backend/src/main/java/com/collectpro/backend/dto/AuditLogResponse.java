package com.collectpro.backend.dto;

import com.collectpro.backend.enums.AuditAction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private String entityType;
    private Long entityId;
    private String details;
    private LocalDateTime createdAt;
    private ActorSummary actor;

    @Getter
    @Builder
    public static class ActorSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
    }
}
