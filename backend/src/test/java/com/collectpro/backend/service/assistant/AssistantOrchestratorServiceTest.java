package com.collectpro.backend.service.assistant;

import com.collectpro.backend.dto.assistant.AssistantChatRequest;
import com.collectpro.backend.dto.assistant.AssistantChatResponse;
import com.collectpro.backend.dto.assistant.ollama.OllamaFunctionCall;
import com.collectpro.backend.dto.assistant.ollama.OllamaMessage;
import com.collectpro.backend.dto.assistant.ollama.OllamaTool;
import com.collectpro.backend.dto.assistant.ollama.OllamaToolCall;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantOrchestratorServiceTest {

    @Mock private OllamaClient ollamaClient;
    @Mock private AssistantToolRegistry toolRegistry;
    @Mock private AssistantToolExecutor toolExecutor;
    @Mock private AssistantConfirmationService confirmationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private AssistantOrchestratorService orchestratorService;

    private User supervisor;

    @BeforeEach
    void setUp() {
        Organization org = Organization.builder()
                .id(1L)
                .name("Santé Plus")
                .build();

        supervisor = User.builder()
                .id(10L)
                .email("sup@santeplus.org")
                .firstName("Fatou")
                .lastName("Ndiaye")
                .role(Role.builder().name(RoleType.SUPERVISOR).build())
                .organization(org)
                .build();
    }

    @Test
    @DisplayName("Réponse directe sans tool call")
    void testChatSansToolCall() {
        when(toolRegistry.getToolsForRole(any())).thenReturn(Collections.emptyList());

        OllamaMessage directReply = new OllamaMessage("assistant", "Bonjour Fatou, comment puis-je vous aider ?");
        when(ollamaClient.chat(anyList(), anyList())).thenReturn(directReply);

        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage("Bonjour");

        AssistantChatResponse response = orchestratorService.chat(supervisor, request);

        assertNotNull(response);
        assertEquals("Bonjour Fatou, comment puis-je vous aider ?", response.getReply());
    }

    @Test
    @DisplayName("Boucle complète de Tool Calling avec exécution et synthèse finale")
    void testChatAvecToolCall() {
        OllamaTool sampleTool = OllamaTool.builder().type("function").build();
        when(toolRegistry.getToolsForRole(any())).thenReturn(List.of(sampleTool));

        // Premier appel : Ollama demande un tool call
        OllamaToolCall call = OllamaToolCall.builder()
                .function(OllamaFunctionCall.builder()
                        .name("lister_missions")
                        .arguments(Map.of())
                        .build())
                .build();

        OllamaMessage toolCallMessage = OllamaMessage.builder()
                .role("assistant")
                .content("")
                .toolCalls(List.of(call))
                .build();

        when(ollamaClient.chat(anyList(), anyList())).thenReturn(toolCallMessage);

        // Exécution du tool
        when(toolExecutor.execute(eq("lister_missions"), anyMap(), eq(supervisor)))
                .thenReturn("{\"total_missions\": 1, \"missions\": [{\"nom\": \"Vaccination 2026\"}]}");

        // Second appel : synthèse finale
        OllamaMessage finalSynthesis = new OllamaMessage("assistant", "Vous avez 1 mission en cours : Vaccination 2026.");
        when(ollamaClient.chat(anyList(), isNull())).thenReturn(finalSynthesis);

        AssistantChatRequest request = new AssistantChatRequest();
        request.setMessage("Quelles sont mes missions ?");

        AssistantChatResponse response = orchestratorService.chat(supervisor, request);

        assertNotNull(response);
        assertEquals("Vous avez 1 mission en cours : Vaccination 2026.", response.getReply());

        verify(toolExecutor).execute(eq("lister_missions"), anyMap(), eq(supervisor));
        verify(ollamaClient).chat(anyList(), isNull());
    }
}
