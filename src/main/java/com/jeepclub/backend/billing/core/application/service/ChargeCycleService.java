package com.jeepclub.backend.billing.core.application.service;


import com.jeepclub.backend.billing.core.application.exception.chargeCycle.ChargeCycleAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeCycle.ChargeCycleNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.chargeCycle.ChargeCycleWithoutAssignmentsException;
import com.jeepclub.backend.billing.core.application.exception.chargeCycle.InactiveChargeDefinitionException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.result.cycle.ChargeCycleResult;
import com.jeepclub.backend.billing.core.application.result.cycle.GenerateChargeCycleResult;
import com.jeepclub.backend.billing.core.domain.enums.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.domain.model.assignment.AllMembersChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.EventParticipantsChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.RoleChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.UserChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.port.BillingAuthorizationPort;
import com.jeepclub.backend.billing.core.port.BillingEventPort;
import com.jeepclub.backend.billing.core.port.BillingMembershipPort;
import com.jeepclub.backend.billing.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChargeCycleService {

    private final ChargeCycleRepository chargeCycleRepository;
    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final ChargeAssignmentRepository chargeAssignmentRepository;
    private final MemberChargeRepository memberChargeRepository;
    private final BillingMembershipPort billingMembershipPort;
    private final BillingAuthorizationPort billingAuthorizationPort;
    private final BillingEventPort billingEventPort;
    private final Clock clock;
    private final MemberPaymentRepository memberPaymentRepository;

    @Transactional
    public GenerateChargeCycleResult generate(
            Long chargeDefinitionId,
            String code,
            LocalDate dueDate,
            Long generatedByUserId
    ) {
        ChargeDefinition chargeDefinition = findChargeDefinitionOrThrow(chargeDefinitionId);

        if (!chargeDefinition.isActive()) {
            throw new InactiveChargeDefinitionException(
                    "Only active charge definitions can generate cycles."
            );
        }

        String normalizedCode = normalizeCode(code);

        if (chargeCycleRepository.existsByChargeDefinitionIdAndCode(chargeDefinitionId, normalizedCode)) {
            throw new ChargeCycleAlreadyExistsException(
                    "Charge cycle already exists for this charge definition."
            );
        }
        Set<Long> targetUserIds = resolveTargetUserIds(chargeDefinitionId);

        if (targetUserIds.isEmpty()) {
            throw new ChargeCycleWithoutAssignmentsException(
                    "Charge cycle cannot be generated without eligible target users."
            );
        }

        Instant now = Instant.now(clock);

        ChargeCycle chargeCycle = ChargeCycle.generate(
                chargeDefinition,
                normalizedCode,
                dueDate,
                generatedByUserId,
                now
        );

        ChargeCycle savedChargeCycle = chargeCycleRepository.save(chargeCycle);

        int createdMemberCharges = createMemberCharges(
                chargeDefinition,
                savedChargeCycle,
                targetUserIds,
                now
        );

        return new GenerateChargeCycleResult(
                ChargeCycleResult.from(savedChargeCycle),
                createdMemberCharges
        );
    }

    @Transactional(readOnly = true)
    public Page<ChargeCycleResult> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    ) {
        Objects.requireNonNull(pageable, "pageable cannot be null");

        ensureChargeDefinitionExists(chargeDefinitionId);

        return chargeCycleRepository.findByChargeDefinitionId(chargeDefinitionId, pageable)
                .map(ChargeCycleResult::from);
    }

    @Transactional(readOnly = true)
    public ChargeCycleResult findById(Long id) {
        ChargeCycle chargeCycle = findChargeCycleOrThrow(id);

        return ChargeCycleResult.from(chargeCycle);
    }

    @Transactional
    public ChargeCycleResult cancel(
            Long id,
            Long canceledByUserId
    ) {
        Objects.requireNonNull(canceledByUserId, "canceledByUserId cannot be null");

        ChargeCycle chargeCycle = findChargeCycleOrThrow(id);
        Instant now = Instant.now(clock);

        chargeCycle.cancel(canceledByUserId, now);

        cancelOpenMemberCharges(id, now);

        ChargeCycle savedChargeCycle = chargeCycleRepository.save(chargeCycle);

        return ChargeCycleResult.from(savedChargeCycle);
    }

    private void cancelOpenMemberCharges(
            Long chargeCycleId,
            Instant now
    ) {
        List<MemberCharge> openMemberCharges = memberChargeRepository.findOpenByChargeCycleId(chargeCycleId);

        for (MemberCharge memberCharge : openMemberCharges) {
            cancelPendingPaymentsForOpenCharge(memberCharge.getId(), now);

            memberCharge.cancel(now);

            memberChargeRepository.save(memberCharge);
        }
    }

    private void cancelPendingPaymentsForOpenCharge(
            Long memberChargeId,
            Instant now
    ) {
        List<MemberPayment> pendingPayments = memberPaymentRepository.findByMemberChargeIdAndStatus(
                memberChargeId,
                MemberPaymentStatus.PENDING_VALIDATION
        );

        for (MemberPayment memberPayment : pendingPayments) {
            memberPayment.cancel(now);
            memberPaymentRepository.save(memberPayment);
        }
    }



    private int createMemberCharges(
            ChargeDefinition chargeDefinition,
            ChargeCycle chargeCycle,
            Set<Long> targetUserIds,
            Instant now
    ) {
        int created = 0;

        for (Long userId : targetUserIds) {
            if (memberChargeRepository.existsByUserIdAndChargeCycleId(userId, chargeCycle.getId())) {
                continue;
            }

            MemberCharge memberCharge = MemberCharge.create(
                    userId,
                    chargeDefinition.getId(),
                    chargeCycle.getId(),
                    chargeDefinition.getDefaultAmount(),
                    chargeCycle.getDueDate(),
                    now
            );

            memberChargeRepository.save(memberCharge);
            created++;
        }

        return created;
    }

    private Set<Long> resolveTargetUserIds(Long chargeDefinitionId) {
        List<ChargeAssignment> assignments = chargeAssignmentRepository
                .findByChargeDefinitionId(chargeDefinitionId, Pageable.unpaged())
                .stream()
                .filter(ChargeAssignment::isActive)
                .toList();

        Set<Long> targetUserIds = new LinkedHashSet<>();

        for (ChargeAssignment assignment : assignments) {
            if (assignment instanceof AllMembersChargeAssignment) {
                targetUserIds.addAll(billingMembershipPort.findActiveMemberUserIds());
                continue;
            }

            if (assignment instanceof UserChargeAssignment userAssignment) {
                Long userId = userAssignment.getUserId();

                if (billingMembershipPort.existsActiveMemberByUserId(userId)) {
                    targetUserIds.add(userId);
                }

                continue;
            }

            if (assignment instanceof RoleChargeAssignment roleAssignment) {
                List<Long> userIdsByRole = billingAuthorizationPort.findUserIdsByRoleId(
                        roleAssignment.getRoleId()
                );

                userIdsByRole.stream()
                        .filter(billingMembershipPort::existsActiveMemberByUserId)
                        .forEach(targetUserIds::add);

                continue;
            }

            if (assignment instanceof EventParticipantsChargeAssignment eventAssignment) {
                targetUserIds.addAll(
                        billingEventPort.findConfirmedParticipantUserIdsByEventId(
                                eventAssignment.getEventId()
                        )
                );

                continue;
            }

            throw new IllegalArgumentException(
                    "Unsupported charge assignment type: " + assignment.getClass().getName()
            );
        }

        return targetUserIds;
    }

    private ChargeDefinition findChargeDefinitionOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return chargeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException(
                        "Charge definition not found."
                ));
    }

    private void ensureChargeDefinitionExists(Long id) {
        findChargeDefinitionOrThrow(id);
    }

    private ChargeCycle findChargeCycleOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return chargeCycleRepository.findById(id)
                .orElseThrow(() -> new ChargeCycleNotFoundException(
                        "Charge cycle not found."
                ));
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be blank.");
        }

        return code.trim();
    }

}