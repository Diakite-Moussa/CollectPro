package com.collectpro.backend.service;

import com.collectpro.backend.dto.AssignAgentsToMissionRequest;
import com.collectpro.backend.dto.AssignFormsToMissionRequest;
import com.collectpro.backend.dto.CreateMissionRequest;
import com.collectpro.backend.dto.MissionProgressResponse;
import com.collectpro.backend.dto.MissionResponse;
import com.collectpro.backend.dto.UpdateMissionRequest;
import com.collectpro.backend.entity.Form;
import com.collectpro.backend.entity.Mission;
import com.collectpro.backend.entity.MissionAgent;
import com.collectpro.backend.entity.Organization;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.MissionStatus;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.CollecteRepository;
import com.collectpro.backend.repository.FormRepository;
import com.collectpro.backend.repository.MissionAgentRepository;
import com.collectpro.backend.repository.MissionRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.repository.projection.MissionAgentCountProjection;
import com.collectpro.backend.repository.projection.MissionProgressProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;
    private final MissionAgentRepository missionAgentRepository;
    private final FormRepository formRepository;
    private final UserRepository userRepository;
    private final CollecteRepository collecteRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public MissionResponse createMission(CreateMissionRequest request, Organization organization, User actor) {
        Mission mission = Mission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organization(organization)
                .status(MissionStatus.DRAFT)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radiusMeters(request.getRadiusMeters())
                .expectedCollectesCount(request.getExpectedCollectesCount())
                .createdBy(actor)
                .build();
        mission = missionRepository.save(mission);

        auditLogService.log(
                actor,
                organization,
                AuditAction.MISSION_CREATED,
                "Mission",
                mission.getId(),
                "Création de la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    @Transactional
    public MissionResponse updateMission(Long missionId, UpdateMissionRequest request, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertSameOrganization(mission, actor);

        if (mission.getStatus() == MissionStatus.CANCELLED) {
            throw new BusinessRuleException("Une mission annulée ne peut plus être modifiée");
        }

        mission.setName(request.getName());
        mission.setDescription(request.getDescription());
        mission.setStartDate(request.getStartDate());
        mission.setEndDate(request.getEndDate());
        mission.setLatitude(request.getLatitude());
        mission.setLongitude(request.getLongitude());
        mission.setRadiusMeters(request.getRadiusMeters());
        mission.setExpectedCollectesCount(request.getExpectedCollectesCount());

        if (request.getStatus() != null) {
            assertValidTransition(mission.getStatus(), request.getStatus());
            mission.setStatus(request.getStatus());
        }

        mission = missionRepository.save(mission);

        auditLogService.log(
                actor,
                mission.getOrganization(),
                AuditAction.MISSION_UPDATED,
                "Mission",
                mission.getId(),
                "Mise à jour de la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    @Transactional
    public MissionResponse cancelMission(Long missionId, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertSameOrganization(mission, actor);

        if (mission.getStatus() == MissionStatus.COMPLETED) {
            throw new BusinessRuleException("Une mission terminée ne peut pas être annulée");
        }
        if (mission.getStatus() == MissionStatus.CANCELLED) {
            throw new BusinessRuleException("Cette mission est déjà annulée");
        }

        mission.setStatus(MissionStatus.CANCELLED);
        mission = missionRepository.save(mission);

        auditLogService.log(
                actor,
                mission.getOrganization(),
                AuditAction.MISSION_CANCELLED,
                "Mission",
                mission.getId(),
                "Annulation de la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    @Transactional
    public MissionResponse activateMission(Long missionId, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertSameOrganization(mission, actor);
        assertValidTransition(mission.getStatus(), MissionStatus.ACTIVE);

        mission.setStatus(MissionStatus.ACTIVE);
        mission = missionRepository.save(mission);

        auditLogService.log(
                actor,
                mission.getOrganization(),
                AuditAction.MISSION_UPDATED,
                "Mission",
                mission.getId(),
                "Activation de la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    @Transactional
    public MissionResponse completeMission(Long missionId, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertSameOrganization(mission, actor);
        assertValidTransition(mission.getStatus(), MissionStatus.COMPLETED);

        mission.setStatus(MissionStatus.COMPLETED);
        mission = missionRepository.save(mission);

        auditLogService.log(
                actor,
                mission.getOrganization(),
                AuditAction.MISSION_UPDATED,
                "Mission",
                mission.getId(),
                "Clôture de la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    private void assertMissionAssignable(Mission mission) {
        if (mission.getStatus() == MissionStatus.COMPLETED || mission.getStatus() == MissionStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Impossible de modifier les assignations d'une mission " + mission.getStatus());
        }
    }

    @Transactional
    public MissionResponse assignAgents(Long missionId, AssignAgentsToMissionRequest request, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertSameOrganization(mission, actor);
        assertMissionAssignable(mission);

        for (Long agentId : request.getAgentIds()) {
            if (missionAgentRepository.existsByMissionIdAndAgentId(missionId, agentId)) {
                continue; // déjà assigné, on ignore silencieusement
            }
            User agent = userRepository.findById(agentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable (id=" + agentId + ")"));

            if (agent.getOrganization() == null
                    || !agent.getOrganization().getId().equals(mission.getOrganization().getId())) {
                throw new BusinessRuleException(
                        "L'agent " + agent.getEmail() + " n'appartient pas à l'organisation de la mission");
            }
            if (agent.getRole().getName() != RoleType.AGENT) {
                throw new BusinessRuleException(
                        "L'utilisateur " + agent.getEmail() + " n'est pas un agent terrain");
            }

            MissionAgent missionAgent = MissionAgent.builder()
                    .mission(mission)
                    .agent(agent)
                    .build();
            missionAgentRepository.save(missionAgent);
        }

        auditLogService.log(
                actor,
                mission.getOrganization(),
                AuditAction.AGENTS_ASSIGNED_TO_MISSION,
                "Mission",
                mission.getId(),
                request.getAgentIds().size() + " agent(s) assigné(s) à la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    @Transactional
    public MissionResponse unassignAgent(Long missionId, Long agentId, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertSameOrganization(mission, actor);
        assertMissionAssignable(mission);

        if (!missionAgentRepository.existsByMissionIdAndAgentId(missionId, agentId)) {
            throw new ResourceNotFoundException("Cet agent n'est pas assigné à cette mission");
        }
        missionAgentRepository.deleteByMissionIdAndAgentId(missionId, agentId);

        auditLogService.log(
                actor,
                mission.getOrganization(),
                AuditAction.AGENT_UNASSIGNED_FROM_MISSION,
                "Mission",
                mission.getId(),
                "Agent (id=" + agentId + ") retiré de la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    @Transactional
    public MissionResponse assignForms(Long missionId, AssignFormsToMissionRequest request, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertSameOrganization(mission, actor);
        assertMissionAssignable(mission);

        for (Long formId : request.getFormIds()) {
            Form form = formRepository.findById(formId)
                    .orElseThrow(() -> new ResourceNotFoundException("Formulaire introuvable (id=" + formId + ")"));

            if (!form.getOrganization().getId().equals(mission.getOrganization().getId())) {
                throw new BusinessRuleException(
                        "Le formulaire '" + form.getName() + "' n'appartient pas à l'organisation de la mission");
            }
            mission.getForms().add(form);
        }
        mission = missionRepository.save(mission);

        auditLogService.log(
                actor,
                mission.getOrganization(),
                AuditAction.FORMS_ASSIGNED_TO_MISSION,
                "Mission",
                mission.getId(),
                request.getFormIds().size() + " formulaire(s) assigné(s) à la mission '" + mission.getName() + "'"
        );

        return toResponse(mission);
    }

    public MissionResponse getMission(Long missionId, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertCanView(mission, actor);
        return toResponse(mission);
    }

    public List<MissionResponse> getMissionsForOrganization(Organization organization) {
        if (organization == null) {
            return List.of();
        }
        return missionRepository.findByOrganization(organization).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MissionResponse> getAllMissions() {
        return missionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MissionResponse> getMissionsForAgent(User agent) {
        return missionAgentRepository.findByAgentId(agent.getId()).stream()
                .map(MissionAgent::getMission)
                .map(this::toResponse)
                .toList();
    }


    /**
     * Fix #12 — pagination serveur, nouvel endpoint dédié dashboard
     * (GET /missions/list). N'est jamais appelé par l'app mobile,
     * donc pas de contrainte de compatibilité de format de réponse.
     * Réutilise la même logique de visibilité par rôle que getMissions().
     */
    @Transactional(readOnly = true)
    public Page<MissionResponse> getMissionsPaged(User actor, Pageable pageable) {
        RoleType role = actor.getRole().getName();
        Page<Mission> page;
        if (role == RoleType.SUPER_ADMIN) {
            page = missionRepository.findAll(pageable);
        } else if (role == RoleType.AGENT) {
            // Cas rare (l'agent utilise normalement le mobile / GET /missions),
            // mais on couvre le cas où le dashboard serait consulté par un agent.
            List<Mission> assigned = missionAgentRepository.findByAgentId(actor.getId()).stream()
                    .map(MissionAgent::getMission)
                    .toList();
            page = new org.springframework.data.domain.PageImpl<>(assigned, pageable, assigned.size());
        } else {
            if (actor.getOrganization() == null) {
                page = Page.empty(pageable);
            } else {
                page = missionRepository.findByOrganization(actor.getOrganization(), pageable);
            }
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MissionProgressResponse getMissionProgress(Long missionId, User actor) {
        Mission mission = getMissionEntity(missionId);
        assertCanView(mission, actor);

        List<MissionProgressProjection> collecteStats =
                collecteRepository.findProgressByMissionIds(List.of(missionId));
        long received = collecteStats.isEmpty() ? 0L : collecteStats.get(0).getReceivedCount();
        long activeAgents = collecteStats.isEmpty() ? 0L : collecteStats.get(0).getActiveAgentsCount();

        long assignedAgents = missionAgentRepository.countByMissionId(missionId);

        return toProgressResponse(mission, received, activeAgents, assignedAgents);
    }

    @Transactional(readOnly = true)
    public List<MissionProgressResponse> getMissionsProgress(User actor) {
        List<Mission> missions = getVisibleMissionEntities(actor);
        if (missions.isEmpty()) {
            return List.of();
        }
        List<Long> missionIds = missions.stream().map(Mission::getId).toList();

        // Deux requêtes groupées au total pour TOUTES les missions (pas de N+1).
        Map<Long, MissionProgressProjection> collecteStatsByMission =
                collecteRepository.findProgressByMissionIds(missionIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                MissionProgressProjection::getMissionId, Function.identity()));
        Map<Long, Long> assignedAgentsByMission =
                missionAgentRepository.countAssignedAgentsByMissionIds(missionIds).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                MissionAgentCountProjection::getMissionId, MissionAgentCountProjection::getCount));

        return missions.stream()
                .map(mission -> {
                    MissionProgressProjection stats = collecteStatsByMission.get(mission.getId());
                    long received = stats != null ? stats.getReceivedCount() : 0L;
                    long activeAgents = stats != null ? stats.getActiveAgentsCount() : 0L;
                    long assignedAgents = assignedAgentsByMission.getOrDefault(mission.getId(), 0L);
                    return toProgressResponse(mission, received, activeAgents, assignedAgents);
                })
                .toList();
    }

    private List<Mission> getVisibleMissionEntities(User actor) {
        RoleType role = actor.getRole().getName();
        if (role == RoleType.SUPER_ADMIN) {
            return missionRepository.findAll();
        }
        if (role == RoleType.AGENT) {
            return missionAgentRepository.findByAgentId(actor.getId()).stream()
                    .map(MissionAgent::getMission)
                    .toList();
        }
        if (actor.getOrganization() == null) {
            return List.of();
        }
        return missionRepository.findByOrganization(actor.getOrganization());
    }

    private MissionProgressResponse toProgressResponse(
            Mission mission, long received, long activeAgents, long assignedAgents) {
        Integer expected = mission.getExpectedCollectesCount();
        Double progressPercent = (expected != null && expected > 0)
                ? Math.min(100.0, received * 100.0 / expected)
                : null;

        return MissionProgressResponse.builder()
                .missionId(mission.getId())
                .missionName(mission.getName())
                .expectedCollectesCount(expected)
                .receivedCollectesCount(received)
                .progressPercent(progressPercent)
                .activeAgentsCount(activeAgents)
                .assignedAgentsCount(assignedAgents)
                .build();
    }

    private Mission getMissionEntity(Long missionId) {
        return missionRepository.findById(missionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable (id=" + missionId + ")"));
    }

    private void assertSameOrganization(Mission mission, User actor) {
        if (actor.getRole().getName() == RoleType.SUPER_ADMIN) {
            return;
        }
        if (actor.getOrganization() == null
                || !actor.getOrganization().getId().equals(mission.getOrganization().getId())) {
            throw new ForbiddenOperationException("Cette mission n'appartient pas à votre organisation");
        }
    }

    private void assertCanView(Mission mission, User actor) {
        if (actor.getRole().getName() == RoleType.SUPER_ADMIN) {
            return;
        }
        if (actor.getRole().getName() == RoleType.AGENT) {
            boolean assigned = missionAgentRepository.existsByMissionIdAndAgentId(mission.getId(), actor.getId());
            if (!assigned) {
                throw new ForbiddenOperationException("Vous n'êtes pas assigné à cette mission");
            }
            return;
        }
        assertSameOrganization(mission, actor);
    }

    private void assertValidTransition(MissionStatus current, MissionStatus next) {
        boolean valid = switch (current) {
            case DRAFT -> next == MissionStatus.ACTIVE || next == MissionStatus.CANCELLED;
            case ACTIVE -> next == MissionStatus.COMPLETED || next == MissionStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new BusinessRuleException(
                    "Transition de statut invalide : " + current + " → " + next);
        }
    }

    private MissionResponse toResponse(Mission mission) {
        List<MissionResponse.AgentSummary> agents = missionAgentRepository.findByMissionId(mission.getId()).stream()
                .map(ma -> MissionResponse.AgentSummary.builder()
                        .id(ma.getAgent().getId())
                        .firstName(ma.getAgent().getFirstName())
                        .lastName(ma.getAgent().getLastName())
                        .build())
                .toList();

        List<MissionResponse.FormSummary> forms = mission.getForms().stream()
                .map(f -> MissionResponse.FormSummary.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .build())
                .toList();

        return MissionResponse.builder()
                .id(mission.getId())
                .name(mission.getName())
                .description(mission.getDescription())
                .organizationId(mission.getOrganization().getId())
                .status(mission.getStatus())
                .startDate(mission.getStartDate())
                .endDate(mission.getEndDate())
                .latitude(mission.getLatitude())
                .longitude(mission.getLongitude())
                .radiusMeters(mission.getRadiusMeters())
                .expectedCollectesCount(mission.getExpectedCollectesCount())
                .createdBy(MissionResponse.CreatorSummary.builder()
                        .id(mission.getCreatedBy().getId())
                        .firstName(mission.getCreatedBy().getFirstName())
                        .lastName(mission.getCreatedBy().getLastName())
                        .build())
                .forms(forms)
                .agents(agents)
                .createdAt(mission.getCreatedAt())
                .updatedAt(mission.getUpdatedAt())
                .build();
    }
}