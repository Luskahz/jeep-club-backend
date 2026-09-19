package com.jeepclub.backend.vehicles.core.application.service.vehicle;

import com.jeepclub.backend.vehicles.core.application.FieldUpdate;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleFieldRequiredException;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
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
    void canonicalizesPlateAndRenavamBeforeDuplicateCheckAndPersistence() {
        when(vehicleRepository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Vehicle vehicle = service.create(
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
    void updatesAndDeletesOwnedVehicleWithAuditData() {
        Vehicle vehicle = vehicle(1L, 7L, VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle));

        service.update(1L, 7L, fullEdit(
                "Novo", "photo", "ABC1D23", "38249206428",
                "Jeep", "Renegade", 2023, 2024, "Preto", 5,
                FuelType.FLEX, 1.8, true
        ));

        assertThat(vehicle.getNickname()).isEqualTo("Novo");
        assertThat(vehicle.getUpdatedAt()).isEqualTo(NOW);
        verify(vehicleRepository).save(vehicle);

        service.delete(1L, 7L);
        verify(vehicleRepository).delete(vehicle, 7L, NOW);
    }

    @Test
    void omittedFieldsPreserveCurrentValuesOnMemberUpdate() {
        Vehicle vehicle = vehicle(1L, 7L, VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle));

        VehicleEditFields onlyColor = new VehicleEditFields(
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.of("Amarelo"),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted()
        );

        service.update(1L, 7L, onlyColor);

        verify(vehicleRepository).save(argThat(saved ->
                saved.getColor().equals("Amarelo") && saved.getModel().equals("Wrangler")
        ));
    }

    @Test
    void clearsNullableFieldOnExplicitNull() {
        Vehicle vehicle = vehicle(1L, 7L, VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle));

        VehicleEditFields clearNickname = new VehicleEditFields(
                FieldUpdate.explicitNull(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted()
        );

        service.update(1L, 7L, clearNickname);

        verify(vehicleRepository).save(argThat(saved -> saved.getNickname() == null));
    }

    @Test
    void canonicalizesPlateAndRenavamOnUpdateBeforeDuplicateCheck() {
        Vehicle vehicle = vehicle(1L, 7L, VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle));

        VehicleEditFields updates = new VehicleEditFields(
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.of("  xyz9z99  "),
                FieldUpdate.of("123.456.789-01"), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted()
        );

        service.update(1L, 7L, updates);

        verify(vehicleRepository).existsByPlate("XYZ9Z99");
        verify(vehicleRepository).existsByRenavam("12345678901");
        verify(vehicleRepository).save(argThat(saved ->
                saved.getPlate().equals("XYZ9Z99") && saved.getRenavam().equals("12345678901")
        ));
    }

    @Test
    void rejectsExplicitNullOnRequiredFieldWithoutTouchingPersistence() {
        Vehicle vehicle = vehicle(1L, 7L, VehicleStatus.ACTIVE);
        when(vehicleRepository.findByIdAndOwnerId(1L, 7L))
                .thenReturn(Optional.of(vehicle));

        VehicleEditFields nullBrand = new VehicleEditFields(
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.explicitNull(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted(), FieldUpdate.omitted(), FieldUpdate.omitted(),
                FieldUpdate.omitted()
        );

        assertThatThrownBy(() -> service.update(1L, 7L, nullBrand))
                .isInstanceOf(VehicleFieldRequiredException.class);

        verify(vehicleRepository, never()).save(any());
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
}
