package com.collectpro.backend.repository;

import com.collectpro.backend.entity.Role;
import com.collectpro.backend.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}