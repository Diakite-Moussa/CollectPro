package com.collectpro.backend.service.assistant;

import com.collectpro.backend.dto.assistant.ollama.OllamaChatRequest;
import com.collectpro.backend.dto.assistant.ollama.OllamaChatResponse;
import com.collectpro.backend.dto.assistant.ollama.OllamaMessage;
import com.collectpro.backend.dto.assistant.ollama.OllamaTool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OllamaClient {

    private final RestClient ollamaRestClient;

    @Value("${app.ollama.model}")
    private String model;

    public OllamaMessage chat(List<OllamaMessage> messages) {
        return chat(messages, null);
    }

    public OllamaMessage chat(List<OllamaMessage> messages, List<OllamaTool> tools) {
        OllamaChatRequest request = OllamaChatRequest.builder()
                .model(model)
                .messages(messages)
                .tools(tools)
                .stream(false)
                .options(java.util.Map.of("num_ctx", 8192))
                .build();

        OllamaChatResponse response = ollamaRestClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);

        if (response == null || response.getMessage() == null) {
            throw new IllegalStateException("Réponse vide d'Ollama");
        }
        return response.getMessage();
    }
}