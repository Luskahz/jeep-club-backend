package com.jeepclub.backend.dependents.api.http.controller.admin;

import com.jeepclub.backend.dependents.api.http.dto.dependent.*;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.storage.image.ImageMultipartAdapter;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.*;

@RestController
@RequestMapping("/users/{userId}/dependents/{id}/photo")
@RequiredArgsConstructor
@Validated
@Tag(name = "Dependents - Admin")
public class AdminDependentMediaController {
    private final DependentService dependents;

    @GetMapping
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(summary = "Ler foto de dependente administrativamente", description = "Leitura auditada em cadastros operacionais, inclusive arquivados, validando o par titular/dependente. Cache-Control: no-store; sem chave de storage na resposta.")
    @ApiResponse(responseCode = "200", description = "Conteúdo da foto privada.", content = @Content(mediaType = "image/*", schema = @Schema(type = "string", format = "binary")))
    @ApiResponse(responseCode = "404", description = "Cadastro/foto não encontrado para o titular.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<byte[]> read(@PathVariable @Positive Long userId,@PathVariable @Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return DependentPhotoHttp.image(dependents.photoForAdmin(id,userId,principal.getUserId()));
    }

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_PHOTO_UPDATE')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_PHOTO_UPDATE")
    @Operation(summary = "Substituir foto de dependente administrativamente", description = "Part photo obrigatória; cadastro e titular elegíveis, par de IDs validado. Mesma compensação transacional do Platform; arquivo anterior preservado.")
    @ApiResponse(responseCode = "200", description = "Foto associada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DependentResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Cadastro ou titular inelegível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "IMAGE_UPLOAD_READ_FAILED: falha operacional ao ler a imagem enviada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> replace(@PathVariable @Positive Long userId,@PathVariable @Positive Long id,
            @RequestPart("photo") MultipartFile photo,MultipartHttpServletRequest multipart,
            @AuthenticationPrincipal UserPrincipal principal) {
        DependentPhotoHttp.validateParts(multipart,false);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(DependentResponseDTO.fromAdmin(
                dependents.replacePhotoForAdmin(id,userId,principal.getUserId(),ImageMultipartAdapter.read(photo))));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_PHOTO_UPDATE')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_PHOTO_UPDATE")
    @Operation(summary = "Desassociar foto de dependente administrativamente", description = "Remoção explícita idempotente em cadastro ACTIVE. Mantém arquivo e demais dados. O administrador real é auditado.")
    @ApiResponse(responseCode = "200", description = "Foto desassociada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = DependentResponseDTO.class)))
    public ResponseEntity<DependentResponseDTO> remove(@PathVariable @Positive Long userId,@PathVariable @Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(DependentResponseDTO.fromAdmin(
                dependents.removePhotoForAdmin(id,userId,principal.getUserId())));
    }
}
