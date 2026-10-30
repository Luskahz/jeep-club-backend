package com.jeepclub.backend.dependents.api.http.controller;

import com.jeepclub.backend.dependents.api.http.dto.dependent.*;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.storage.image.ImageMultipartAdapter;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

@RestController
@RequestMapping("/dependents")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
@Tag(name = "Dependents - Dependent")
public class DependentMediaController {
    private final DependentService dependents;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastrar dependente com foto opcional", description = "Part dependent: JSON validado. Part photo: arquivo opcional até 5 MiB, JPEG/PNG/WebP. Ownership vem do principal; não aceita storageKey. Rollback compensa somente o novo arquivo privado.")
    @ApiResponse(responseCode = "201", description = "Dependente criado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DependentResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "DTO, imagem ou multipart inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Titular inelegível ou CPF indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "503", description = "Storage indisponível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "IMAGE_UPLOAD_READ_FAILED: falha operacional ao ler a imagem enviada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> createWithPhoto(@RequestPart("dependent") @Valid CreateDependentRequestDTO request,
            @RequestPart(value = "photo", required = false) MultipartFile photo, MultipartHttpServletRequest multipart,
            @AuthenticationPrincipal UserPrincipal principal) {
        DependentPhotoHttp.validateParts(multipart,true);
        var value=dependents.create(request.name(),request.cpf(),request.birthDate(),request.relationshipType(),
                request.phoneNumber(),principal.getUserId(),ImageMultipartAdapter.read(photo));
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(DependentResponseDTO.from(value));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualizar dependente com foto opcional", description = "Foto omitida preserva a associação. Nova photo substitui. Para remoção explícita use DELETE /dependents/{id}/photo. Apenas ACTIVE; titular e DTO validados antes do armazenamento.")
    @ApiResponse(responseCode = "200", description = "Dependente atualizado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DependentResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "DTO ou arquivo inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Cadastro não está ACTIVE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "IMAGE_UPLOAD_READ_FAILED: falha operacional ao ler a imagem enviada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> updateWithPhoto(@PathVariable @Positive Long id,
            @RequestPart("dependent") @Valid UpdateDependentRequestDTO request,
            @RequestPart(value = "photo", required = false) MultipartFile photo, MultipartHttpServletRequest multipart,
            @AuthenticationPrincipal UserPrincipal principal) {
        DependentPhotoHttp.validateParts(multipart,true);
        var value=dependents.update(id,request.name(),request.cpf(),request.birthDate(),request.relationshipType(),
                request.phoneNumber(),principal.getUserId(),ImageMultipartAdapter.read(photo));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(DependentResponseDTO.from(value));
    }

    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Substituir foto do meu dependente", description = "Arquivo obrigatório na part photo. Somente titular e cadastro ACTIVE. Não apaga mídia anterior ou compartilhada.")
    @ApiResponse(responseCode = "200", description = "Foto associada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DependentResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Cadastro inelegível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "IMAGE_UPLOAD_READ_FAILED: falha operacional ao ler a imagem enviada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> replace(@PathVariable @Positive Long id,
            @RequestPart("photo") MultipartFile photo, MultipartHttpServletRequest multipart,
            @AuthenticationPrincipal UserPrincipal principal) {
        DependentPhotoHttp.validateParts(multipart,false);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(DependentResponseDTO.from(
                dependents.replacePhoto(id,principal.getUserId(),ImageMultipartAdapter.read(photo))));
    }

    @DeleteMapping("/{id}/photo")
    @Operation(summary = "Desassociar foto do meu dependente", description = "Remoção explícita e idempotente em cadastro ACTIVE. Preserva o arquivo no storage e os demais dados.")
    @ApiResponse(responseCode = "200", description = "Foto desassociada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DependentResponseDTO.class)))
    public ResponseEntity<DependentResponseDTO> remove(@PathVariable @Positive Long id,@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(DependentResponseDTO.from(dependents.removePhoto(id,principal.getUserId())));
    }

    @GetMapping("/{id}/photo")
    @Operation(summary = "Ler foto do meu dependente", description = "Snapshot consistente de ownership, cadastro ACTIVE e acesso do ator. INACTIVE/ARCHIVED e foto removida antes do snapshot retornam 404. Uma leitura já autorizada pode terminar com a foto capturada antes da revogação. Auditoria confirmada antes dos bytes; Cache-Control: no-store; sem chave privada na URL.")
    @ApiResponse(responseCode = "200", description = "Conteúdo da foto privada.", content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Dependente ou foto não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<byte[]> read(@PathVariable @Positive Long id,@AuthenticationPrincipal UserPrincipal principal) {
        return DependentPhotoHttp.image(dependents.photo(id,principal.getUserId()));
    }
}
