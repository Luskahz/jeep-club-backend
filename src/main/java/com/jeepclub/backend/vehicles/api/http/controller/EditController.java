package com.jeepclub.backend.vehicles.api.http.controller;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.core.application.services.EditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vehicles/edit")
@RequiredArgsConstructor
@Validated
@Tag(name = "Vehicles", description = "")
public class EditController {

    private final EditService editService;

    @PutMapping("/member/{vehicleId}")
    @Operation(
            summary = "Editar veículo do membro logado",
            description = "Atualiza os dados de um veículo pertencente ao membro autenticado"
    )
    public ResponseEntity<Void> editMemberVehicle(
            @PathVariable Long vehicleId,
            @RequestBody @Valid EditRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        editService.execute(vehicleId, principal.getUserId(), requestDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_UPDATE')")
    @RequiredPermission("VEHICLES_VEHICLE_UPDATE")
    @Operation(
            summary = "Editar qualquer veículo",
            description = "Atualiza os dados de qualquer veículo pelo ID"
    )
    public ResponseEntity<Void> editVehicle(
            @PathVariable Long vehicleId,
            @RequestBody @Valid EditRequestDTO requestDTO
    ) {
        editService.executeAsAdmin(vehicleId, requestDTO);
        return ResponseEntity.noContent().build();
    }
}
