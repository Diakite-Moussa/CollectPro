package com.collectpro.backend.config;

import com.collectpro.backend.entity.Role;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (RoleType type : RoleType.values()) {
            roleRepository.findByName(type).orElseGet(() -> {
                log.info("Initialisation du rôle système : {}", type);
                return roleRepository.save(Role.builder().name(type).build());
            });
        }
    }
}