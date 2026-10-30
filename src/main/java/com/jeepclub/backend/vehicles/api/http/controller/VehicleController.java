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
        name = "Vehicles - Member",
        description = "Operações do membro autenticado sobre seus veículos; leitura de disponibilidade e edição somente elegível."
)
public class VehicleController {

    private final VehicleService vehicleService;
    private final EditRequestFieldReader editRequestFieldReader;

    @PostMapping(value = "/include/member", consumes = MediaType.APPLICATION_JSON_VALUE)
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
        return createVehicle(request, null, principal.getUserId());
    }

    @PostMapping(value = "/include/member", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastrar veículo com foto opcional",
        description = "Part vehicle: JSON validado sem photo/storageKey. Part photos: zero a cinco arquivos JPEG/PNG/WebP até 5 MiB cada; gallery opcional define ordem/principal por uploadIndex. photo único legado não pode ser combinado. Upload e cadastro são orquestrados; rollback compensa exclusivamente o novo arquivo.",
        responses = {@ApiResponse(responseCode = "201", description = "Veículo cadastrado."),
            @ApiResponse(responseCode = "400", description = "DTO ou imagem inválida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Identificadores já cadastrados.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "413", description = "Limite multipart do servidor excedido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "415", description = "Part vehicle exige application/json.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Storage indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))})
    @ApiResponse(responseCode = "500", description = "IMAGE_UPLOAD_READ_FAILED: falha operacional ao ler a imagem enviada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<Void> includeVehicleWithPhoto(
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
            vehicleService.createWithGallery(request.nickname(),change,request.plate(),request.renavam(),request.brand(),request.model(),
                    request.manufacturingYear(),request.modelYear(),request.color(),request.seatingCapacity(),request.fuelType(),
                    request.engineDisplacement(),request.towing(),principal.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        return createVehicle(request,ImageMultipartAdapter.read(photo),principal.getUserId());
    }

    private ResponseEntity<Void> createVehicle(IncludeRequestDTO request, ImageUpload photo, Long ownerId) {
        vehicleService.create(
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
            @org.springframework.web.bind.annotation.RequestParam(defaultValue="ACTIVE") com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus status,
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(
                (status == com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus.ACTIVE
                        ? vehicleService.findAll(principal.getUserId(),pageable)
                        : vehicleService.findAll(principal.getUserId(),status,pageable))
                        .map(ListResponseDTO::from)
        ));
    }

    @GetMapping("/detail/member/{vehicleId}")
    @Operation(
            summary = "Consultar veículo do membro autenticado",
            description = "Retorna veículo próprio com galeria, capa e revisão, inclusive INACTIVE/ARCHIVED atuais; inexistência, outro proprietário e exclusão legada são ocultados como não encontrado.",
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
            description = "Retorna dados/galeria/revisão do veículo próprio, inclusive INACTIVE/ARCHIVED para leitura. Escrita exige ACTIVE; ownership inválido ou exclusão legada produz 404.",
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
            description = "Sob lock, arquiva o snapshot e remove fisicamente o veículo pertencente ao membro quando não há vínculos impeditivos.",
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

    @org.springframework.web.bind.annotation.PatchMapping("/activate/member/{vehicleId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary="Ativar veículo", description="Unarchive retorna a INACTIVE. Exclusão legada não pode ser reativada.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> activate(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(vehicleService.activate(vehicleId,principal.getUserId())));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/deactivate/member/{vehicleId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary="Desativar veículo", description="HTTP 409 VEHICLE_LIFECYCLE_CONFLICT se houver participação operacional em Event não encerrado. Não cancela inscrições nem desassocia participantes. Histórico não impede a transição.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> deactivate(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(vehicleService.deactivate(vehicleId,principal.getUserId())));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/archive/member/{vehicleId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary="Arquivar veículo", description="HTTP 409 VEHICLE_LIFECYCLE_CONFLICT se houver participação operacional em Event não encerrado. Não cancela inscrições nem desassocia participantes. Histórico não impede a transição.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> archive(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(vehicleService.archive(vehicleId,principal.getUserId())));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/unarchive/member/{vehicleId}")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary="Desarquivar veículo", description="Unarchive retorna a INACTIVE. Exclusão legada não pode ser reativada.")
    @ApiResponse(responseCode="409",description="Transição incompatível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> unarchive(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DetailResponseDTO.from(vehicleService.unarchive(vehicleId,principal.getUserId())));
    }

    @PutMapping(value="/gallery/member/{vehicleId}",consumes=MediaType.APPLICATION_JSON_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary="Substituir galeria completa do veículo",description="expectedRevision obrigatório; images [] limpa. Reordena/troca principal/retém referências legadas próprias. Nova referência pública exige existência. Foto omitida nos endpoints de dados cadastrais preserva a galeria.")
    @ApiResponse(responseCode="200",description="Galeria atual; repetição idêntica é idempotente.",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=DetailResponseDTO.class)))
    @ApiResponse(responseCode="409",description="Revisão desatualizada ou cadastro inelegível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> replaceGallery(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @RequestBody @Valid com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(DetailResponseDTO.from(vehicleService.replaceGallery(vehicleId,principal.getUserId(),request.change(java.util.List.of()))));
    }

    @PutMapping(value="/gallery/member/{vehicleId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary="Substituir galeria com upload de novas imagens",description="gallery JSON com expectedRevision/images é obrigatório; photos contém até cinco arquivos, indexados a partir de zero por uploadIndex. Não apaga arquivos antigos/compartilhados. Rollback compensa somente os novos.")
    @ApiResponse(responseCode="200",description="Galeria completa atualizada.",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=DetailResponseDTO.class)))
    @ApiResponse(responseCode="409",description="Revisão desatualizada ou cadastro inelegível.",content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE,schema=@Schema(implementation=ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "IMAGE_UPLOAD_READ_FAILED: falha operacional ao ler a imagem enviada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DetailResponseDTO> replaceGalleryWithUploads(@PathVariable @jakarta.validation.constraints.Positive Long vehicleId,
            @RequestPart("gallery") @Valid com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryUpdateRequest request,
            @RequestPart(value="photos",required=false) java.util.List<MultipartFile> photos,
            @Parameter(hidden=true) org.springframework.web.multipart.MultipartHttpServletRequest multipart,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException, jakarta.servlet.ServletException {
        com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHttp.validate(multipart,false);
        var change=com.jeepclub.backend.vehicles.api.http.dto.gallery.VehicleGalleryHttp.change(request.asGallery(),photos);
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(DetailResponseDTO.from(vehicleService.replaceGallery(vehicleId,principal.getUserId(),change)));
    }
}
