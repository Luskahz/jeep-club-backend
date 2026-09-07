package com.jeepclub.backend.health.infra.integration.owner;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MedicalProfileOwnerStatusAdapter implements MedicalProfileOwnerStatusChecker {

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
