package com.collectpro.backend.controller;

import com.collectpro.backend.dto.FormVersionResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.FormVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/form-versions")
@RequiredArgsConstructor
public class FormVersionLookupController {

    private final FormVersionService formVersionService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FormVersionResponse> getVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(formVersionService.getVersionResponse(id, principal.getUser()));
    }
}