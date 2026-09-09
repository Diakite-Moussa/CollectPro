package com.collectpro.backend.dto.assistant.ollama;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class OllamaChatResponse {
    private String model;
    private OllamaMessage message;
    private boolean done;
}
