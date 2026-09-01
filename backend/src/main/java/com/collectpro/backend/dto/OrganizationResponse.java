package com.collectpro.backend.dto;

import com.collectpro.backend.enums.OrganizationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrganizationResponse {
    private Long id;
    private String name;
    private String description;
    private OrganizationStatus status;
    private LocalDateTime createdAt;
    private String logoUrl; // ← ajouté
    private UserSummary principalAdmin;

    @Getter
    @Builder
    public static class UserSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String status;
    }


}