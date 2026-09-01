package com.collectpro.backend.controller;

import com.collectpro.backend.dto.AssignAgentsToMissionRequest;
import com.collectpro.backend.dto.AssignFormsToMissionRequest;
import com.collectpro.backend.dto.CreateMissionRequest;
import com.collectpro.backend.dto.MissionProgressResponse;
import com.collectpro.backend.dto.MissionResponse;
import com.collectpro.backend.dto.UpdateMissionRequest;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.security.CustomUserDetails;
import com.collectpro.backend.service.MissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @PostMapping
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'CREATE_MISSION')")
    public ResponseEntity<MissionResponse> createMission(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateMissionRequest request) {
        MissionResponse response = missionService.createMission(
                request, principal.getUser().getOrganization(), principal.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_MISSION')")
    public ResponseEntity<List<MissionResponse>> getMissions(
            @AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        if (user.getRole().getName() == RoleType.AGENT) {
            return ResponseEntity.ok(missionService.getMissionsForAgent(user));
        }
        if (user.getRole().getName() == RoleType.SUPER_ADMIN) {
            return ResponseEntity.ok(missionService.getAllMissions());
        }
        return ResponseEntity.ok(missionService.getMissionsForOrganization(user.getOrganization()));
    }

    @GetMapping("/progress")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_MISSION')")
    public ResponseEntity<List<MissionProgressResponse>> getMissionsProgress(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(missionService.getMissionsProgress(principal.getUser()));
    }

    @GetMapping("/{id}/progress")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_MISSION')")
    public ResponseEntity<MissionProgressResponse> getMissionProgress(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(missionService.getMissionProgress(id, principal.getUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'VIEW_MISSION')")
    public ResponseEntity<MissionResponse> getMission(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(missionService.getMission(id, principal.getUser()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'UPDATE_MISSION')")
    public ResponseEntity<MissionResponse> updateMission(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateMissionRequest request) {
        return ResponseEntity.ok(missionService.updateMission(id, request, principal.getUser()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'CANCEL_MISSION')")
    public ResponseEntity<MissionResponse> cancelMission(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(missionService.cancelMission(id, principal.getUser()));
    }

    @PostMapping("/{id}/agents")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'ASSIGN_MISSION')")
    public ResponseEntity<MissionResponse> assignAgents(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignAgentsToMissionRequest request) {
        return ResponseEntity.ok(missionService.assignAgents(id, request, principal.getUser()));
    }

    @DeleteMapping("/{id}/agents/{agentId}")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'ASSIGN_MISSION')")
    public ResponseEntity<MissionResponse> unassignAgent(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @PathVariable Long agentId) {
        return ResponseEntity.ok(missionService.unassignAgent(id, agentId, principal.getUser()));
    }

    @PostMapping("/{id}/forms")
    @PreAuthorize("@securityAuth.hasPermission(authentication, 'ASSIGN_MISSION')")
    public ResponseEntity<MissionResponse> assignForms(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody AssignFormsToMissionRequest request) {
        return ResponseEntity.ok(missionService.assignForms(id, request, principal.getUser()));
    }
}