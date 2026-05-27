package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.result.ChargeAssignmentResult;
import com.jeepclub.backend.billing.core.domain.model.assignment.AllMembersChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.EventParticipantsChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.RoleChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.UserChargeAssignment;
import com.jeepclub.backend.billing.core.port.BillingEventPort;
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
    private final Clock clock;

    @Transactional
    public ChargeAssignmentResult assignToAllMembers(Long chargeDefinitionId) {
        ensureChargeDefinitionExists(chargeDefinitionId);
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
        ensureChargeDefinitionExists(chargeDefinitionId);
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
        ensureChargeDefinitionExists(chargeDefinitionId);
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
        ensureChargeDefinitionExists(chargeDefinitionId);
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

        chargeAssignment.activate(Instant.now(clock));

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    @Transactional
    public ChargeAssignmentResult deactivate(Long id) {
        ChargeAssignment chargeAssignment = findChargeAssignmentOrThrow(id);

        chargeAssignment.deactivate(Instant.now(clock));

        ChargeAssignment savedChargeAssignment = chargeAssignmentRepository.save(chargeAssignment);

        return ChargeAssignmentResult.from(savedChargeAssignment);
    }

    private void ensureChargeDefinitionExists(Long chargeDefinitionId) {
        Objects.requireNonNull(chargeDefinitionId, "chargeDefinitionId cannot be null");

        if (chargeDefinitionRepository.findById(chargeDefinitionId).isEmpty()) {
            throw new ChargeDefinitionNotFoundException("Charge definition not found.");
        }
    }

    private void ensureEventExists(Long eventId) {
        Objects.requireNonNull(eventId, "eventId cannot be null");

        if (!billingEventPort.existsEventById(eventId)) {
            throw new IllegalArgumentException("Event not found.");
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
}