package com.jeepclub.backend.vehicles.infra.persistence.adapter;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.exception.VehicleAlreadyDeletedException;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleHistoryEntity;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleHistoryJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleHistoryMapper;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({
        VehicleRepositoryAdapter.class,
        VehicleHistoryMapper.class
})
class VehicleRepositoryAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-31T12:00:00Z");

    @Autowired
    private VehicleRepositoryAdapter repository;
    @Autowired
    private VehicleJpaRepository vehicleJpaRepository;
    @Autowired
    private VehicleHistoryJpaRepository historyJpaRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void operationalColumnMetadataReflectsHardenedNullabilityAndLength() throws SQLException {
        assertColumn("VEHICLES_VEHICLE", "NICKNAME", true, 100);
        assertColumn("VEHICLES_VEHICLE", "PHOTO", true, 500);
        assertColumn("VEHICLES_VEHICLE", "PLATE", false, 7);
        assertColumn("VEHICLES_VEHICLE", "RENAVAM", false, 11);
        assertColumn("VEHICLES_VEHICLE", "BRAND", false, 50);
        assertColumn("VEHICLES_VEHICLE", "MODEL", false, 100);
        assertColumn("VEHICLES_VEHICLE", "COLOR", true, 30);
        assertColumnNullable("VEHICLES_VEHICLE", "SEATING_CAPACITY", false);
        assertColumnNullable("VEHICLES_VEHICLE", "ENGINE_DISPLACEMENT", false);
        assertColumnNullable("VEHICLES_VEHICLE", "TOWING", false);
        assertColumnNullable("VEHICLES_VEHICLE", "OWNER_ID", false);
        assertColumnNullable("VEHICLES_VEHICLE", "CREATED_AT", false);
        assertColumnNullable("VEHICLES_VEHICLE", "UPDATED_AT", true);
    }

    @Test
    void historyColumnMetadataReflectsHardenedSnapshotNullabilityAndLength() throws SQLException {
        assertColumn("VEHICLES_VEHICLE_HISTORY", "NICKNAME", true, 100);
        assertColumn("VEHICLES_VEHICLE_HISTORY", "PHOTO", true, 500);
        assertColumn("VEHICLES_VEHICLE_HISTORY", "PLATE", false, 7);
        assertColumn("VEHICLES_VEHICLE_HISTORY", "RENAVAM", false, 11);
        assertColumn("VEHICLES_VEHICLE_HISTORY", "BRAND", false, 50);
        assertColumn("VEHICLES_VEHICLE_HISTORY", "MODEL", false, 100);
        assertColumn("VEHICLES_VEHICLE_HISTORY", "COLOR", true, 30);
        assertColumnNullable("VEHICLES_VEHICLE_HISTORY", "SEATING_CAPACITY", false);
        assertColumnNullable("VEHICLES_VEHICLE_HISTORY", "ENGINE_DISPLACEMENT", false);
        assertColumnNullable("VEHICLES_VEHICLE_HISTORY", "TOWING", false);
        assertColumnNullable("VEHICLES_VEHICLE_HISTORY", "UPDATED_AT", true);
        assertColumnNullable("VEHICLES_VEHICLE_HISTORY", "DELETED_BY_USER_ID", false);
        assertColumnNullable("VEHICLES_VEHICLE_HISTORY", "DELETED_AT", false);
    }

    @Test
    void savingEntityWithNullTowingViolatesNotNullConstraint() {
        // O domínio (Vehicle.create/update/reconstitute) já rejeita towing nulo;
        // este teste prova que a coluna também recusa o estado inválido para
        // qualquer chamador que grave a entity diretamente, bypassando o domínio.
        VehicleEntity entity = VehicleMapper.toEntity(vehicle("ABC1D23", "38249206428"));
        entity.setTowing(null);

        assertThatThrownBy(() -> vehicleJpaRepository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void assertColumn(String table, String column, boolean nullable, int length) throws SQLException {
        assertColumnNullable(table, column, nullable);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select character_maximum_length from information_schema.columns "
                             + "where table_name = ? and column_name = ?"
             )) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getInt(1)).isEqualTo(length);
            }
        }
    }

    private void assertColumnNullable(String table, String column, boolean nullable) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "select is_nullable from information_schema.columns "
                             + "where table_name = ? and column_name = ?"
             )) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString(1)).isEqualTo(nullable ? "YES" : "NO");
            }
        }
    }

    @Test
    void deleteSavesHistoryAndRemovesOperationalEntity() {
        Vehicle saved = repository.save(vehicle("ABC1D23", "38249206428"));
        entityManager.flush();

        repository.delete(saved, 99L, NOW.plusSeconds(60));
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleJpaRepository.findById(saved.getId())).isEmpty();
        assertThat(historyJpaRepository.findAll()).singleElement()
                .satisfies(history -> {
                    assertThat(history.getId()).isNotNull();
                    assertThat(history.getVehicleId()).isEqualTo(saved.getId());
                    assertThat(history.getOwnerId()).isEqualTo(7L);
                    assertThat(history.getDeletedByUserId()).isEqualTo(99L);
                    assertThat(history.getDeletedAt()).isEqualTo(NOW.plusSeconds(60));
                    assertThat(history.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
                });
    }

    @Test
    void deleteRollsBackOperationalRemovalWhenHistorySnapshotFails() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        Long vehicleId = transaction.execute(status -> {
            Vehicle saved = repository.save(vehicle("ABC1D23", "38249206428"));
            historyJpaRepository.saveAndFlush(
                    history(saved.getId(), "POISON1", "00000000000")
            );
            return saved.getId();
        });

        assertThat(vehicleId).isNotNull();

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Vehicle saved = repository.findById(vehicleId).orElseThrow();
            repository.delete(saved, 99L, NOW.plusSeconds(60));
            entityManager.flush();
        })).isInstanceOf(DataIntegrityViolationException.class);

        transaction.executeWithoutResult(status -> {
            entityManager.clear();
            assertThat(vehicleJpaRepository.findById(vehicleId))
                    .as("o veículo operacional deve continuar existindo após o rollback")
                    .isPresent();
            assertThat(historyJpaRepository.findAll())
                    .as("a transação que falhou não pode persistir estado parcial")
                    .singleElement()
                    .satisfies(history -> assertThat(history.getVehicleId()).isEqualTo(vehicleId));
        });
    }

    @Test
    void repeatedDeleteLosingTheLockRaceIsTranslatedToAlreadyDeletedConflict() {
        Vehicle saved = repository.save(vehicle("ABC1D23", "38249206428"));
        entityManager.flush();

        repository.delete(saved, 99L, NOW.plusSeconds(60));
        entityManager.flush();
        entityManager.clear();

        // Simula uma segunda requisição de exclusão que perdeu a corrida pelo
        // lock: quando ela tenta localizar a linha operacional para excluir,
        // o veículo já não existe mais.
        assertThatThrownBy(() -> repository.delete(saved, 99L, NOW.plusSeconds(120)))
                .isInstanceOf(VehicleAlreadyDeletedException.class);

        assertThat(historyJpaRepository.findAll())
                .as("a segunda tentativa não deve gravar um segundo snapshot")
                .hasSize(1);
    }

    @Test
    void historyVehicleIdIsUniqueButIdentifiersCanBeReused() {
        historyJpaRepository.saveAndFlush(history(42L, "ABC1D23", "38249206428"));
        historyJpaRepository.saveAndFlush(history(43L, "ABC1D23", "38249206428"));

        assertThatThrownBy(() -> historyJpaRepository.saveAndFlush(
                history(42L, "XYZ9Z99", "12345678901")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void identifiersAreReleasedAfterOperationalDelete() {
        Vehicle saved = repository.save(vehicle("ABC1D23", "38249206428"));
        repository.delete(saved, 7L, NOW.plusSeconds(60));
        entityManager.flush();

        Vehicle replacement = repository.save(vehicle("ABC1D23", "38249206428"));

        assertThat(replacement.getId()).isNotEqualTo(saved.getId());
        assertThat(replacement.getPlate()).isEqualTo(saved.getPlate());
        assertThat(replacement.getRenavam()).isEqualTo(saved.getRenavam());
    }

    @Test
    void concurrentDuplicatePlateOnSaveIsTranslatedToBusinessConflict() {
        repository.save(vehicle("ABC1D23", "38249206428"));
        entityManager.flush();

        // Simula duas requisições que passaram na pré-checagem antes de qualquer
        // uma commitar: a constraint de banco é quem efetivamente decide.
        assertThatThrownBy(() -> repository.save(vehicle("ABC1D23", "12345678901")))
                .isInstanceOf(VehiclePlateAlreadyExistsException.class);
    }

    @Test
    void concurrentDuplicateRenavamOnSaveIsTranslatedToBusinessConflict() {
        repository.save(vehicle("ABC1D23", "38249206428"));
        entityManager.flush();

        assertThatThrownBy(() -> repository.save(vehicle("XYZ9Z99", "38249206428")))
                .isInstanceOf(VehicleRenavamAlreadyExistsException.class);
    }

    @Test
    void savePersistsCanonicalPlateAndRenavamEvenWhenDomainAlreadyNormalized() {
        Vehicle saved = repository.save(vehicle("ABC1D23", "38249206428"));
        entityManager.flush();
        entityManager.clear();

        VehicleEntity persisted = vehicleJpaRepository.findById(saved.getId()).orElseThrow();
        assertThat(persisted.getPlate()).isEqualTo("ABC1D23");
        assertThat(persisted.getRenavam()).isEqualTo("38249206428");
    }

    @Test
    void legacySoftDeletedRowIsReadWithoutMappingFailure() {
        // Nenhum caso de uso atual grava SOFT_DELETED; esta linha simula um
        // registro legado pré-existente inserido diretamente via JPA, sem
        // passar por Vehicle.create/save. O risco real é o Hibernate falhar
        // ao desserializar VehicleEntity.status; este teste prova que a
        // leitura continua segura mesmo assim.
        VehicleEntity legacy = new VehicleEntity();
        legacy.setNickname("Legado");
        legacy.setPhoto("photo");
        legacy.setPlate("LEG1D23");
        legacy.setRenavam("98765432100");
        legacy.setBrand("Jeep");
        legacy.setModel("Wrangler");
        legacy.setManufacturingYear(2020);
        legacy.setModelYear(2020);
        legacy.setColor("Preto");
        legacy.setSeatingCapacity(5);
        legacy.setFuelType(FuelType.DIESEL);
        legacy.setEngineDisplacement(2.0);
        legacy.setStatus(VehicleStatus.SOFT_DELETED);
        legacy.setTowing(true);
        legacy.setOwnerId(7L);
        VehicleEntity savedLegacy = vehicleJpaRepository.saveAndFlush(legacy);
        entityManager.clear();

        Vehicle loaded = repository.findById(savedLegacy.getId()).orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(VehicleStatus.SOFT_DELETED);
        assertThat(loaded.getPlate()).isEqualTo("LEG1D23");
    }

    private Vehicle vehicle(String plate, String renavam) {
        return Vehicle.create(
                "Trovão",
                "photo",
                plate,
                renavam,
                "Jeep",
                "Wrangler",
                2023,
                2024,
                "Verde",
                5,
                FuelType.DIESEL,
                2.0,
                true,
                7L,
                NOW
        );
    }

    private VehicleHistoryEntity history(
            Long vehicleId,
            String plate,
            String renavam
    ) {
        VehicleHistoryEntity history = new VehicleHistoryEntity();
        history.setVehicleId(vehicleId);
        history.setNickname("Trovão");
        history.setPhoto("photo");
        history.setPlate(plate);
        history.setRenavam(renavam);
        history.setBrand("Jeep");
        history.setModel("Wrangler");
        history.setManufacturingYear(2023);
        history.setModelYear(2024);
        history.setColor("Verde");
        history.setSeatingCapacity(5);
        history.setFuelType(FuelType.DIESEL);
        history.setEngineDisplacement(2.0);
        history.setStatus(VehicleStatus.ACTIVE);
        history.setTowing(true);
        history.setOwnerId(7L);
        history.setDeletedByUserId(99L);
        history.setCreatedAt(NOW);
        history.setDeletedAt(NOW.plusSeconds(60));
        return history;
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = VehicleJpaRepository.class)
    @EntityScan(basePackageClasses = VehicleEntity.class)
    static class TestConfiguration {
    }
}
