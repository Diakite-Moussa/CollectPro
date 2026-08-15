package com.collectpro.backend.repository;

import com.collectpro.backend.entity.ActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByTokenHash(String tokenHash);
}