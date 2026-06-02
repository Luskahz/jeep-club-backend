package com.jeepclub.backend.vehicles.api.controller;


import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.core.application.services.DeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles/delete")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles",
        description = ""
)
public class DeleteController {

    private final DeleteService deleteService;

    @DeleteMapping("/member/{vehicleId}")
    @Operation(
            summary = "Deletar veículo do membro logado",
            description = "Realiza soft delete do veículo pertencente ao membro autenticado"
    )
    public ResponseEntity<Void> deleteMemberVehicle(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        deleteService.execute(vehicleId, principal.getUserId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_DELETE')")
    @RequiredPermission("VEHICLES_VEHICLE_DELETE")
    @Operation(
            summary = "Deletar qualquer veículo",
            description = "Realiza soft delete de qualquer veículo pelo ID"
    )
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long vehicleId) {
        deleteService.executeAsAdmin(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
