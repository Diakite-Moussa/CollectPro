package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignSupervisorRequest {

    @NotNull(message = "L'id de l'agent est obligatoire")
    private Long agentId;

    @NotNull(message = "L'id du superviseur est obligatoire")
    private Long supervisorId;
}