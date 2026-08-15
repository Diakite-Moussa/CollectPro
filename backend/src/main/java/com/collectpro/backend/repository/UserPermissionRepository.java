package com.collectpro.backend.repository;

import com.collectpro.backend.entity.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
    List<UserPermission> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}