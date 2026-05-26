package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeAssignment.ChargeAssignmentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.result.ChargeAssignmentResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeAssignmentType;
import com.jeepclub.backend.billing.core.domain.model.ChargeAssignment;
import com.jeepclub.backend.billing.core.repository.ChargeAssignmentRepository;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChargeAssignmentService {

    private final ChargeAssignmentRepository chargeAssignmentRepository;
    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final Clock clock;

    @Transactional
    public ChargeAssignmentResult assignToAllMembers(Long chargeDefinitionId) {
        ensureChargeDefinitionExists(chargeDefinitionId);
        ensureAssignmentDoesNotExist(
                chargeDefinitionId,
                ChargeAssignmentType.ALL_MEMBERS,
                null
        );

        ChargeAssignment chargeAssignment = ChargeAssignment.assignToAllMembers(
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
        ensureAssignmentDoesNotExist(
                chargeDefinitionId,
                ChargeAssignmentType.USER,
                userId
        );

        ChargeAssignment chargeAssignment = ChargeAssignment.assignToUser(
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
        ensureAssignmentDoesNotExist(
                chargeDefinitionId,
                ChargeAssignmentType.ROLE,
                roleId
        );

        ChargeAssignment chargeAssignment = ChargeAssignment.assignToRole(
                chargeDefinitionId,
                roleId,
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

        if (!chargeDefinitionRepository.findById(chargeDefinitionId).isPresent()) {
            throw new ChargeDefinitionNotFoundException("Charge definition not found.");
        }
    }

    private void ensureAssignmentDoesNotExist(
            Long chargeDefinitionId,
            ChargeAssignmentType assignmentType,
            Long targetId
    ) {
        if (chargeAssignmentRepository.existsByChargeDefinitionIdAndAssignmentTypeAndTargetId(
                chargeDefinitionId,
                assignmentType,
                targetId
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