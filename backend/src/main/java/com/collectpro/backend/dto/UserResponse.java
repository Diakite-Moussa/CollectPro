package com.collectpro.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String status;
    private String role;
    private Long organizationId;
    private String organizationName;
    private LocalDateTime createdAt;
    /** Pour les agents : superviseur affecté (RB-ORG-09). */
    private Long assignedSupervisorId;
    private String assignedSupervisorName;
}