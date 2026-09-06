package com.collectpro.backend.repository;

import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByOrganizationId(Long organizationId);
    Optional<User> findByOrganizationIdAndRole_Name(Long organizationId, RoleType roleName);

    // Fix #12 — pagination serveur
    Page<User> findByOrganizationId(Long organizationId, Pageable pageable);
    // Page<User> findAll(Pageable pageable) est déjà héritée de JpaRepository

    long countByOrganizationId(Long organizationId);
    long countByOrganizationIdAndRole_Name(Long organizationId, RoleType roleName);
    long countByRole_Name(RoleType roleName);
}