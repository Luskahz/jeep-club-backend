package com.jeepclub.backend.vehicles.api.http.controller;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.vehicles.api.http.dto.detail.DetailResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.detailforedit.DetailForEditResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.include.IncludeRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.ListResponseDTO;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.VehicleService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping("/include/member")
    @Operation(
            summary = "Registrar veiculo para o usu'ario logado",
            description = "Cria um novo veiculo anexado a um membro"
    )
    public ResponseEntity<Void> includeVehicle(
            @RequestBody @Valid IncludeRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        vehicleService.create(
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
                principal.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/list/member")
    @Operation(
            summary = "Listar veículos do membro logado",
            description = "Retorna os veículos ativos do membro autenticado"
    )
    public ResponseEntity<Page<ListResponseDTO>> listMemberVehicles(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(hidden = true)
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(
                vehicleService.findAll(principal.getUserId(), pageable)
                        .map(ListResponseDTO::from)
        );
    }

    @GetMapping("/detail/member/{vehicleId}")
    @Operation(
            summary = "Detalhar veiculo pertencente ao membro logado",
            description = "Retorna os detalhes de um veículo pelo seu ID"
    )
    public ResponseEntity<DetailResponseDTO> detailMemberVehicle(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(DetailResponseDTO.from(
                vehicleService.findById(vehicleId, principal.getUserId())
        ));
    }

    @GetMapping("/detail-for-edit/member/{vehicleId}")
    @Operation(
            operationId = "detailMemberVehicle_1",
            summary = "Detalhar veiculo pertencente ao membro logado",
            description = "Retorna os detalhes de um veículo pelo seu ID"
    )
    public ResponseEntity<DetailForEditResponseDTO> detailMemberVehicleForEdit(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(DetailForEditResponseDTO.from(
                vehicleService.findById(vehicleId, principal.getUserId())
        ));
    }

    @PutMapping("/edit/member/{vehicleId}")
    @Operation(
            summary = "Editar veículo do membro logado",
            description = "Atualiza os dados de um veículo pertencente ao membro autenticado"
    )
    public ResponseEntity<Void> editMemberVehicle(
            @PathVariable Long vehicleId,
            @RequestBody @Valid EditRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        vehicleService.update(
                vehicleId,
                principal.getUserId(),
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

    @DeleteMapping("/delete/member/{vehicleId}")
    @Operation(
            summary = "Deletar veículo do membro logado",
            description = "Realiza soft delete do veículo pertencente ao membro autenticado"
    )
    public ResponseEntity<Void> deleteMemberVehicle(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        vehicleService.delete(vehicleId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
