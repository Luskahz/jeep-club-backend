package com.jeepclub.backend.dependents.system;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.dependents.core.application.exception.DependentCpfAlreadyInUseException;
import com.jeepclub.backend.dependents.core.application.exception.DependentOwnerInactiveException;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentHistoryEntity;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentHistoryJpaRepository;
import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import com.jeepclub.backend.health.core.port.MedicalProfileActiveOwnersQuery;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatusChecker;
import com.jeepclub.backend.iam.identity.api.module.UserStatus;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.iam.identity.infra.persistence.jpa.UserJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:dependents_system_test;DB_CLOSE_DELAY=-1;MODE=MySQL;NON_KEYWORDS=USER")
@ActiveProfiles("test")
@Import(DependentSystemFlowTest.TimeConfiguration.class)
class DependentSystemFlowTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    @Autowired
    private DependentService dependentService;
    @Autowired
    private UserJpaRepository identityJpaRepository;
    @Autowired
    private DependentJpaRepository dependentJpaRepository;
    @Autowired
    private DependentHistoryJpaRepository historyJpaRepository;
    @Autowired
    private TestClock clock;
    @Autowired
    private DependentsQuery dependentsQuery;
    @Autowired
    private MedicalProfileOwnerStatusChecker ownerStatus;
    @Autowired
    private MedicalProfileActiveOwnersQuery activeOwners;
    @Autowired
    private DependentOwnershipChecker ownership;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        clock.now = NOW;
        historyJpaRepository.deleteAll();
        dependentJpaRepository.deleteAll();
        identityJpaRepository.deleteAll();
        user = identityJpaRepository.saveAndFlush(user());
    }

    @Test
    void createDisableEnableUpdateAndDeleteDependent() {
        DependentResult created = dependentService.create(
                "Pedro Silva", "529.982.247-25", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, "11988887777", user.getId()
        );

        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(created.updatedAt()).isNull();
        assertThat(dependentService.findById(created.id(), user.getId())).isEqualTo(created);
        assertThat(dependentService.findAllByUserId(user.getId())).extracting(DependentResult::id)
                .containsExactly(created.id());
        assertThat(dependentService.findAllByUserId(-1L)).isEmpty();
        assertOwnerBoundary(created.id(), MedicalProfileOwnerStatus.ACTIVE);
        assertThat(ownership.belongsToUser(created.id(), user.getId())).isTrue();
        assertThat(ownership.belongsToUser(created.id(), -1L)).isFalse();

        clock.now = NOW.plusSeconds(10);
        assertThat(dependentService.disable(created.id(), user.getId()).status())
                .isEqualTo(DependentStatus.DISABLED);
        assertOwnerBoundary(created.id(), MedicalProfileOwnerStatus.INACTIVE);
        assertThat(dependentService.findAllByUserId(user.getId())).isEmpty();
        assertThat(ownership.belongsToUser(created.id(), user.getId())).isFalse();
        clock.now = NOW.plusSeconds(20);
        assertThat(dependentService.enable(created.id(), user.getId()).status())
                .isEqualTo(DependentStatus.ACTIVE);

        clock.now = NOW.plusSeconds(30);
        DependentResult updated = dependentService.update(
                created.id(), "Pedro Ramos", "111.444.777-35",
                LocalDate.of(2010, 5, 20), RelationshipType.CHILD,
                null, user.getId()
        );
        assertThat(updated.name()).isEqualTo("Pedro Ramos");
        assertThat(updated.createdAt()).isEqualTo(NOW);
        assertThat(updated.updatedAt()).isEqualTo(NOW.plusSeconds(30));

        clock.now = NOW.plusSeconds(40);
        dependentService.delete(created.id(), user.getId());

        assertThat(dependentJpaRepository.findById(created.id())).isEmpty();
        assertThat(historyJpaRepository.findAll()).singleElement()
                .satisfies(history -> {
                    assertThat(history.getDependentId()).isEqualTo(created.id());
                    assertThat(history.getCreatedAt()).isEqualTo(NOW);
                    assertThat(history.getUpdatedAt()).isEqualTo(NOW.plusSeconds(30));
                    assertThat(history.getDeletedAt()).isEqualTo(NOW.plusSeconds(40));
                });
        assertOwnerBoundary(created.id(), MedicalProfileOwnerStatus.NOT_FOUND);
        assertThat(ownership.belongsToUser(created.id(), user.getId())).isFalse();
    }

    @Test
    void failureWhileSavingHistoryRollsBackOperationalDelete() {
        DependentResult created = dependentService.create(
                "Pedro Silva", "529.982.247-25", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, null, user.getId()
        );
        historyJpaRepository.saveAndFlush(history(created));

        assertThatThrownBy(() -> dependentService.delete(created.id(), user.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(dependentJpaRepository.findById(created.id())).isPresent();
        assertThat(historyJpaRepository.findAll()).hasSize(1);
    }

    private void assertOwnerBoundary(Long id, MedicalProfileOwnerStatus expected) {
        assertThat(ownerStatus.getStatus(MedicalProfileOwnerType.DEPENDENT, id)).isEqualTo(expected);
        assertThat(dependentsQuery.existsById(id)).isEqualTo(expected != MedicalProfileOwnerStatus.NOT_FOUND);
        assertThat(dependentsQuery.existsActiveById(id)).isEqualTo(expected == MedicalProfileOwnerStatus.ACTIVE);
        assertThat(activeOwners.findActiveOwnerIds(MedicalProfileOwnerType.DEPENDENT, List.of(id, -1L)))
                .containsExactlyInAnyOrderElementsOf(expected == MedicalProfileOwnerStatus.ACTIVE ? List.of(id) : List.of());
    }

    @Test
    void identityBoundaryRejectsInactiveOwnerAndCpfAlreadyRegisteredAsUser() {
        assertThatThrownBy(() -> dependentService.create("Pedro", user.getCpf(), LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, null, user.getId()))
                .isInstanceOf(DependentCpfAlreadyInUseException.class);
        user.setStatus(UserStatus.DISABLED);
        identityJpaRepository.saveAndFlush(user);
        assertThatThrownBy(() -> dependentService.create("Pedro", "52998224725", LocalDate.of(2010, 5, 20),
                RelationshipType.CHILD, null, user.getId()))
                .isInstanceOf(DependentOwnerInactiveException.class);
        assertThat(dependentJpaRepository.count()).isZero();
    }

    private DependentHistoryEntity history(DependentResult dependent) {
        DependentHistoryEntity history = new DependentHistoryEntity();
        history.setDependentId(dependent.id());
        history.setName(dependent.name());
        history.setCpf(dependent.cpf());
        history.setBirthDate(dependent.birthDate());
        history.setRelationshipType(dependent.relationshipType());
        history.setPhoneNumber(dependent.phoneNumber());
        history.setUserId(dependent.userId());
        history.setStatus(dependent.status());
        history.setDeletedByUserId(user.getId());
        history.setCreatedAt(dependent.createdAt());
        history.setUpdatedAt(dependent.updatedAt());
        history.setDeletedAt(NOW.plusSeconds(60));
        return history;
    }

    private UserEntity user() {
        UserEntity entity = new UserEntity();
        entity.setName("Titular Fluxo");
        entity.setCpf("39053344705");
        entity.setStatus(UserStatus.ACTIVE);
        entity.setCreatedAt(NOW);
        return entity;
    }

    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        TestClock dependentTestClock() { return new TestClock(); }
    }

    static class TestClock extends Clock {
        private Instant now = NOW;
        @Override
        public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override
        public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override
        public Instant instant() { return now; }
    }
}
