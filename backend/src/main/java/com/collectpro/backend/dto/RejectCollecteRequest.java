package com.collectpro.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectCollecteRequest {

    @NotBlank(message = "Un commentaire est obligatoire pour rejeter une collecte")
    private String comment;
}