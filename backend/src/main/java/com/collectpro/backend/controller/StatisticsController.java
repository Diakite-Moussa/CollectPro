package com.collectpro.backend.controller;

import com.collectpro.backend.dto.StatisticsResponse;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_STATISTICS')")
    public ResponseEntity<StatisticsResponse> getStatistics(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(statisticsService.getStatistics(principal.getUser()));
    }
}