package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignAgentsToMissionRequest {

    @NotEmpty(message = "La liste des agents à assigner est obligatoire")
    private List<Long> agentIds;
}