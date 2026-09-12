package com.jeepclub.backend.health.infra.integration.owner;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.MedicalProfileActiveOwnersQuery;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MedicalProfileOwnerStatusAdapter implements
        MedicalProfileOwnerStatusChecker,
        MedicalProfileActiveOwnersQuery {

    private final UserQuery userQuery;
    private final DependentsQuery dependentsQuery;

    @Override
    public MedicalProfileOwnerStatus getStatus(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        if (ownerType == null || ownerId == null || ownerId <= 0) {
            return MedicalProfileOwnerStatus.NOT_FOUND;
        }

        return switch (ownerType) {
            case USER -> userStatus(ownerId);
            case DEPENDENT -> dependentStatus(ownerId);
        };
    }

    @Override
    public Set<Long> findActiveOwnerIds(
            MedicalProfileOwnerType ownerType,
            Collection<Long> ownerIds
    ) {
        if (ownerType == null || ownerIds == null || ownerIds.isEmpty()) {
            return Set.of();
        }

        return switch (ownerType) {
            case USER -> userQuery
                    .findAdministrativelyActiveUserIdsByIds(ownerIds);
            case DEPENDENT -> dependentsQuery
                    .findActiveDependentIdsByIds(ownerIds);
        };
    }

    private MedicalProfileOwnerStatus userStatus(Long ownerId) {
        if (!userQuery.existsById(ownerId)) {
            return MedicalProfileOwnerStatus.NOT_FOUND;
        }

        return userQuery.isAdministrativelyActive(ownerId)
                ? MedicalProfileOwnerStatus.ACTIVE
                : MedicalProfileOwnerStatus.INACTIVE;
    }

    private MedicalProfileOwnerStatus dependentStatus(Long ownerId) {
        if (!dependentsQuery.existsById(ownerId)) {
            return MedicalProfileOwnerStatus.NOT_FOUND;
        }

        return dependentsQuery.existsActiveById(ownerId)
                ? MedicalProfileOwnerStatus.ACTIVE
                : MedicalProfileOwnerStatus.INACTIVE;
    }
}
