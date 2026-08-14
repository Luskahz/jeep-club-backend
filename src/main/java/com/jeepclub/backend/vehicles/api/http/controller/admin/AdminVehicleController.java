package com.jeepclub.backend.vehicles.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.vehicles.api.http.dto.detail.DetailResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.detailforedit.DetailForEditResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.include.IncludeRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.ListResponseDTO;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.AdminVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(name = "Vehicles", description = "")
public class AdminVehicleController {

    private final AdminVehicleService adminVehicleService;

    @PostMapping("/include/admin/{memberId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_INCLUDE')")
    @RequiredPermission("VEHICLES_VEHICLE_INCLUDE")
    @Operation(
            summary = "Registrar veículo para um membro cadastrado",
            description = "Cria um novo veículo anexado a um membro pelo seu ID"
    )
    public ResponseEntity<Void> includeVehicle(
            @PathVariable Long memberId,
            @RequestBody @Valid IncludeRequestDTO request
    ) {
        adminVehicleService.createForOwner(
                request.nickname(),
                request.photo(),
                request.plate(),
                request.renavam(),
                request.brand(),
                request.model(),
                request.manufacturingYear(),
                request.modelYear(),
                request.color(),
                request.seatingCapacity(),
                request.fuelType(),
                request.engineDisplacement(),
                request.towing(),
                memberId
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/list/admin")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_READ')")
    @RequiredPermission("VEHICLES_VEHICLE_READ")
    @Operation(
            summary = "Listar todos os veículos",
            description = "Retorna todos os veículos ativos da plataforma"
    )
    public ResponseEntity<Page<ListResponseDTO>> listAllVehicles(
            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(
                adminVehicleService.findAll(pageable).map(ListResponseDTO::from)
        );
    }

    @GetMapping("/detail/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_READ')")
    @RequiredPermission("VEHICLES_VEHICLE_READ")
    @Operation(
            summary = "Detalhar qualquer veículo",
            description = "Retorna os detalhes de qualquer veículo pelo ID"
    )
    public ResponseEntity<DetailResponseDTO> detailVehicle(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(DetailResponseDTO.from(
                adminVehicleService.findById(vehicleId)
        ));
    }

    @GetMapping("/detail-for-edit/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_READ')")
    @RequiredPermission("VEHICLES_VEHICLE_READ")
    @Operation(
            operationId = "detailVehicle_1",
            summary = "Detalhar qualquer veículo",
            description = "Retorna os detalhes de qualquer veículo pelo ID"
    )
    public ResponseEntity<DetailForEditResponseDTO> detailVehicleForEdit(
            @PathVariable Long vehicleId
    ) {
        return ResponseEntity.ok(DetailForEditResponseDTO.from(
                adminVehicleService.findById(vehicleId)
        ));
    }

    @PutMapping("/edit/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_UPDATE')")
    @RequiredPermission("VEHICLES_VEHICLE_UPDATE")
    @Operation(
            summary = "Editar qualquer veículo",
            description = "Atualiza os dados de qualquer veículo pelo ID"
    )
    public ResponseEntity<Void> editVehicle(
            @PathVariable Long vehicleId,
            @RequestBody @Valid EditRequestDTO request
    ) {
        adminVehicleService.update(
                vehicleId,
                request.nickname(),
                request.photo(),
                request.plate(),
                request.renavam(),
                request.brand(),
                request.model(),
                request.manufacturingYear(),
                request.modelYear(),
                request.color(),
                request.seatingCapacity(),
                request.fuelType(),
                request.engineDisplacement(),
                request.towing()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_DELETE')")
    @RequiredPermission("VEHICLES_VEHICLE_DELETE")
    @Operation(
            summary = "Deletar qualquer veículo",
            description = "Realiza soft delete de qualquer veículo pelo ID"
    )
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long vehicleId) {
        adminVehicleService.delete(vehicleId);
        return ResponseEntity.noContent().build();
    }
}
