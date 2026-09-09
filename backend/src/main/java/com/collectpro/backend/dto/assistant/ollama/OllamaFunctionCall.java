package com.collectpro.backend.dto.assistant.ollama;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaFunctionCall {
    private String name;
    private Map<String, Object> arguments;
}
