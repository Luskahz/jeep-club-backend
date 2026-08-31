package com.jeepclub.backend.vehicles.core.application.service.vehicle;

import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotFoundException;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.port.UserPort;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminVehicleServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserPort userPort;

    private AdminVehicleService service;

    @BeforeEach
    void setUp() {
        service = new AdminVehicleService(
                vehicleRepository,
                userPort,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void createsVehicleForExistingOwner() {
        when(userPort.existsById(7L)).thenReturn(true);
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle vehicle = createVehicle();

        assertThat(vehicle.getOwnerId()).isEqualTo(7L);
        assertThat(vehicle.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsMissingOwnerAfterIdentifierChecks() {
        when(userPort.existsById(7L)).thenReturn(false);

        assertThatThrownBy(this::createVehicle)
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User id not found.");
    }

    @Test
    void listsFindsUpdatesAndDeletesAnyActiveVehicle() {
        var pageable = PageRequest.of(0, 10);
        Vehicle vehicle = vehicle();
        when(vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        assertThat(service.findAll(pageable).getContent()).containsExactly(vehicle);
        assertThat(service.findById(1L)).isSameAs(vehicle);

        service.update(
                1L, "Novo", "photo", "ABC1D23", "38249206428", "Jeep",
                "Renegade", 2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true
        );
        assertThat(vehicle.getModel()).isEqualTo("Renegade");

        service.delete(1L, 99L);
        verify(vehicleRepository).delete(vehicle, 99L, NOW);
    }

    private Vehicle createVehicle() {
        return service.createForOwner(
                "Trovão", "photo", "ABC1D23", "38249206428", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, true, 7L
        );
    }

    private Vehicle vehicle() {
        return Vehicle.reconstitute(
                1L, "Trovão", "photo", "ABC1D23", "38249206428", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, VehicleStatus.ACTIVE, true, 7L, NOW.minusSeconds(60), null, null
        );
    }
}
