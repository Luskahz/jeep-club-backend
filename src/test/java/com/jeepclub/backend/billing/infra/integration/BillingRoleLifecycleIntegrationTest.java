package com.jeepclub.backend.billing.infra.integration;

import com.jeepclub.backend.billing.core.application.exception.assignment.BillingAssignmentTargetNotFoundException;
import com.jeepclub.backend.billing.core.application.service.chargeassignment.AdminChargeAssignmentService;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.repository.ChargeAssignmentRepository;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind;
import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleEntity;
import com.jeepclub.backend.iam.authorization.infra.persistence.jpa.RoleJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BillingRoleLifecycleIntegrationTest {
    @Autowired AdminChargeAssignmentService service;
    @Autowired ChargeDefinitionRepository definitions;
    @Autowired ChargeAssignmentRepository assignments;
    @Autowired RoleJpaRepository roles;
    @Autowired PlatformTransactionManager manager;

    @Test void assignmentTraversesRealRoleLockAndRejectsUnavailableRoles() {
        var tx = new TransactionTemplate(manager);
        var ids = tx.execute(status -> {
            var now = Instant.now();
            var definition = definitions.save(ChargeDefinition.create("role lifecycle integration", null, BigDecimal.TEN,
                    ChargeRecurrenceType.ONE_TIME, true, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, now));
            var active = roles.save(new RoleEntity(null, "role-active-integration", null, RoleKind.CUSTOM, RoleStatus.ACTIVE, now, null, null));
            var inactive = roles.save(new RoleEntity(null, "role-inactive-integration", null, RoleKind.CUSTOM, RoleStatus.INACTIVE, now, null, null));
            var archived = roles.save(new RoleEntity(null, "role-archived-integration", null, RoleKind.CUSTOM, RoleStatus.ARCHIVED, now, null, null, now));
            return java.util.List.of(definition.getId(), active.getId(), inactive.getId(), archived.getId());
        });
        var assigned = service.assignToRole(ids.get(0), ids.get(1));
        assertThat(assigned.roleId()).isEqualTo(ids.get(1));
        for (Long roleId : java.util.List.of(ids.get(2), ids.get(3), Long.MAX_VALUE)) {
            assertThatThrownBy(() -> service.assignToRole(ids.get(0), roleId)).isInstanceOf(BillingAssignmentTargetNotFoundException.class);
            assertThat((Boolean) tx.execute(status -> assignments.existsRoleAssignmentByChargeDefinitionIdAndRoleId(ids.get(0), roleId))).isFalse();
        }
        assertThat((Boolean) tx.execute(status -> assignments.existsRoleAssignmentByChargeDefinitionIdAndRoleId(ids.get(0), ids.get(1)))).isTrue();
    }
}
