package com.collectpro.backend.config;

import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.repository.RoleRepository;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(3) // après DataInitializer (rôles) et PermissionDataInitializer (permissions)
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.email}")
    private String superAdminEmail;

    @Value("${app.super-admin.password}")
    private String superAdminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.findByEmail(superAdminEmail).isPresent()) {
            log.info("Compte SUPER_ADMIN déjà initialisé ({})", superAdminEmail);
            return;
        }

        Role superAdminRole = roleRepository.findByName(RoleType.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                        "Rôle SUPER_ADMIN introuvable — DataInitializer n'a pas tourné"));

        User superAdmin = User.builder()
                .firstName("Super")
                .lastName("Admin")
                .email(superAdminEmail)
                .password(passwordEncoder.encode(superAdminPassword))
                .status(UserStatus.ACTIVE)
                .role(superAdminRole)
                .organization(null) // le SUPER_ADMIN opère au niveau plateforme, hors organisation
                .build();

        userRepository.save(superAdmin);
        log.warn("Compte SUPER_ADMIN créé ({}). Changez le mot de passe par défaut si non fourni via variable d'environnement.", superAdminEmail);
    }
}