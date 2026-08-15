package com.collectpro.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateCollecteRequest {

    /** Commentaire optionnel du superviseur à la validation. */
    private String comment;
}