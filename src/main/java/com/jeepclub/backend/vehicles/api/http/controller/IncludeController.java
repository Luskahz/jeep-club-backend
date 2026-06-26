package com.jeepclub.backend.vehicles.api.http.controller;


import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.http.dto.include.IncludeRequestDTO;
import com.jeepclub.backend.vehicles.core.application.services.IncludeService;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles",
        description = ""
)
public class IncludeController {

    private final IncludeService includeService;

    @PostMapping("/include/member")
    @Operation(
            summary = "Registrar veiculo para o usu'ario logado",
            description = "Cria um novo veiculo anexado a um membro"
    )
    public ResponseEntity<Void> includeVehicle(
            @RequestBody @Valid IncludeRequestDTO requestDTO,
            @AuthenticationPrincipal UserPrincipal principal
    ) {


        Vehicle vehicle = includeService.includeVehicle(
                requestDTO.nickname(),
                requestDTO.photo(),
                requestDTO.plate(),
                requestDTO.renavam(),
                requestDTO.brand(),
                requestDTO.model(),
                requestDTO.manufacturingYear(),
                requestDTO.modelYear(),
                requestDTO.color(),
                requestDTO.seatingCapacity(),
                requestDTO.fuelType(),
                requestDTO.engineDisplacement(),
                requestDTO.towing(),
                principal.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/include/admin/{memberId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_INCLUDE')")
    @RequiredPermission("VEHICLES_VEHICLE_INCLUDE")
    @Operation(
            summary = "Registrar veículo para um membro cadastrado",
            description = "Cria um novo veículo anexado a um membro pelo seu ID"
    )
    public ResponseEntity<Void> includeVehicle(
            @PathVariable Long memberId,
            @RequestBody @Valid IncludeRequestDTO requestDTO
    ) {
        includeService.includeVehicleAsAdmin(
                requestDTO.nickname(),
                requestDTO.photo(),
                requestDTO.plate(),
                requestDTO.renavam(),
                requestDTO.brand(),
                requestDTO.model(),
                requestDTO.manufacturingYear(),
                requestDTO.modelYear(),
                requestDTO.color(),
                requestDTO.seatingCapacity(),
                requestDTO.fuelType(),
                requestDTO.engineDisplacement(),
                requestDTO.towing(),
                memberId
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}

