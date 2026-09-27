package com.jeepclub.backend.publications.api.http.dto;
import java.util.List;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;
public final class EventOperationRequests {
    private EventOperationRequests() {}
    public record Registration(@NotNull List<@Valid Allocation> allocations,
        @Schema(description="Own active dependents without vehicle allocation; do not repeat IDs already in allocations.") List<@Positive Long> dependentIds) {
        public Registration { dependentIds = dependentIds == null ? List.of() : List.copyOf(dependentIds); }
    }
    @Schema(description="Own vehicle. Member occupies exactly one vehicle; dependent IDs must be own active dependents and unique across allocations. Capacity includes driver.")
    public record Allocation(@NotNull @Positive Long vehicleId, boolean member, @NotNull List<@Positive Long> dependentIds) {}
    public record Guest(@NotBlank @Pattern(regexp="[0-9.\\- ]+") String cpf, @Positive Long vehicleId) {}
    public record Rejection(@NotBlank String reason) {}
    public record Ride(@NotNull @Positive Long vehicleId, boolean accept) {}
}
