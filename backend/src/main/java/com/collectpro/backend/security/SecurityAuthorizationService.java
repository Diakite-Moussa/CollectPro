package com.collectpro.backend.security;

import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Expressions SpEL pour @PreAuthorize, ex. :
 * {@code @PreAuthorize("@securityAuth.hasRole(authentication, 'SUPERVISOR')")}
 */
@Component("securityAuth")
@RequiredArgsConstructor
public class SecurityAuthorizationService {

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    public boolean hasRole(Authentication authentication, String role) {
        User user = resolveUser(authentication);
        if (user == null) return false;
        return user.getRole().getName().name().equals(role);
    }

    public boolean hasAnyRole(Authentication authentication, String... roles) {
        User user = resolveUser(authentication);
        if (user == null) return false;
        String userRole = user.getRole().getName().name();
        for (String role : roles) {
            if (userRole.equals(role)) return true;
        }
        return false;
    }

    public boolean hasPermission(Authentication authentication, String permissionCode) {
        User user = resolveUser(authentication);
        if (user == null) return false;
        return permissionService.hasPermission(user, permissionCode);
    }

    public boolean isSuperAdmin(Authentication authentication) {
        return hasRole(authentication, RoleType.SUPER_ADMIN.name());
    }

    private User resolveUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return null;
        }
        return userRepository.findById(details.getUser().getId()).orElse(null);
    }
}
