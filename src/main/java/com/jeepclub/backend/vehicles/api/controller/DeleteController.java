package com.jeepclub.backend.vehicles.api.controller;


import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.core.application.services.DeleteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
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
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Long memberId = principal.getUserId();

        deleteService.execute(vehicleId, memberId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/{vehicleId}")
    // pre autorize
    @Operation(
            summary = "Deletar qualquer veículo",
            description = "Realiza soft delete de qualquer veículo pelo ID"
    )
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long vehicleId) {
        deleteService.executeAsAdmin(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
