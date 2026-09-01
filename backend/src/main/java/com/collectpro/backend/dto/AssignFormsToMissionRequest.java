package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AssignFormsToMissionRequest {

    @NotEmpty(message = "La liste des formulaires à assigner est obligatoire")
    private List<Long> formIds;
}