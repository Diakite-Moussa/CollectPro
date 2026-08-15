package com.collectpro.backend.dto;

import com.collectpro.backend.enums.SyncResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSyncLogRequest {

    @NotBlank(message = "La référence locale est obligatoire")
    private String localReference;

    private Long collecteId;

    @NotNull(message = "Le résultat est obligatoire")
    private SyncResult result;

    private String errorMessage;
}
