package com.jeepclub.backend.vehicles.core.application.service.vehicle;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Mock
    private VehicleRepository vehicleRepository;

    private VehicleService service;

    @BeforeEach
    void setUp() {
        service = new VehicleService(
                vehicleRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsVehicleForAuthenticatedOwner() {
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle vehicle = createVehicle();

        assertThat(vehicle.getOwnerId()).isEqualTo(7L);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
        assertThat(vehicle.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void preservesPlateUniquenessValidation() {
        when(vehicleRepository.existsByPlate("ABC1D23")).thenReturn(true);

        assertThatThrownBy(this::createVehicle)
                .isInstanceOf(VehiclePlateAlreadyExistsException.class)
                .hasMessage("The license PLATE provided is already registered.");
    }

    @Test
    void listsAndFindsOnlyActiveOwnedVehicles() {
        var pageable = PageRequest.of(0, 10);
        Vehicle vehicle = vehicle(1L, 7L, VehicleStatus.ACTIVE);
        when(vehicleRepository.findAllByOwnerIdAndStatus(
                7L, VehicleStatus.ACTIVE, pageable
        )).thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle));

        assertThat(service.findAll(7L, pageable).getContent()).containsExactly(vehicle);
        assertThat(service.findById(1L, 7L)).isSameAs(vehicle);
    }

    @Test
    void hidesSoftDeletedVehicle() {
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle(1L, 7L, VehicleStatus.SOFT_DELETED)));

        assertThatThrownBy(() -> service.findById(1L, 7L))
                .isInstanceOf(VehicleIdNotFoundException.class)
                .hasMessage("Vehicle not found.");
    }

    @Test
    void updatesAndSoftDeletesOwnedVehicle() {
        Vehicle vehicle = vehicle(1L, 7L, VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle));

        service.update(
                1L, 7L, "Novo", "photo", "ABC1D23", "38249206428",
                "Jeep", "Renegade", 2023, 2024, "Preto", 5,
                FuelType.FLEX, 1.8, true
        );

        assertThat(vehicle.getNickname()).isEqualTo("Novo");
        assertThat(vehicle.getUpdatedAt()).isEqualTo(NOW);
        verify(vehicleRepository).save(vehicle);

        service.delete(1L, 7L);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.SOFT_DELETED);
        assertThat(vehicle.getDeletedAt()).isEqualTo(NOW);
    }

    private Vehicle createVehicle() {
        return service.create(
                "Trovão", "photo", "ABC1D23", "38249206428", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, true, 7L
        );
    }

    private Vehicle vehicle(Long id, Long ownerId, VehicleStatus status) {
        return Vehicle.reconstitute(
                id, "Trovão", "photo", "ABC1D23", "38249206428", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, status, true, ownerId, NOW.minusSeconds(60), null, null
        );
    }
}
