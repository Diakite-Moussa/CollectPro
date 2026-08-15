package com.collectpro.backend.service;

import com.collectpro.backend.dto.PermissionResponse;
import com.collectpro.backend.dto.UpdateUserPermissionsRequest;
import com.collectpro.backend.dto.UserPermissionsResponse;
import com.collectpro.backend.entity.Permission;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.entity.UserPermission;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.PermissionRepository;
import com.collectpro.backend.repository.UserPermissionRepository;
import com.collectpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permissions effectives = union des permissions du rôle (role_permissions)
 * et des permissions individuelles (user_permissions). Cf. cahier des
 * charges, section 9.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    /** Permissions qu'un Admin principal peut déléguer à un Admin secondaire. */
    private static final List<String> DELEGATABLE_PERMISSION_CODES = List.of(
            "CREATE_USER", "UPDATE_USER", "DISABLE_USER",
            "CREATE_FORM", "UPDATE_FORM", "PUBLISH_FORM",
            "VIEW_COLLECTE", "GENERATE_REPORT", "VIEW_STATISTICS");

    private static final Map<String, String> PERMISSION_DESCRIPTIONS = Map.ofEntries(
            Map.entry("CREATE_ORGANIZATION", "Créer des organisations"),
            Map.entry("CREATE_USER", "Créer des utilisateurs"),
            Map.entry("UPDATE_USER", "Modifier des utilisateurs"),
            Map.entry("DISABLE_USER", "Désactiver des utilisateurs"),
            Map.entry("CREATE_FORM", "Créer des formulaires"),
            Map.entry("UPDATE_FORM", "Modifier des formulaires"),
            Map.entry("PUBLISH_FORM", "Publier des formulaires"),
            Map.entry("CREATE_COLLECTE", "Créer des collectes"),
            Map.entry("VIEW_COLLECTE", "Consulter les collectes"),
            Map.entry("VALIDATE_COLLECTE", "Valider des collectes"),
            Map.entry("REJECT_COLLECTE", "Rejeter des collectes"),
            Map.entry("GENERATE_REPORT", "Générer des rapports"),
            Map.entry("VIEW_STATISTICS", "Consulter les statistiques"));

    private final UserPermissionRepository userPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public boolean hasPermission(User user, String permissionCode) {
        User managed = userRepository.findById(user.getId()).orElse(user);
        boolean fromRole = managed.getRole().getPermissions().stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
        if (fromRole) {
            return true;
        }
        return userPermissionRepository.findByUserId(managed.getId()).stream()
                .anyMatch(up -> up.getPermission().getCode().equals(permissionCode));
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getDelegatablePermissions(User requester) {
        User managedRequester = loadRequester(requester);
        if (!canManageUserPermissions(managedRequester)) {
            throw new ForbiddenOperationException(
                    "Seul un Admin principal ou le Super Admin peut consulter les permissions déléguables");
        }
        return DELEGATABLE_PERMISSION_CODES.stream()
                .map(code -> PermissionResponse.builder()
                        .code(code)
                        .description(PERMISSION_DESCRIPTIONS.getOrDefault(code, code))
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public UserPermissionsResponse getUserPermissions(User requester, Long userId) {
        User managedRequester = loadRequester(requester);
        User target = loadTargetUser(userId);
        checkCanManageTargetPermissions(managedRequester, target);

        List<String> roleCodes = target.getRole().getPermissions().stream()
                .map(Permission::getCode)
                .sorted()
                .toList();
        List<String> individualCodes = userPermissionRepository.findByUserId(target.getId()).stream()
                .map(up -> up.getPermission().getCode())
                .sorted()
                .toList();
        Set<String> effective = new HashSet<>(roleCodes);
        effective.addAll(individualCodes);

        return UserPermissionsResponse.builder()
                .userId(target.getId())
                .role(target.getRole().getName().name())
                .rolePermissionCodes(roleCodes)
                .individualPermissionCodes(individualCodes)
                .effectivePermissionCodes(effective.stream().sorted().toList())
                .build();
    }

    @Transactional
    public UserPermissionsResponse updateUserPermissions(
            User requester, Long userId, UpdateUserPermissionsRequest body) {
        User managedRequester = loadRequester(requester);
        User target = loadTargetUser(userId);
        checkCanManageTargetPermissions(managedRequester, target);

        Set<String> requested = new HashSet<>(body.getPermissionCodes());
        for (String code : requested) {
            if (!DELEGATABLE_PERMISSION_CODES.contains(code)) {
                throw new BusinessRuleException(
                        "La permission '" + code + "' ne peut pas être déléguée à un Admin secondaire");
            }
        }

        userPermissionRepository.deleteByUserId(target.getId());

        for (String code : requested) {
            Permission permission = permissionRepository.findByCode(code)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission introuvable : " + code));
            userPermissionRepository.save(UserPermission.builder()
                    .user(target)
                    .permission(permission)
                    .grantedBy(managedRequester.getId())
                    .build());
        }

        return getUserPermissions(managedRequester, userId);
    }

    private User loadRequester(User requester) {
        return userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private User loadTargetUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }

    private boolean canManageUserPermissions(User requester) {
        RoleType role = requester.getRole().getName();
        return role == RoleType.SUPER_ADMIN || role == RoleType.ADMIN_PRINCIPAL;
    }

    private void checkCanManageTargetPermissions(User requester, User target) {
        if (!canManageUserPermissions(requester)) {
            throw new ForbiddenOperationException(
                    "Seul un Admin principal ou le Super Admin peut gérer les permissions individuelles");
        }
        if (target.getRole().getName() != RoleType.ADMIN_SECONDAIRE) {
            throw new BusinessRuleException(
                    "Les permissions individuelles ne peuvent être gérées que pour un Admin secondaire");
        }
        if (requester.getRole().getName() != RoleType.SUPER_ADMIN
                && (target.getOrganization() == null
                        || requester.getOrganization() == null
                        || !target.getOrganization().getId().equals(requester.getOrganization().getId()))) {
            throw new ForbiddenOperationException("Hors de votre organisation");
        }
    }
}
