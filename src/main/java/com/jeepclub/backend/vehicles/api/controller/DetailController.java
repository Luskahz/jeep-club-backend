package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.dto.detail.DetailResponseDTO;
import com.jeepclub.backend.vehicles.core.application.services.DetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles/detail")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles",
        description = ""
)
public class DetailController {

    private final DetailService detailService;

    @GetMapping("/member/{vehicleId}")
    @Operation(
            summary = "Detalhar veiculo pertencente ao membro logado",
            description = "Retorna os detalhes de um veículo pelo seu ID"
    )
    public ResponseEntity<DetailResponseDTO> detailMemberVehicle(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
            ) {
        return ResponseEntity.ok(detailService.execute(vehicleId, principal.getUserId()));
    }

    @GetMapping("/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_READ')")
    @RequiredPermission("VEHICLES_VEHICLE_READ")
    @Operation(
            summary = "Detalhar qualquer veículo",
            description = "Retorna os detalhes de qualquer veículo pelo ID"
    )
    public ResponseEntity<DetailResponseDTO> detailVehicle(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(detailService.executeAsAdmin(vehicleId));
    }
}