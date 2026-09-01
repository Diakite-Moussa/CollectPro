package com.collectpro.backend.config;

import com.collectpro.backend.entity.Permission;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.repository.PermissionRepository;
import com.collectpro.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class PermissionDataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    private static final List<String> ALL_PERMISSION_CODES = List.of(
            "CREATE_ORGANIZATION", "CREATE_USER", "UPDATE_USER", "DISABLE_USER",
            "CREATE_FORM", "UPDATE_FORM", "PUBLISH_FORM",
            "CREATE_COLLECTE", "VIEW_COLLECTE", "VALIDATE_COLLECTE", "REJECT_COLLECTE",
            "GENERATE_REPORT", "VIEW_REPORT", "VIEW_STATISTICS",
            "CREATE_MISSION", "UPDATE_MISSION", "ASSIGN_MISSION", "VIEW_MISSION", "CANCEL_MISSION"
    );

    private static final Map<RoleType, List<String>> DEFAULT_ROLE_PERMISSIONS = new EnumMap<>(RoleType.class);
    static {
        DEFAULT_ROLE_PERMISSIONS.put(RoleType.SUPER_ADMIN, List.of("CREATE_ORGANIZATION", "VIEW_STATISTICS"));
        DEFAULT_ROLE_PERMISSIONS.put(RoleType.ADMIN_PRINCIPAL, List.of(
                "CREATE_USER", "UPDATE_USER", "DISABLE_USER",
                "CREATE_FORM", "UPDATE_FORM", "PUBLISH_FORM",
                "VIEW_COLLECTE", "GENERATE_REPORT", "VIEW_REPORT", "VIEW_STATISTICS",
                "CREATE_MISSION", "UPDATE_MISSION", "ASSIGN_MISSION", "VIEW_MISSION", "CANCEL_MISSION"));
        DEFAULT_ROLE_PERMISSIONS.put(RoleType.ADMIN_SECONDAIRE, List.of());
        DEFAULT_ROLE_PERMISSIONS.put(RoleType.SUPERVISOR, List.of(
                "VIEW_COLLECTE", "VALIDATE_COLLECTE", "REJECT_COLLECTE", "VIEW_STATISTICS",
                "VIEW_MISSION", "ASSIGN_MISSION", "VIEW_REPORT"));
        DEFAULT_ROLE_PERMISSIONS.put(RoleType.AGENT, List.of("CREATE_COLLECTE", "VIEW_COLLECTE", "VIEW_MISSION"));
    }


    @Override
    @Transactional
    public void run(String... args) {
        for (String code : ALL_PERMISSION_CODES) {
            permissionRepository.findByCode(code).orElseGet(() -> {
                log.info("Initialisation de la permission : {}", code);
                return permissionRepository.save(Permission.builder().code(code).build());
            });
        }

        for (Map.Entry<RoleType, List<String>> entry : DEFAULT_ROLE_PERMISSIONS.entrySet()) {
            Role role = roleRepository.findByName(entry.getKey()).orElse(null);
            if (role == null) continue;

            boolean changed = false;
            for (String code : entry.getValue()) {
                Permission permission = permissionRepository.findByCode(code).orElse(null);
                if (permission != null && role.getPermissions().stream()
                        .noneMatch(p -> p.getCode().equals(code))) {
                    role.getPermissions().add(permission);
                    changed = true;
                }
            }
            if (changed) {
                log.info("Attribution des permissions par défaut au rôle {}", role.getName());
                roleRepository.save(role);
            }
        }
    }
}