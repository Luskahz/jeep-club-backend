package com.jeepclub.backend.vehicles.infra.persistence.adapter;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleHistoryEntity;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleHistoryJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleHistoryMapper;
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
