package com.collectpro.backend.service;

import com.collectpro.backend.entity.ActivationToken;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.repository.ActivationTokenRepository;
import com.collectpro.backend.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInvitationService {

    private final ActivationTokenRepository activationTokenRepository;
    private final TokenUtil tokenUtil;
    private final EmailService emailService;

    @Value("${app.activation-token.expiration-hours}")
    private long activationTokenExpirationHours;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendInvitation(User user) {
        String rawToken = tokenUtil.generateRawToken();
        ActivationToken activationToken = ActivationToken.builder()
                .user(user)
                .tokenHash(tokenUtil.hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(activationTokenExpirationHours))
                .build();
        activationTokenRepository.save(activationToken);

        String activationLink = frontendUrl + "/activate?token=" + rawToken;

        log.info("[INVITATION] Invitation envoyée à {} ({}). Lien d'activation : {}",
                user.getEmail(), user.getRole().getName(), activationLink);

        emailService.sendActivationEmail(user.getEmail(), user.getFirstName(), activationLink);
    }
}