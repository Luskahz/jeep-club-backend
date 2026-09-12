package com.jeepclub.backend.health.infra.persistence;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileConflictException;
import com.jeepclub.backend.health.core.application.service.medicalprofile.AdminMedicalProfileService;
import com.jeepclub.backend.health.core.application.service.medicalprofile.MedicalProfileService;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.core.port.MedicalProfileOwnerStatus;
import com.jeepclub.backend.health.infra.persistence.adapter.MedicalProfileRepositoryAdapter;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileHistoryJpaRepository;
import com.jeepclub.backend.health.infra.persistence.jpa.MedicalProfileJpaRepository;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileHistoryMapper;
import com.jeepclub.backend.health.infra.persistence.mapper.MedicalProfileMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = MedicalProfileConcurrencyIntegrationTest.JpaTestConfiguration.class)
@Import({
        MedicalProfileRepositoryAdapter.class,
        MedicalProfileMapper.class,
        MedicalProfileHistoryMapper.class
})
class MedicalProfileConcurrencyIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-07T12:00:00Z");
    private static final Long CREATE_OWNER_ID = 90_001L;
    private static final Long UPDATE_OWNER_ID = 90_002L;

    @Autowired
    private MedicalProfileRepositoryAdapter repository;
    @Autowired
    private MedicalProfileJpaRepository jpaRepository;
    @Autowired
    private MedicalProfileHistoryJpaRepository historyJpaRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            historyJpaRepository.deleteAllInBatch();
            jpaRepository.deleteAllInBatch();
        });
    }

    @Test
    void concurrentCreationKeepsOneProfileAndReturnsAFunctionalConflict() throws Exception {
        CountDownLatch bothReadMissingProfile = new CountDownLatch(2);
        CountDownLatch startInsert = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<MedicalProfile> first = executor.submit(() -> createAfterConcurrentRead(
                    "Alergia da primeira requisição",
                    bothReadMissingProfile,
                    startInsert
            ));
            Future<MedicalProfile> second = executor.submit(() -> createAfterConcurrentRead(
                    "Alergia da segunda requisição",
                    bothReadMissingProfile,
                    startInsert
            ));

            assertThat(bothReadMissingProfile.await(5, TimeUnit.SECONDS)).isTrue();
            startInsert.countDown();

            List<Object> outcomes = List.of(outcome(first), outcome(second));

            assertThat(outcomes).filteredOn(MedicalProfile.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(MedicalProfileConflictException.class::isInstance)
                    .hasSize(1);
            Long persistedProfiles = transactionTemplate.execute(status -> jpaRepository
                    .findAll()
                    .stream()
                    .filter(entity -> entity.getOwnerType() == MedicalProfileOwnerType.USER)
                    .filter(entity -> CREATE_OWNER_ID.equals(entity.getOwnerId()))
                    .count());
            assertThat(persistedProfiles).isEqualTo(1L);
        } finally {
            startInsert.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void memberAndAdminUpdatesAreSerializedWithoutSilentLostUpdate() throws Exception {
        transactionTemplate.execute(status -> repository.save(profile(
                UPDATE_OWNER_ID,
                "Alergia inicial"
        )));

        MedicalProfileService memberService = new MedicalProfileService(
                repository,
                (dependentId, userId) -> true,
                (ownerType, ownerId) -> MedicalProfileOwnerStatus.ACTIVE,
                Clock.fixed(NOW.plusSeconds(10), ZoneOffset.UTC)
        );
        AdminMedicalProfileService adminService = new AdminMedicalProfileService(
                repository,
                (ownerType, ownerId) -> MedicalProfileOwnerStatus.ACTIVE,
                (ownerType, ownerIds) -> java.util.Set.of(),
                Clock.fixed(NOW.plusSeconds(20), ZoneOffset.UTC)
        );

        CountDownLatch memberUpdatedWhileLockIsHeld = new CountDownLatch(1);
        CountDownLatch releaseMemberTransaction = new CountDownLatch(1);
        CountDownLatch adminStarted = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<MedicalProfile> member = executor.submit(() -> transactionTemplate.execute(status -> {
                MedicalProfile updated = memberService.upsertMyMedicalProfile(
                        UPDATE_OWNER_ID,
                        command("Alergia informada pelo membro")
                );
                memberUpdatedWhileLockIsHeld.countDown();
                await(releaseMemberTransaction);
                return updated;
            }));

            assertThat(memberUpdatedWhileLockIsHeld.await(5, TimeUnit.SECONDS)).isTrue();

            Future<MedicalProfile> admin = executor.submit(() -> {
                adminStarted.countDown();
                return transactionTemplate.execute(status -> adminService.upsertByOwner(
                        MedicalProfileOwnerType.USER,
                        UPDATE_OWNER_ID,
                        command("Alergia informada pelo administrador")
                ));
            });

            assertThat(adminStarted.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(200);
            assertThat(admin).as("a atualização administrativa deve aguardar o lock do membro")
                    .isNotDone();

            releaseMemberTransaction.countDown();

            assertThat(member.get(5, TimeUnit.SECONDS).getAllergies())
                    .isEqualTo("Alergia informada pelo membro");
            assertThat(admin.get(5, TimeUnit.SECONDS).getAllergies())
                    .isEqualTo("Alergia informada pelo administrador");

            MedicalProfile persisted = transactionTemplate.execute(status -> repository
                    .findByOwner(MedicalProfileOwnerType.USER, UPDATE_OWNER_ID)
                    .orElseThrow());
            assertThat(persisted.getAllergies())
                    .isEqualTo("Alergia informada pelo administrador");
        } finally {
            releaseMemberTransaction.countDown();
            executor.shutdownNow();
        }
    }

    private MedicalProfile createAfterConcurrentRead(
            String allergies,
            CountDownLatch bothReadMissingProfile,
            CountDownLatch startInsert
    ) {
        return transactionTemplate.execute(status -> {
            assertThat(repository.findByOwnerForUpdate(
                    MedicalProfileOwnerType.USER,
                    CREATE_OWNER_ID
            )).isEmpty();
            bothReadMissingProfile.countDown();
            await(startInsert);
            return repository.save(profile(CREATE_OWNER_ID, allergies));
        });
    }

    private Object outcome(Future<MedicalProfile> future) throws Exception {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            return exception.getCause();
        }
    }

    private MedicalProfile profile(Long ownerId, String allergies) {
        return MedicalProfile.create(
                MedicalProfileOwnerType.USER,
                ownerId,
                BloodType.O_POSITIVE,
                allergies,
                "Asma",
                "Bombinha",
                "Unimed",
                "Enfermaria",
                "123456789",
                "Maria",
                "12999999999",
                "Mãe",
                "Observação",
                NOW
        );
    }

    private UpsertMedicalProfileCommand command(String allergies) {
        return new UpsertMedicalProfileCommand(
                BloodType.A_POSITIVE,
                allergies,
                "Asma",
                "Bombinha",
                "Unimed",
                "Apartamento",
                "987654321",
                "João",
                "11999999999",
                "Pai",
                "Atualizado"
        );
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo esgotado aguardando sincronização do teste.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Teste concorrente interrompido.", exception);
        }
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = MedicalProfileJpaRepository.class)
    @EntityScan(basePackageClasses = MedicalProfileEntity.class)
    static class JpaTestConfiguration {
    }
}
