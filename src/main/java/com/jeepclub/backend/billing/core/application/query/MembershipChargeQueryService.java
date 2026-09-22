package com.jeepclub.backend.billing.core.application.query;

import com.jeepclub.backend.billing.api.module.MembershipChargeQuery;
import com.jeepclub.backend.billing.api.module.MembershipChargeResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;
import com.jeepclub.backend.billing.core.repository.ChargeCycleRepository;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeEffectiveStatus;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MembershipChargeQueryService implements MembershipChargeQuery {

    private final ChargeCycleRepository chargeCycleRepository;
    private final MemberChargeRepository memberChargeRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public MembershipChargeResult evaluate(Long chargeDefinitionId, Long userId) {
        validateId(chargeDefinitionId, "chargeDefinitionId");
        validateId(userId, "userId");

        LocalDate referenceDate = LocalDate.now(clock);

        return findRelevantCycle(chargeDefinitionId, referenceDate)
                .flatMap(cycle -> memberChargeRepository.findByChargeCycleIdAndUserId(cycle.getId(), userId))
                .map(charge -> map(charge.effectiveStatusAt(referenceDate)))
                .orElse(MembershipChargeResult.CHARGE_NOT_FOUND);
    }

    private Optional<ChargeCycle> findRelevantCycle(Long chargeDefinitionId, LocalDate referenceDate) {
        Comparator<ChargeCycle> relevanceOrder = (left, right) -> {
            boolean leftFuture = left.getDueDate().isAfter(referenceDate);
            boolean rightFuture = right.getDueDate().isAfter(referenceDate);
            if (leftFuture != rightFuture) {
                return leftFuture ? 1 : -1;
            }

            int byDueDate = leftFuture
                    ? left.getDueDate().compareTo(right.getDueDate())
                    : right.getDueDate().compareTo(left.getDueDate());
            if (byDueDate != 0) {
                return byDueDate;
            }

            return Comparator.nullsLast(Comparator.<Long>reverseOrder())
                    .compare(left.getId(), right.getId());
        };

        return chargeCycleRepository.findByChargeDefinitionId(chargeDefinitionId, Pageable.unpaged())
                .stream()
                .filter(cycle -> cycle.getStatus() != ChargeCycleStatus.ARCHIVED)
                .filter(cycle -> isInCurrentRecurrencePeriod(cycle, referenceDate))
                .min(relevanceOrder);
    }

    private static boolean isInCurrentRecurrencePeriod(ChargeCycle cycle, LocalDate referenceDate) {
        ChargeRecurrenceType recurrence = cycle.getChargeDefinitionRecurrenceTypeSnapshot();
        return switch (recurrence) {
            case ONE_TIME -> true;
            case MONTHLY -> YearMonth.from(cycle.getDueDate()).equals(YearMonth.from(referenceDate));
            case YEARLY -> cycle.getDueDate().getYear() == referenceDate.getYear();
        };
    }

    private static MembershipChargeResult map(MemberChargeEffectiveStatus status) {
        return switch (status) {
            case PENDING -> MembershipChargeResult.WITHIN_PAYMENT_PERIOD;
            case PAID -> MembershipChargeResult.SATISFIED;
            case CANCELED -> MembershipChargeResult.CANCELED;
            case OVERDUE, EXPIRED -> MembershipChargeResult.PAYMENT_REQUIRED;
        };
    }

    private static void validateId(Long id, String name) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(name + " must be greater than zero");
        }
    }
}
