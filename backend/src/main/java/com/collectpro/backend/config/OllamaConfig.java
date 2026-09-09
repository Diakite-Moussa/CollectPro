package com.collectpro.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OllamaConfig {

    @Value("${app.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${app.ollama.timeout-seconds}")
    private int timeoutSeconds;

    @Value("${app.ollama.api-key:}")
    private String ollamaApiKey;

    @Bean
    public RestClient ollamaRestClient(RestClient.Builder builder) {
        String cleanBaseUrl = normalizeBaseUrl(ollamaBaseUrl);

        RestClient.Builder configured = builder
                .baseUrl(cleanBaseUrl)
                .requestFactory(clientHttpRequestFactory());

        // Ajouté uniquement si une clé est fournie — garde la compatibilité avec Ollama local (pas de clé requise)
        if (ollamaApiKey != null && !ollamaApiKey.isBlank()) {
            configured = configured.defaultHeader("Authorization", "Bearer " + ollamaApiKey.trim());
        }

        return configured.build();
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:11434";
        }
        String clean = url.trim();
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (clean.endsWith("/api")) {
            clean = clean.substring(0, clean.length() - 4);
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }

    private org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return factory;
    }
}