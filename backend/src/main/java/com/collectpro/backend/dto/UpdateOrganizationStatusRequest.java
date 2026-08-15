package com.collectpro.backend.dto;

import com.collectpro.backend.enums.OrganizationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrganizationStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private OrganizationStatus status;
}