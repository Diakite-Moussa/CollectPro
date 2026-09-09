package com.collectpro.backend.dto.assistant.ollama;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaTool {
    @Builder.Default
    private String type = "function";
    private OllamaFunctionDefinition function;
}
