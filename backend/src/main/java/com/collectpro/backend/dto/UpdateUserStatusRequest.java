package com.collectpro.backend.dto;

import com.collectpro.backend.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusRequest {

    @NotNull(message = "Le statut est obligatoire")
    private UserStatus status;
}
