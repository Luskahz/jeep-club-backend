package com.jeepclub.backend.vehicles.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.vehicles.api.http.dto.detail.DetailResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.detailforedit.DetailForEditResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.edit.EditRequestFieldReader;
import com.jeepclub.backend.vehicles.api.http.dto.include.IncludeRequestDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.AdminListResponseDTO;
import com.jeepclub.backend.vehicles.api.http.dto.list.AdminVehiclePageResponseSchema;
import com.jeepclub.backend.vehicles.core.application.VehicleEditFields;
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
import tools.jackson.databind.JsonNode;
import com.jeepclub.backend.platform.storage.image.ImageUpload;
import com.jeepclub.backend.platform.storage.image.ImageMultipartAdapter;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Vehicles - Admin",
        description = "Operações administrativas de veículos; leitura de disponibilidade e escrita somente elegível."
)
public class AdminVehicleController {

    private final AdminVehicleService adminVehicleService;
    private final EditRequestFieldReader editRequestFieldReader;

    @PostMapping(value = "/include/admin/{memberId}", consumes = MediaType.APPLICATION_JSON_VALUE)
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
        return createVehicle(request, null, memberId);
    }

    @PostMapping(value = "/include/admin/{memberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_CREATE')")
    @RequiredPermission("VEHICLES_VEHICLE_CREATE")
    @Operation(summary = "Cadastrar veículo para membro com foto opcional",
        description = "Part vehicle: JSON validado sem photo/storageKey. Part photos: zero a cinco arquivos até 5 MiB; gallery opcional define ordem/principal. photo único legado não pode ser combinado. Exige proprietário ativo. Rollback compensa somente a imagem recém-criada.",
        responses = {@ApiResponse(responseCode = "201", description = "Veículo cadastrado."),
            @ApiResponse(responseCode = "400", description = "DTO ou imagem inválida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Sem VEHICLES_VEHICLE_CREATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Proprietário não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Identificadores duplicados ou proprietário inativo.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "Limite multipart do servidor excedido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "Part vehicle exige application/json.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Storage indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))})
    public ResponseEntity<Void> includeVehicleWithPhoto(@PathVariable Long memberId,
            @RequestPart("vehicle") @Valid IncludeRequestDTO request,
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestPart(value="photos",required=false) java.util.List<MultipartFile> photos,
            @RequestPart(value="gallery",required=false) @Valid com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryRequest gallery,
            @Parameter(hidden = true) org.springframework.web.multipart.MultipartHttpServletRequest multipart,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException, jakarta.servlet.ServletException {
        com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHttp.validate(multipart,true);
        if(gallery==null && (multipart.getMultiFileMap().containsKey("gallery") || multipart.getParameterMap().containsKey("gallery")))
            throw new com.jeepclub.backend.shared.storage.exception.InvalidStorageFileException("Explicit gallery must be a JSON object.");
        if(photos!=null || gallery!=null) {
            var change=com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHttp.change(gallery,photos);
            adminVehicleService.createWithGallery(request.nickname(),change,request.plate(),request.renavam(),request.brand(),request.model(),
                    request.manufacturingYear(),request.modelYear(),request.color(),request.seatingCapacity(),request.fuelType(),
                    request.engineDisplacement(),request.towing(),memberId,principal.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        return createVehicle(request,ImageMultipartAdapter.read(photo),memberId);
    }

    private ResponseEntity<Void> createVehicle(IncludeRequestDTO request, ImageUpload photo, Long ownerId) {
        adminVehicleService.createForOwner(
                request.nickname(),
                photo,
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
                ownerId
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
                    @ApiResponse(responseCode = "200", description = "Página de veículos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AdminVehiclePageResponseSchema.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<AdminListResponseDTO>> listAllVehicles(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue="ACTIVE") com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus status,
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                (status == com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.ACTIVE
                    ? adminVehicleService.findAllWithOwners(pageable)
                    : adminVehicleService.findAllWithOwners(status,pageable)).map(AdminListResponseDTO::from)
        ));
    }

    @GetMapping("/detail/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_READ')")
    @RequiredPermission("VEHICLES_VEHICLE_READ")
    @Operation(
            summary = "Detalhar qualquer veículo",
            description = "Retorna detalhes e galeria/revisão por ID, inclusive INACTIVE/ARCHIVED atuais; exclusão legada é ocultada.",
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
            description = "Retorna dados/galeria/revisão por ID, inclusive INACTIVE/ARCHIVED para leitura; escrita exige ACTIVE.",
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
            description = "Atualiza parcialmente os dados de um veículo ACTIVE. Um campo omitido "
                    + "do JSON preserva o valor atual; consulte o schema de EditRequestDTO para "
                    + "saber quais campos aceitam null explícito para limpeza.",
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
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão VEHICLES_VEHICLE_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Veículo ativo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Nova placa ou RENAVAM já cadastrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> editVehicle(
            @Parameter(description = "Identificador do veículo.", example = "42", required = true)
            @PathVariable Long vehicleId,
            @RequestBody JsonNode request
    ) {
        VehicleEditFields updates = editRequestFieldReader.read(request);
        adminVehicleService.update(vehicleId, updates);

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

    @org.springframework.web.bind.annotation.PatchMapping("/activate/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_ACTIVATE')")
    @RequiredPermission("VEHICLES_VEHICLE_ACTIVATE")
    @Operation(summary="Ativar veículo", description="Unarchive retorna a INACTIVE. Exclusão legada não pode ser reativada.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> activate(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(adminVehicleService.activate(vehicleId,principal.getUserId())));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/deactivate/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_DEACTIVATE')")
    @RequiredPermission("VEHICLES_VEHICLE_DEACTIVATE")
    @Operation(summary="Desativar veículo", description="Unarchive retorna a INACTIVE. Exclusão legada não pode ser reativada.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> deactivate(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(adminVehicleService.deactivate(vehicleId,principal.getUserId())));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/archive/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_ARCHIVE')")
    @RequiredPermission("VEHICLES_VEHICLE_ARCHIVE")
    @Operation(summary="Arquivar veículo", description="Unarchive retorna a INACTIVE. Exclusão legada não pode ser reativada.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> archive(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(adminVehicleService.archive(vehicleId,principal.getUserId())));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/unarchive/admin/{vehicleId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_UNARCHIVE')")
    @RequiredPermission("VEHICLES_VEHICLE_UNARCHIVE")
    @Operation(summary="Desarquivar veículo", description="Unarchive retorna a INACTIVE. Exclusão legada não pode ser reativada.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> unarchive(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(adminVehicleService.unarchive(vehicleId,principal.getUserId())));
    }

    @PutMapping(value="/gallery/admin/{vehicleId}",consumes=MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_UPDATE')")
    @RequiredPermission("VEHICLES_VEHICLE_UPDATE")
    @Operation(summary="Substituir galeria completa do veículo",description="expectedRevision obrigatório; images [] limpa. Reordena/troca principal/retém referências legadas próprias. Nova referência pública exige existência. Foto omitida nos endpoints de dados cadastrais preserva a galeria.")
    @ApiResponse(responseCode="200",description="Galeria atual; repetição idêntica é idempotente.",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=DetailResponseDTO.class)))
    @ApiResponse(responseCode="409",description="Revisão desatualizada ou cadastro inelegível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> replaceGallery(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @RequestBody @Valid com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(DetailResponseDTO.from(adminVehicleService.replaceGallery(vehicleId,request.change(java.util.List.of()),principal.getUserId())));
    }

    @PutMapping(value="/gallery/admin/{vehicleId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_UPDATE')")
    @RequiredPermission("VEHICLES_VEHICLE_UPDATE")
    @Operation(summary="Substituir galeria com upload de novas imagens",description="gallery JSON com expectedRevision/images é obrigatório; photos contém até cinco arquivos, indexados a partir de zero por uploadIndex. Não apaga arquivos antigos/compartilhados. Rollback compensa somente os novos.")
    @ApiResponse(responseCode="200",description="Galeria completa atualizada.",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=DetailResponseDTO.class)))
    @ApiResponse(responseCode="409",description="Revisão desatualizada ou cadastro inelegível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> replaceGalleryWithUploads(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @RequestPart("gallery") @Valid com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryUpdateRequest request,
            @RequestPart(value="photos",required=false) java.util.List<MultipartFile> photos,
            @Parameter(hidden=true) org.springframework.web.multipart.MultipartHttpServletRequest multipart,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException, jakarta.servlet.ServletException {
        com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHttp.validate(multipart,false);
        var change=com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHttp.change(request.asGallery(),photos);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(DetailResponseDTO.from(adminVehicleService.replaceGallery(vehicleId,change,principal.getUserId())));
    }

    @GetMapping("/gallery/history/admin/{historyId}")
    @PreAuthorize("hasAuthority('VEHICLES_VEHICLE_EXPORT')")
    @RequiredPermission("VEHICLES_VEHICLE_EXPORT")
    @Operation(summary="Consultar galeria histórica de veículo excluído",description="Snapshot independente por historyId; mesma autoridade da exportação histórica. Sem dependência do Vehicle operacional.")
    @ApiResponse(responseCode="200",description="Galeria histórica.",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHistoryResponseDTO.class)))
    public ResponseEntity<com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHistoryResponseDTO> galleryHistory(
            @PathVariable @jakarta.validation.constraints.Positive Long historyId,@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(
                com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHistoryResponseDTO.from(adminVehicleService.galleryHistory(historyId,principal.getUserId())));
    }
}
