package com.jeepclub.backend.vehicles.core.application.query;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class EventVehicleQueryServiceTest {
    @Test void exposesCanonicalCapacityAndOwnerInBatch() {
        var repository = mock(VehicleRepository.class); var vehicle = mock(Vehicle.class);
        when(vehicle.getId()).thenReturn(1L); when(vehicle.getOwnerId()).thenReturn(2L);
        when(vehicle.getSeatingCapacity()).thenReturn(4); when(vehicle.getStatus()).thenReturn(VehicleStatus.ACTIVE);
        when(repository.findAllByIds(List.of(1L))).thenReturn(List.of(vehicle));
        var values = new EventVehicleQueryService(repository).findActiveBatch(List.of(1L));
        assertThat(values).singleElement().satisfies(v -> { assertThat(v.ownerId()).isEqualTo(2L); assertThat(v.seatingCapacity()).isEqualTo(4); });
    }
    @Test void missingAndInactiveVehiclesAreNotEligible() {
        var repository = mock(VehicleRepository.class); var vehicle = mock(Vehicle.class);
        when(vehicle.getStatus()).thenReturn(VehicleStatus.SOFT_DELETED);
        when(repository.findById(1L)).thenReturn(Optional.of(vehicle));
        var query = new EventVehicleQueryService(repository);
        assertThat(query.findActive(1L)).isEmpty(); assertThat(query.findActive(2L)).isEmpty();
    }
}
