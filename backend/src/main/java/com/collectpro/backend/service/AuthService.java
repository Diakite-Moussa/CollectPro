package com.collectpro.backend.service;

import com.collectpro.backend.dto.*;
import com.collectpro.backend.entity.ActivationToken;
import com.collectpro.backend.entity.RefreshToken;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.InvalidCredentialsException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.ActivationTokenRepository;
import com.collectpro.backend.repository.RefreshTokenRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.security.JwtService;
import com.collectpro.backend.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.collectpro.backend.entity.PasswordResetToken;
import com.collectpro.backend.repository.PasswordResetTokenRepository;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenUtil tokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.refresh-token.expiration-days}")
    private long refreshTokenExpirationDays;

    @Value("${app.password-reset-token.expiration-hours}")
    private long passwordResetTokenExpirationHours;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final Set<RoleType> WEB_ONLY_ROLES = Set.of(
            RoleType.SUPER_ADMIN, RoleType.ADMIN_PRINCIPAL, RoleType.ADMIN_SECONDAIRE);
    private static final Set<RoleType> MOBILE_ONLY_ROLES = Set.of(RoleType.AGENT);
    // SUPERVISOR : acces autorise sur les deux plateformes, aucune restriction.

    private void assertUserAndOrganizationActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Ce compte n'est pas actif");
        }
        if (user.getOrganization() != null && user.getOrganization().getStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessRuleException("L'organisation de ce compte est désactivée");
        }
    }

    /**
     * Restreint la connexion selon le role de l'utilisateur et la plateforme
     * d'origine (header X-Client-Platform : "WEB" ou "MOBILE", envoye
     * automatiquement par le dashboard et l'app mobile). Si le header est
     * absent (client non a jour ou appel hors navigateur/app), aucune
     * restriction n'est appliquee.
     */
    private void enforcePlatformAccess(User user, String platform) {
        if (platform == null || platform.isBlank()) {
            return;
        }
        String normalized = platform.trim().toUpperCase();
        RoleType role = user.getRole().getName();

        if (WEB_ONLY_ROLES.contains(role) && "MOBILE".equals(normalized)) {
            throw new BusinessRuleException(
                    "Les comptes administrateurs ne peuvent pas se connecter depuis l'application mobile. Utilisez le dashboard web.");
        }
        if (MOBILE_ONLY_ROLES.contains(role) && "WEB".equals(normalized)) {
            throw new BusinessRuleException(
                    "Les comptes agents ne peuvent pas se connecter au dashboard web. Utilisez l'application mobile.");
        }
    }

    public ActivationTokenStatusResponse checkActivationToken(String rawToken) {

        String tokenHash = tokenUtil.hashToken(rawToken);

        ActivationToken activationToken = activationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Token d'activation invalide"));

        if (!activationToken.isValid()) {
            throw new BusinessRuleException("Ce token d'activation est expiré ou a déjà été utilisé");
        }

        User user = activationToken.getUser();

        return ActivationTokenStatusResponse.builder()
                .valid(true)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .build();
    }

    @Transactional
    public UserResponse activateAccount(ActivateAccountRequest request) {

        String tokenHash = tokenUtil.hashToken(request.getToken());

        ActivationToken activationToken = activationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Token d'activation invalide"));

        if (!activationToken.isValid()) {
            throw new BusinessRuleException("Ce token d'activation est expiré ou a déjà été utilisé");
        }

        User user = activationToken.getUser();

        if (user.getStatus() != UserStatus.INVITED) {
            throw new BusinessRuleException("Ce compte n'est pas en attente d'activation");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        activationToken.setUsedAt(LocalDateTime.now());
        activationTokenRepository.save(activationToken);

        auditLogService.log(
                user,
                user.getOrganization(),
                AuditAction.ACCOUNT_ACTIVATED,
                "User",
                user.getId(),
                "Activation du compte " + user.getEmail()
        );

        return toUserResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientPlatform) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Email ou mot de passe incorrect"));

        assertUserAndOrganizationActive(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Email ou mot de passe incorrect");
        }

        enforcePlatformAccess(user, clientPlatform);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String clientPlatform) {

        String tokenHash = tokenUtil.hashToken(request.getRefreshToken());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token invalide"));

        if (!refreshToken.isValid()) {
            throw new InvalidCredentialsException("Refresh token expiré ou révoqué");
        }

        assertUserAndOrganizationActive(refreshToken.getUser());

        enforcePlatformAccess(refreshToken.getUser(), clientPlatform);

        // Rotation : on révoque l'ancien refresh token
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(refreshToken.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = tokenUtil.hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            // On ne crée le token que si le compte est actif — mais on ne le dit jamais
            // à l'appelant (même réponse générique côté controller dans tous les cas,
            // pour ne pas révéler si un email existe en base).
            if (user.getStatus() != UserStatus.ACTIVE) {
                return;
            }

            String rawToken = tokenUtil.generateRawToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenUtil.hashToken(rawToken))
                    .expiresAt(LocalDateTime.now().plusHours(passwordResetTokenExpirationHours))
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);

            auditLogService.log(
                    user,
                    user.getOrganization(),
                    AuditAction.PASSWORD_RESET_REQUESTED,
                    "User",
                    user.getId(),
                    "Demande de réinitialisation de mot de passe pour " + user.getEmail()
            );
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = tokenUtil.hashToken(request.getToken());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Token de réinitialisation invalide"));

        if (!resetToken.isValid()) {
            throw new BusinessRuleException("Ce lien de réinitialisation est expiré ou a déjà été utilisé");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        auditLogService.log(
                user,
                user.getOrganization(),
                AuditAction.PASSWORD_RESET_COMPLETED,
                "User",
                user.getId(),
                "Mot de passe réinitialisé pour " + user.getEmail()
        );
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user.getEmail());

        String rawRefreshToken = tokenUtil.generateRawToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenUtil.hashToken(rawRefreshToken))
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(jwtExpiration / 1000) // en secondes
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .role(user.getRole().getName().name())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}