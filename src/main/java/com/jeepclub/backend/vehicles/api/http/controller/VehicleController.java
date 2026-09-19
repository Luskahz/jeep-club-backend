package com.jeepclub.backend.vehicles.api.http.controller;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.vehicles.api.http.dto.detail.DetailResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.detailforedit.DetailForEditResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestFieldReader;
import com.jeepclub.backend.vehicles.api.http.dto.include.IncludeRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.ListResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.VehiclePageResponseSchema;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
import com.jeepclub.backend.vehicles.core.application.service.vehicle.VehicleService;
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
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles - Member",
        description = "Operações do membro autenticado sobre seus próprios veículos ativos."
)
public class VehicleController {

    private final VehicleService vehicleService;
    private final EditRequestFieldReader editRequestFieldReader;

    @PostMapping("/include/member")
    @Operation(
            summary = "Cadastrar veículo do membro autenticado",
            description = "Cria um veículo ACTIVE usando o userId do principal autenticado como proprietário.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Veículo cadastrado."),
                    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Placa ou RENAVAM já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
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
            description = "Retorna uma página zero-based dos veículos ACTIVE do membro autenticado. `size` é 10 por padrão e limitado globalmente a 50; `sort` usa `id` por padrão.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de veículos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = VehiclePageResponseSchema.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<ListResponseDTO>> listMemberVehicles(
            @AuthenticationPrincipal UserPrincipal principal,
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                vehicleService.findAll(principal.getUserId(), pageable)
                        .map(ListResponseDTO::from)
        ));
    }

    @GetMapping("/detail/member/{vehicleId}")
    @Operation(
            summary = "Consultar veículo do membro autenticado",
            description = "Retorna somente veículo ACTIVE pertencente ao membro; inexistência, outro proprietário e status não ativo são ocultados como não encontrado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Veículo retornado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado para o membro.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DetailResponseDTO> detailMemberVehicle(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
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
            summary = "Consultar dados de edição do veículo do membro",
            description = "Retorna os dados editáveis somente de veículo ACTIVE pertencente ao membro; ownership inválido é ocultado como não encontrado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dados de edição retornados.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DetailForEditResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado para o membro.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DetailForEditResponseDTO> detailMemberVehicleForEdit(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
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
            description = "Atualiza parcialmente os dados do veículo ACTIVE pertencente ao membro. "
                    + "Um campo omitido do JSON preserva o valor atual; consulte o schema de "
                    + "EditRequestDTO para saber quais campos aceitam null explícito para limpeza.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EditRequestDTO.class)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Veículo atualizado."),
                    @ApiResponse(responseCode = "400", description = "Payload inválido, campo com formato inválido, ou campo obrigatório enviado como null.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado para o membro.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Nova placa ou RENAVAM já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> editMemberVehicle(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
            @PathVariable Long vehicleId,
            @RequestBody JsonNode request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        VehicleEditFields updates = editRequestFieldReader.read(request);
        vehicleService.update(vehicleId, principal.getUserId(), updates);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/delete/member/{vehicleId}")
    @Operation(
            summary = "Deletar veículo do membro logado",
            description = "Sob lock, arquiva o snapshot e remove fisicamente o veículo ACTIVE pertencente ao membro.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Veículo removido e histórico arquivado."),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado para o membro.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Veículo removido concorrentemente antes da obtenção do lock.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> deleteMemberVehicle(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        vehicleService.delete(vehicleId, principal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
