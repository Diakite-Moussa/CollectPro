package com.collectpro.backend.service;

import com.collectpro.backend.dto.AssignSupervisorRequest;
import com.collectpro.backend.dto.CreateUserRequest;
import com.collectpro.backend.dto.UserResponse;
import com.collectpro.backend.entity.Role;
import com.collectpro.backend.entity.SupervisorAgent;
import com.collectpro.backend.entity.User;
import com.collectpro.backend.enums.AuditAction;
import com.collectpro.backend.enums.RoleType;
import com.collectpro.backend.enums.UserStatus;
import com.collectpro.backend.exception.BusinessRuleException;
import com.collectpro.backend.exception.ForbiddenOperationException;
import com.collectpro.backend.exception.ResourceNotFoundException;
import com.collectpro.backend.repository.RoleRepository;
import com.collectpro.backend.repository.SupervisorAgentRepository;
import com.collectpro.backend.repository.UserRepository;
import com.collectpro.backend.security.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RF-004 / RF-USER-01 : création des comptes Admin secondaire, Superviseur
 * et Agent par un administrateur autorisé de l'organisation.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<RoleType> CREATABLE_ROLES = EnumSet.of(RoleType.ADMIN_SECONDAIRE, RoleType.SUPERVISOR,
            RoleType.AGENT);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenUtil tokenUtil;
    private final PermissionService permissionService;
    private final UserInvitationService userInvitationService;
    private final SupervisorAgentRepository supervisorAgentRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public UserResponse createUser(User creator, CreateUserRequest request) {

        // Le "creator" vient du principal d'authentification, résolu dans une
        // transaction distincte (déjà fermée) lors du filtre JWT. Ses relations
        // paresseuses (role, permissions) sont donc détachées et ne peuvent plus
        // être chargées. On le recharge ici, dans la transaction courante.
        User managedCreator = userRepository.findById(creator.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Créateur introuvable"));

        if (managedCreator.getOrganization() == null) {
            throw new BusinessRuleException(
                    "Le créateur ne peut pas créer d'utilisateur en dehors d'une organisation");
        }

        if (!CREATABLE_ROLES.contains(request.getRoleType())) {
            throw new BusinessRuleException(
                    "Le rôle " + request.getRoleType() + " ne peut pas être créé via cet endpoint");
        }

        if (!permissionService.hasPermission(managedCreator, "CREATE_USER")) {
            throw new ForbiddenOperationException(
                    "Le créateur ne dispose pas de la permission CREATE_USER");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException(
                    "Un utilisateur avec l'email '" + request.getEmail() + "' existe déjà");
        }

        Role targetRole = roleRepository.findByName(request.getRoleType())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rôle " + request.getRoleType() + " introuvable"));

        // RB-ORG-07 : rattachement automatique à l'organisation du créateur
        User newUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(tokenUtil.generateRawToken()))
                .status(UserStatus.INVITED)
                .role(targetRole)
                .organization(managedCreator.getOrganization())
                .build();
        newUser = userRepository.save(newUser);

        userInvitationService.sendInvitation(newUser);

        auditLogService.log(
                managedCreator,
                managedCreator.getOrganization(),
                request.getRoleType() == RoleType.ADMIN_SECONDAIRE ? AuditAction.ADMIN_CREATED : AuditAction.USER_CREATED,
                "User",
                newUser.getId(),
                "Création de " + request.getRoleType() + " (" + newUser.getEmail() + ")"
        );

        return toResponse(newUser);
    }

    /**
     * RF-USER-02 : liste des utilisateurs. Le Super Admin (sans organisation)
     * voit tout le monde ; les autres rôles ne voient que les utilisateurs de
     * leur propre organisation.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(User requester) {
        User managedRequester = userRepository.findById(requester.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        List<User> users;
        if (managedRequester.getRole().getName() == RoleType.SUPER_ADMIN) {
            users = userRepository.findAll();
        } else {
            if (managedRequester.getOrganization() == null) {
                throw new BusinessRuleException("Utilisateur sans organisation");
            }
            users = userRepository.findByOrganizationId(managedRequester.getOrganization().getId());
        }

        List<Long> agentIds = users.stream()
                .filter(u -> u.getRole().getName() == RoleType.AGENT)
                .map(User::getId)
                .toList();
        Map<Long, SupervisorAgent> supervisionMap = agentIds.isEmpty()
                ? Map.of()
                : supervisorAgentRepository.findByAgentIdIn(agentIds).stream()
                        .collect(Collectors.toMap(sa -> sa.getAgent().getId(), sa -> sa));

        return users.stream()
                .map(u -> toResponse(u, supervisionMap.get(u.getId())))
                .toList();
    }

    @Transactional
    public void assignAgentToSupervisor(User creator, AssignSupervisorRequest request) {
        User managedCreator = userRepository.findById(creator.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Créateur introuvable"));

        User agent = userRepository.findById(request.getAgentId())
                .orElseThrow(() -> new ResourceNotFoundException("Agent introuvable"));
        User supervisor = userRepository.findById(request.getSupervisorId())
                .orElseThrow(() -> new ResourceNotFoundException("Superviseur introuvable"));

        if (agent.getRole().getName() != RoleType.AGENT) {
            throw new BusinessRuleException("L'utilisateur cible n'est pas un Agent");
        }
        if (supervisor.getRole().getName() != RoleType.SUPERVISOR) {
            throw new BusinessRuleException("L'utilisateur cible n'est pas un Superviseur");
        }
        // RB-ORG-08 : même organisation obligatoire
        if (!agent.getOrganization().getId().equals(supervisor.getOrganization().getId())) {
            throw new BusinessRuleException("L'agent et le superviseur doivent appartenir à la même organisation");
        }
        if (managedCreator.getRole().getName() != RoleType.SUPER_ADMIN
                && !agent.getOrganization().getId().equals(managedCreator.getOrganization().getId())) {
            throw new ForbiddenOperationException("Hors de votre organisation");
        }

        // RB-ORG-09 : un agent n'a qu'un seul superviseur principal → mise à jour si existant
        supervisorAgentRepository.findByAgentId(agent.getId())
                .ifPresentOrElse(existing -> {
                    if (existing.getSupervisor().getId().equals(supervisor.getId())) {
                        return;
                    }
                    existing.setSupervisor(supervisor);
                    supervisorAgentRepository.save(existing);
                }, () -> supervisorAgentRepository.save(SupervisorAgent.builder()
                        .supervisor(supervisor)
                        .agent(agent)
                        .build()));

        auditLogService.log(
                managedCreator,
                agent.getOrganization(),
                AuditAction.SUPERVISOR_ASSIGNED,
                "User",
                agent.getId(),
                "Agent " + agent.getEmail() + " affecté au superviseur " + supervisor.getEmail()
        );
    }

    @Transactional
    public void resendInvitation(User creator, Long userId) {
        User managedCreator = userRepository.findById(creator.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Créateur introuvable"));

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        // Même organisation obligatoire (sauf Super Admin)
        if (managedCreator.getRole().getName() != RoleType.SUPER_ADMIN
                && (target.getOrganization() == null
                        || !target.getOrganization().getId().equals(managedCreator.getOrganization().getId()))) {
            throw new ForbiddenOperationException("Hors de votre organisation");
        }

        // On ne renvoie une invitation que si le compte n'est pas déjà activé
        if (target.getStatus() != UserStatus.INVITED) {
            throw new BusinessRuleException(
                    "Impossible de renvoyer une invitation : le compte est déjà " + target.getStatus());
        }

        userInvitationService.sendInvitation(target);

        auditLogService.log(
                managedCreator,
                target.getOrganization(),
                AuditAction.INVITATION_RESENT,
                "User",
                target.getId(),
                "Invitation renvoyée à " + target.getEmail()
        );
    }

    private UserResponse toResponse(User user) {
        return toResponse(user, null);
    }

    private UserResponse toResponse(User user, SupervisorAgent assignment) {
        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus().name())
                .role(user.getRole().getName().name())
                .organizationId(user.getOrganization() != null ? user.getOrganization().getId() : null)
                .createdAt(user.getCreatedAt());

        if (assignment != null) {
            User supervisor = assignment.getSupervisor();
            builder.assignedSupervisorId(supervisor.getId())
                    .assignedSupervisorName(supervisor.getFirstName() + " " + supervisor.getLastName());
        }

        return builder.build();
    }
}