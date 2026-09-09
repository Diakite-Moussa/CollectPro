package com.collectpro.backend.dto.assistant.ollama;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class OllamaMessage {
    private String role;    // "system" | "user" | "assistant" | "tool"
    private String content;

    @JsonProperty("tool_calls")
    private List<OllamaToolCall> toolCalls;

    public OllamaMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
