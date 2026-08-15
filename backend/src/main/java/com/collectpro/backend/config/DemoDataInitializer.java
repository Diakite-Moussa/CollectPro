package com.collectpro.backend.config;

import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.FormVersion;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.SupervisorAgent;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.FormStatus;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.repository.FormRepository;
import com.collectpro.backend.repository.FormVersionRepository;
import com.collectpro.backend.repository.OrganizationRepository;
import com.collectpro.backend.repository.RoleRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(4) // après SuperAdminInitializer
@Profile("dev")
public class DemoDataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FormRepository formRepository;
    private final FormVersionRepository formVersionRepository;
    private final SupervisorAgentRepository supervisorAgentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. Démo Organisation par défaut
        Organization demoOrg = organizationRepository.findByName("Organisation Démo CollectPro")
                .orElseGet(() -> organizationRepository.save(Organization.builder()
                        .name("Organisation Démo CollectPro")
                        .description("Organisation par défaut pour les tests de collecte")
                        .status(OrganizationStatus.ACTIVE)
                        .build()));

        // 2. Démo Agent (agent2@test.com)
        Role agentRole = roleRepository.findByName(RoleType.AGENT)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.AGENT).build()));

        User demoAgent = userRepository.findByEmail("agent2@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName("Aïcha")
                        .lastName("Diallo")
                        .email("agent2@test.com")
                        .password(passwordEncoder.encode("MotDePasse123!"))
                        .status(UserStatus.ACTIVE)
                        .role(agentRole)
                        .organization(demoOrg)
                        .build()));

        // 2b. Démo Superviseur (supervisor@test.com) — pour tester RF-012
        Role supervisorRole = roleRepository.findByName(RoleType.SUPERVISOR)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.SUPERVISOR).build()));

        User demoSupervisor = userRepository.findByEmail("supervisor@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName("Moussa")
                        .lastName("Koné")
                        .email("supervisor@test.com")
                        .password(passwordEncoder.encode("MotDePasse123!"))
                        .status(UserStatus.ACTIVE)
                        .role(supervisorRole)
                        .organization(demoOrg)
                        .build()));

        // 2c. Démo Admin principal (admin@test.com) — pour le dashboard
        Role adminPrincipalRole = roleRepository.findByName(RoleType.ADMIN_PRINCIPAL)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleType.ADMIN_PRINCIPAL).build()));

        userRepository.findByEmail("admin@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .firstName("Fatou")
                        .lastName("Sow")
                        .email("admin@test.com")
                        .password(passwordEncoder.encode("MotDePasse123!"))
                        .status(UserStatus.ACTIVE)
                        .role(adminPrincipalRole)
                        .organization(demoOrg)
                        .build()));

        // 2d. Affectation superviseur ↔ agent (RB-ORG-09)
        if (supervisorAgentRepository.findByAgentId(demoAgent.getId()).isEmpty()) {
            supervisorAgentRepository.save(SupervisorAgent.builder()
                    .supervisor(demoSupervisor)
                    .agent(demoAgent)
                    .build());
            log.info("Affectation démo : {} supervisé par {}", demoAgent.getEmail(), demoSupervisor.getEmail());
        }

        // S'assurer que tous les agents/utilisateurs existants non-superadmin ont une
        // organisation attribuée
        userRepository.findAll().forEach(user -> {
            if (user.getRole().getName() != RoleType.SUPER_ADMIN && user.getOrganization() == null) {
                user.setOrganization(demoOrg);
                userRepository.save(user);
            }
        });

        // 3. Démo Formulaire & Version pour l'organisation démo
        Form demoForm;
        if (formRepository.count() == 0) {
            demoForm = formRepository.save(Form.builder()
                    .name("Collecte Ménage (Démo)")
                    .description("Formulaire de démonstration pour la collecte sur le terrain")
                    .organization(demoOrg)
                    .status(FormStatus.PUBLISHED)
                    .build());

            String schemaJson = """
                    {
                      "fields": [
                        {"key": "nom_chef", "label": "Nom du chef de ménage", "type": "text", "required": true},
                        {"key": "taille_menage", "label": "Nombre de personnes", "type": "number", "required": true},
                        {"key": "remarques", "label": "Remarques", "type": "textarea", "required": false}
                      ]
                    }
                    """;

            formVersionRepository.save(FormVersion.builder()
                    .form(demoForm)
                    .versionNumber(1)
                    .schemaJson(schemaJson)
                    .createdBy(demoAgent)
                    .build());

            log.info("Formulaire démo et FormVersion #1 créés avec succès pour l'organisation {}", demoOrg.getName());
        } else {
            // S'assurer que les formulaires existants ont une organisation et sont publiés
            formRepository.findAll().forEach(f -> {
                boolean updated = false;
                if (f.getOrganization() == null) {
                    f.setOrganization(demoOrg);
                    updated = true;
                }
                if (f.getStatus() != FormStatus.PUBLISHED) {
                    f.setStatus(FormStatus.PUBLISHED);
                    updated = true;
                }
                if (updated) {
                    formRepository.save(f);
                }
            });
        }
    }
}
