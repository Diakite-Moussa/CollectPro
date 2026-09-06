package com.collectpro.backend.security;

import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.OrganizationStatus;
import com.collectpro.backend.enums.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        boolean isUserNonLocked = user.getStatus() != UserStatus.DISABLED;
        boolean isOrgNonDisabled = user.getOrganization() == null
                || user.getOrganization().getStatus() != OrganizationStatus.INACTIVE;
        return isUserNonLocked && isOrgNonDisabled;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        boolean isUserActive = user.getStatus() == UserStatus.ACTIVE;
        boolean isOrgActive = user.getOrganization() == null
                || user.getOrganization().getStatus() == OrganizationStatus.ACTIVE;
        return isUserActive && isOrgActive;
    }
}