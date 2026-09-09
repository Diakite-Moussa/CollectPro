package com.collectpro.backend.dto.assistant.ollama;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OllamaChatRequest {
    private String model;
    private List<OllamaMessage> messages;
    private List<OllamaTool> tools;
    private boolean stream; // toujours false en V1 — pas de streaming HTTP chunké ce sprint

    @Builder.Default
    private Boolean think = false; // désactive le raisonnement <think> de Qwen3 : gain de latence important, réponses/tool_calls plus directs

    private java.util.Map<String, Object> options;
}
