package com.collectpro.backend.controller;

import com.collectpro.backend.dto.assistant.AssistantChatRequest;
import com.collectpro.backend.dto.assistant.AssistantChatResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.assistant.AssistantOrchestratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantOrchestratorService assistantOrchestratorService;

    @PostMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssistantChatResponse> chat(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AssistantChatRequest request) {
        return ResponseEntity.ok(assistantOrchestratorService.chat(principal.getUser(), request));
    }
}
