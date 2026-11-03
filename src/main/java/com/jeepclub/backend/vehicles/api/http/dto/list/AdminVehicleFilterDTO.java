package com.jeepclub.backend.vehicles.api.http.dto.list;

import com.jeepclub.backend.vehicles.core.application.query.AdminVehicleFilter;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description="Filtros administrativos combinados com AND. q usa OR entre proprietário, placa, nickname e model; texto literal, trim e case-insensitive.")
public record AdminVehicleFilterDTO(@Size(max=150) String q, @Size(max=20) String plate,
        @Positive Long ownerId, @Size(max=100) String nickname, @Size(max=100) String model,
        @Schema(defaultValue="ACTIVE") VehicleStatus status) {
    public AdminVehicleFilter toFilter() {return new AdminVehicleFilter(q,plate,ownerId,nickname,model,status,java.util.Set.of());}
}
