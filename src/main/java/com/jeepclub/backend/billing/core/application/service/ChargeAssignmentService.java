package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.assignment.BillingAssignmentTargetNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeAssignmentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeAssignmentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.assignment.ChargeDefinitionCannotChangeAssignmentsException;
import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.result.ChargeAssignmentResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.domain.model.assignment.AllMembersChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.EventParticipantsChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.RoleChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.UserChargeAssignment;
import com.jeepclub.backend.billing.core.port.BillingAuthorizationPort;
import com.jeepclub.backend.billing.core.port.BillingEventPort;
import com.jeepclub.backend.billing.core.port.BillingMembershipPort;
import com.jeepclub.backend.billing.core.repository.ChargeAssignmentRepository;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChargeAssignmentService {

    private final ChargeAssignmentRepository chargeAssignmentRepository;
    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final BillingEventPort billingEventPort;
    private final BillingMembershipPort billingMembershipPort;
    private final BillingAuthorizationPort billingAuthorizationPort;
    private final Clock clock;

    @Transactional
    public ChargeAssignmentResult assignToAllMembers(Long chargeDefinitionId) {
        findActiveChargeDefinitionOrThrow(chargeDefinitionId);
        ensureAllMembersAssignmentDoesNotExist(chargeDefinitionId);

        ChargeAssignment chargeAssignment = AllMembersChargeAssignment.create(
                chargeDefinitionId,
                Instant.now(clock)
        );

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    @Transactional
    public ChargeAssignmentResult assignToUser(
            Long chargeDefinitionId,
            Long userId
    ) {
        findActiveChargeDefinitionOrThrow(chargeDefinitionId);
        ensureActiveMemberExists(userId);
        ensureUserAssignmentDoesNotExist(chargeDefinitionId, userId);

        ChargeAssignment chargeAssignment = UserChargeAssignment.create(
                chargeDefinitionId,
                userId,
                Instant.now(clock)
        );

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    @Transactional
    public ChargeAssignmentResult assignToRole(
            Long chargeDefinitionId,
            Long roleId
    ) {
        findActiveChargeDefinitionOrThrow(chargeDefinitionId);
        ensureActiveRoleExists(roleId);
        ensureRoleAssignmentDoesNotExist(chargeDefinitionId, roleId);

        ChargeAssignment chargeAssignment = RoleChargeAssignment.create(
                chargeDefinitionId,
                roleId,
                Instant.now(clock)
        );

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    @Transactional
    public ChargeAssignmentResult assignToEventParticipants(
            Long chargeDefinitionId,
            Long eventId
    ) {
        findActiveChargeDefinitionOrThrow(chargeDefinitionId);
        ensureEventExists(eventId);
        ensureEventParticipantsAssignmentDoesNotExist(chargeDefinitionId, eventId);

        ChargeAssignment chargeAssignment = EventParticipantsChargeAssignment.create(
                chargeDefinitionId,
                eventId,
                Instant.now(clock)
        );

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    @Transactional(readOnly = true)
    public Page<ChargeAssignmentResult> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    ) {
        Objects.requireNonNull(pageable, "pageable cannot be null");

        ensureChargeDefinitionExists(chargeDefinitionId);

        return chargeAssignmentRepository.findByChargeDefinitionId(
                        chargeDefinitionId,
                        pageable
                )
                .map(ChargeAssignmentResult::from);
    }

    @Transactional
    public ChargeAssignmentResult activate(Long id) {
        ChargeAssignment chargeAssignment = findChargeAssignmentOrThrow(id);

        findActiveChargeDefinitionOrThrow(chargeAssignment.getChargeDefinitionId());

        chargeAssignment.activate(Instant.now(clock));

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    @Transactional
    public ChargeAssignmentResult deactivate(Long id) {
        ChargeAssignment chargeAssignment = findChargeAssignmentOrThrow(id);

        ensureChargeDefinitionIsNotArchived(chargeAssignment.getChargeDefinitionId());

        chargeAssignment.deactivate(Instant.now(clock));

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    @Transactional(readOnly = true)
    public ChargeAssignmentResult findById(Long id) {
        return ChargeAssignmentResult.from(findChargeAssignmentOrThrow(id));
    }

    private ChargeDefinition findActiveChargeDefinitionOrThrow(Long chargeDefinitionId) {
        Objects.requireNonNull(chargeDefinitionId, "chargeDefinitionId cannot be null");

        ChargeDefinition chargeDefinition = chargeDefinitionRepository.findById(chargeDefinitionId)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException(
                        "Charge definition not found."
                ));

        if (!chargeDefinition.isActive()) {
            throw new ChargeDefinitionCannotChangeAssignmentsException(
                    "Only active charge definitions can receive assignments."
            );
        }

        return chargeDefinition;
    }

    private void ensureEventExists(Long eventId) {
        Objects.requireNonNull(eventId, "eventId cannot be null");

        if (!billingEventPort.existsEventById(eventId)) {
            throw new BillingAssignmentTargetNotFoundException(
                    "Event not found."
            );
        }
    }

    private void ensureAllMembersAssignmentDoesNotExist(Long chargeDefinitionId) {
        if (chargeAssignmentRepository.existsAllMembersAssignmentByChargeDefinitionId(chargeDefinitionId)) {
            throw new ChargeAssignmentAlreadyExistsException(
                    "Charge assignment already exists."
            );
        }
    }

    private void ensureUserAssignmentDoesNotExist(
            Long chargeDefinitionId,
            Long userId
    ) {
        Objects.requireNonNull(userId, "userId cannot be null");

        if (chargeAssignmentRepository.existsUserAssignmentByChargeDefinitionIdAndUserId(
                chargeDefinitionId,
                userId
        )) {
            throw new ChargeAssignmentAlreadyExistsException(
                    "Charge assignment already exists."
            );
        }
    }

    private void ensureRoleAssignmentDoesNotExist(
            Long chargeDefinitionId,
            Long roleId
    ) {
        Objects.requireNonNull(roleId, "roleId cannot be null");

        if (chargeAssignmentRepository.existsRoleAssignmentByChargeDefinitionIdAndRoleId(
                chargeDefinitionId,
                roleId
        )) {
            throw new ChargeAssignmentAlreadyExistsException(
                    "Charge assignment already exists."
            );
        }
    }

    private void ensureEventParticipantsAssignmentDoesNotExist(
            Long chargeDefinitionId,
            Long eventId
    ) {
        Objects.requireNonNull(eventId, "eventId cannot be null");

        if (chargeAssignmentRepository.existsEventParticipantsAssignmentByChargeDefinitionIdAndEventId(
                chargeDefinitionId,
                eventId
        )) {
            throw new ChargeAssignmentAlreadyExistsException(
                    "Charge assignment already exists."
            );
        }
    }

    private ChargeAssignment findChargeAssignmentOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return chargeAssignmentRepository.findById(id)
                .orElseThrow(() -> new ChargeAssignmentNotFoundException(
                        "Charge assignment not found."
                ));
    }

    private void ensureActiveMemberExists(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        if (!billingMembershipPort.existsActiveMemberByUserId(userId)) {
            throw new BillingAssignmentTargetNotFoundException(
                    "Active member not found for user."
            );
        }
    }

    private void ensureActiveRoleExists(Long roleId) {
        Objects.requireNonNull(roleId, "roleId cannot be null");

        if (!billingAuthorizationPort.existsActiveRoleById(roleId)) {
            throw new BillingAssignmentTargetNotFoundException(
                    "Active role not found."
            );
        }
    }

    private void ensureChargeDefinitionExists(Long chargeDefinitionId) {
        Objects.requireNonNull(chargeDefinitionId, "chargeDefinitionId cannot be null");

        chargeDefinitionRepository.findById(chargeDefinitionId)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException(
                        "Charge definition not found."
                ));
    }

    private void ensureChargeDefinitionIsNotArchived(Long chargeDefinitionId) {
        Objects.requireNonNull(chargeDefinitionId, "chargeDefinitionId cannot be null");

        ChargeDefinition chargeDefinition = chargeDefinitionRepository.findById(chargeDefinitionId)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException(
                        "Charge definition not found."
                ));

        if (chargeDefinition.getStatus() == ChargeDefinitionStatus.ARCHIVED) {
            throw new ChargeDefinitionCannotChangeAssignmentsException(
                    "Archived charge definitions cannot have assignments changed."
            );
        }
    }
}