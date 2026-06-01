package com.jeepclub.backend.vehicles.api.controller;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.core.application.services.EditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.Authentication;
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
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long memberId = principal.getUserId();

        editService.execute(vehicleId, memberId, requestDTO);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/admin/{vehicleId}")
    // pre autorize boi
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
