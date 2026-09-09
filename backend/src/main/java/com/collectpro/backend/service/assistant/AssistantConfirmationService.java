package com.collectpro.backend.service.assistant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AssistantConfirmationService {

    private static final long TTL_SECONDS = 60;

    /** Une seule action en attente par utilisateur à la fois (clé = userId). */
    private final Map<Long, PendingAction> pendingByUser = new ConcurrentHashMap<>();

    public record PendingAction(
            String id,
            Long ownerUserId,
            String toolName,
            Map<String, Object> arguments,
            String humanSummary,
            LocalDateTime expiresAt
    ) {
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    /** Enregistre une nouvelle action en attente, écrase toute action précédente de cet utilisateur. */
    public PendingAction create(Long userId, String toolName, Map<String, Object> arguments, String humanSummary) {
        PendingAction action = new PendingAction(
                UUID.randomUUID().toString(),
                userId,
                toolName,
                arguments,
                humanSummary,
                LocalDateTime.now().plusSeconds(TTL_SECONDS)
        );
        pendingByUser.put(userId, action);
        log.info("Action IA en attente de confirmation créée pour l'utilisateur {} : {}", userId, humanSummary);
        return action;
    }

    /** Retourne l'action en attente valide de cet utilisateur, ou null si absente/expirée. */
    public PendingAction getValidPending(Long userId) {
        PendingAction action = pendingByUser.get(userId);
        if (action == null) {
            return null;
        }
        if (action.isExpired()) {
            pendingByUser.remove(userId);
            return null;
        }
        return action;
    }

    /** Vérifie qu'un pendingActionId explicite correspond bien à l'action en attente de cet utilisateur. */
    public boolean matches(Long userId, String pendingActionId) {
        PendingAction action = getValidPending(userId);
        return action != null && action.id().equals(pendingActionId);
    }

    /** Supprime l'action en attente (après exécution ou annulation). Usage unique garanti. */
    public void clear(Long userId) {
        pendingByUser.remove(userId);
    }
}