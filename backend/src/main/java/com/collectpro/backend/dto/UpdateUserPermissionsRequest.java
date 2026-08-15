package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateUserPermissionsRequest {

    @NotNull(message = "La liste des permissions est obligatoire")
    private List<String> permissionCodes;
}
