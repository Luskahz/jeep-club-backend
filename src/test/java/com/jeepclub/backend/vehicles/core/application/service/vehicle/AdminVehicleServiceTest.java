package com.jeepclub.backend.vehicles.core.application.service.vehicle;

import com.jeepclub.backend.vehicles.core.application.FieldUpdate;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotActiveException;
import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleFieldRequiredException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
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
    void createsVehicleForExistingActiveOwner() {
        when(userPort.existsById(7L)).thenReturn(true);
        when(userPort.existsActiveById(7L)).thenReturn(true);
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
    void canonicalizesPlateAndRenavamBeforeDuplicateCheckAndPersistence() {
        when(userPort.existsById(7L)).thenReturn(true);
        when(userPort.existsActiveById(7L)).thenReturn(true);
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle vehicle = service.createForOwner(
                "Trovão", "photo", "  abc1d23  ", "382.492.064-28", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, true, 7L
        );

        verify(vehicleRepository).existsByPlate("ABC1D23");
        verify(vehicleRepository).existsByRenavam("38249206428");
        assertThat(vehicle.getPlate()).isEqualTo("ABC1D23");
        assertThat(vehicle.getRenavam()).isEqualTo("38249206428");
    }

    @Test
    void rejectsAdministrativelyDisabledOwner() {
        when(userPort.existsById(7L)).thenReturn(true);
        when(userPort.existsActiveById(7L)).thenReturn(false);

        assertThatThrownBy(this::createVehicle)
                .isInstanceOf(UserNotActiveException.class)
                .hasMessage("User is not administratively active.");
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

        service.update(1L, fullEdit(
                "Novo", "photo", "ABC1D23", "38249206428", "Jeep",
                "Renegade", 2023, 2024, "Preto", 5, FuelType.FLEX, 1.8, true
        ));
        assertThat(vehicle.getModel()).isEqualTo("Renegade");

        service.delete(1L, 99L);
        verify(vehicleRepository).delete(vehicle, 99L, NOW);
    }

    @Test
    void omittedFieldsPreserveCurrentValuesOnUpdate() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle()));

        VehicleEditFields onlyNickname = new VehicleEditFields(
                FieldUpdate.of("Atualizado"),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted(),
                FieldUpdate.omitted()
        );

        service.update(1L, onlyNickname);

        verify(vehicleRepository).save(argThat(vehicle ->
                vehicle.getNickname().equals("Atualizado")
                        && vehicle.getModel().equals("Wrangler")
                        && vehicle.getPlate().equals("ABC1D23")
        ));
    }

    @Test
    void canonicalizesPlateAndRenavamOnUpdateBeforeDuplicateCheck() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle()));

        VehicleEditFields updates = fullEditFields(
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.of("  xyz9z99  "),
                FieldUpdate.of("123.456.789-01"), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted()
        );

        service.update(1L, updates);

        verify(vehicleRepository).existsByPlate("XYZ9Z99");
        verify(vehicleRepository).existsByRenavam("12345678901");
        verify(vehicleRepository).save(argThat(vehicle ->
                vehicle.getPlate().equals("XYZ9Z99") && vehicle.getRenavam().equals("12345678901")
        ));
    }

    @Test
    void rejectsExplicitNullOnRequiredFieldWithoutTouchingPersistence() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle()));

        VehicleEditFields nullPlate = fullEditFields(
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.explicitNull(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted()
        );

        assertThatThrownBy(() -> service.update(1L, nullPlate))
                .isInstanceOf(VehicleFieldRequiredException.class);

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void hidesLegacySoftDeletedVehicleAsNotFoundWithoutFailingOnRead() {
        Vehicle legacy = Vehicle.reconstitute(
                1L, "Trovão", "photo", "ABC1D23", "38249206428", "Jeep",
                "Wrangler", 2023, 2024, "Verde", 5, FuelType.DIESEL,
                2.0, VehicleStatus.SOFT_DELETED, true, 7L, NOW.minusSeconds(60), null
        );
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(legacy));

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(VehicleIdNotFoundException.class)
                .hasMessage("Vehicle not found.");
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
                2.0, VehicleStatus.ACTIVE, true, 7L, NOW.minusSeconds(60), null
        );
    }

    private VehicleEditFields fullEdit(
            String nickname, String photo, String plate, String renavam, String brand,
            String model, int manufacturingYear, int modelYear, String color,
            int seatingCapacity, FuelType fuelType, double engineDisplacement, boolean towing
    ) {
        return new VehicleEditFields(
                FieldUpdate.of(nickname),
                FieldUpdate.of(photo),
                FieldUpdate.of(plate),
                FieldUpdate.of(renavam),
                FieldUpdate.of(brand),
                FieldUpdate.of(model),
                FieldUpdate.of(manufacturingYear),
                FieldUpdate.of(modelYear),
                FieldUpdate.of(color),
                FieldUpdate.of(seatingCapacity),
                FieldUpdate.of(fuelType),
                FieldUpdate.of(engineDisplacement),
                FieldUpdate.of(towing)
        );
    }

    private VehicleEditFields fullEditFields(
            FieldUpdate<String> nickname, FieldUpdate<String> photo, FieldUpdate<String> plate,
            FieldUpdate<String> renavam, FieldUpdate<String> brand, FieldUpdate<String> model,
            FieldUpdate<Integer> manufacturingYear, FieldUpdate<Integer> modelYear,
            FieldUpdate<String> color, FieldUpdate<Integer> seatingCapacity,
            FieldUpdate<FuelType> fuelType, FieldUpdate<Double> engineDisplacement,
            FieldUpdate<Boolean> towing
    ) {
        return new VehicleEditFields(
                nickname, photo, plate, renavam, brand, model, manufacturingYear,
                modelYear, color, seatingCapacity, fuelType, engineDisplacement, towing
        );
    }
}
