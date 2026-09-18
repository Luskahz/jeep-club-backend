package com.jeepclub.backend.vehicles.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.vehicles.api.http.dto.detail.DetailResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.detailforedit.DetailForEditResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.include.IncludeRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.ListResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.VehiclePageResponseSchema;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.AdminVehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(
        name = "Vehicles - Admin",
        description = "Operações administrativas sobre veículos ativos de qualquer proprietário."
)
public class AdminVehicleController {

    private final AdminVehicleService adminVehicleService;

    @PostMapping("/include/admin/{memberId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_CREATE')")
    @RequiredPermission("VEHICLES_VEHICLE_CREATE")
    @Operation(
            summary = "Registrar veículo para um membro cadastrado",
            description = "Cria um veículo ACTIVE para um userId existente. O proprietário informado precisa "
                    + "existir e estar administrativamente ativo (não desabilitado); um proprietário "
                    + "desabilitado é rejeitado mesmo que o ID exista.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Veículo cadastrado."),
                    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_CREATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Proprietário não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Placa/RENAVAM já cadastrado, ou proprietário existente porém administrativamente desabilitado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> includeVehicle(
            @Parameter(description = "Identificador do usuário proprietário.", example = "7", required = true)
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
            description = "Retorna uma página zero-based de todos os veículos ACTIVE. `size` é 10 por padrão e limitado globalmente a 50; `sort` usa `id` por padrão.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de veículos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = VehiclePageResponseSchema.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<ListResponseDTO>> listAllVehicles(
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                adminVehicleService.findAll(pageable).map(ListResponseDTO::from)
        ));
    }

    @GetMapping("/detail/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_READ')")
    @RequiredPermission("VEHICLES_VEHICLE_READ")
    @Operation(
            summary = "Detalhar qualquer veículo",
            description = "Retorna os detalhes de um veículo ACTIVE pelo ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Veículo retornado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DetailResponseDTO> detailVehicle(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
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
            summary = "Consultar dados de edição de qualquer veículo",
            description = "Retorna os dados editáveis de um veículo ACTIVE pelo ID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dados de edição retornados.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailForEditResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DetailForEditResponseDTO> detailVehicleForEdit(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
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
            description = "Substitui os dados de um veículo ACTIVE. O payload atual não possui semântica parcial uniforme; consulte o schema de EditRequestDTO.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Veículo atualizado."),
                    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Nova placa ou RENAVAM já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> editVehicle(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
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
            description = "Sob lock, arquiva o snapshot e remove fisicamente um veículo ACTIVE pelo ID.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Veículo removido e histórico arquivado."),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_DELETE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Veículo removido concorrentemente antes da obtenção do lock.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> deleteVehicle(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        adminVehicleService.delete(
                vehicleId,
                principal.getUserId()
        );
        return ResponseEntity.noContent().build();
    }
}
