package com.jeepclub.backend.publications.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.publications.api.http.dto.*;
import com.jeepclub.backend.publications.core.application.service.AdminNoticeService;
import com.jeepclub.backend.publications.core.domain.model.PublicationImage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import java.util.List;

@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
@Tag(name = "Publications - Notice Admin", description = "Gestão de avisos em todos os estados editoriais.")
public class AdminNoticeController {
    private final AdminNoticeService notices;
    private final NoticeRequestReader reader;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PUBLICATIONS_NOTICE_CREATE')")
    @RequiredPermission("PUBLICATIONS_NOTICE_CREATE")
    @Operation(summary = "Criar aviso em rascunho", description = "Usa o userId autenticado como autor. A galeria referencia imagens existentes no storage global; o servidor define DRAFT e timestamps.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = CreateNoticeRequestDTO.class))),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Aviso criado em DRAFT.", content = @Content(schema = @Schema(implementation = NoticeResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Payload ou galeria inválida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sem PUBLICATIONS_NOTICE_CREATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Imagem informada não existe.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<NoticeResponseDTO> create(@RequestBody JsonNode body,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        var request = reader.create(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(NoticeResponseDTO.from(
                notices.create(principal.getUserId(), request.title(), request.content(), images(request.images()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_NOTICE_READ')")
    @RequiredPermission("PUBLICATIONS_NOTICE_READ")
    @Operation(summary = "Consultar aviso administrativamente", description = "Retorna Notice DRAFT, PUBLISHED ou ARCHIVED. Outro subtipo e ID removido são tratados como não encontrado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Aviso encontrado.", content = @Content(schema = @Schema(implementation = NoticeResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sem PUBLICATIONS_NOTICE_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Aviso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<NoticeResponseDTO> findById(@Parameter(description = "ID do aviso.") @PathVariable Long id) {
        return ResponseEntity.ok(NoticeResponseDTO.from(notices.findById(id)));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('PUBLICATIONS_NOTICE_UPDATE')")
    @RequiredPermission("PUBLICATIONS_NOTICE_UPDATE")
    @Operation(summary = "Editar aviso", description = "DRAFT e PUBLISHED aceitam edição. Campo omitido preserva o valor; null explícito é inválido. Galeria enviada substitui a anterior. ARCHIVED é somente leitura.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
                    content = @Content(schema = @Schema(implementation = UpdateNoticeRequestDTO.class))),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Aviso atualizado.", content = @Content(schema = @Schema(implementation = NoticeResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Payload ou galeria inválida.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sem PUBLICATIONS_NOTICE_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Aviso ou imagem não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Aviso arquivado ou conflito persistente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<NoticeResponseDTO> update(@Parameter(description = "ID do aviso.") @PathVariable Long id,
                                                     @RequestBody JsonNode body) {
        var request = reader.update(body);
        return ResponseEntity.ok(NoticeResponseDTO.from(notices.update(id, request.title(), request.content(),
                request.images() == null ? null : images(request.images()))));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('PUBLICATIONS_NOTICE_PUBLISH')")
    @RequiredPermission("PUBLICATIONS_NOTICE_PUBLISH")
    @Operation(summary = "Publicar aviso", description = "Transição DRAFT → PUBLISHED; preenche publishedAt. Não republica um aviso.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Aviso publicado.", content = @Content(schema = @Schema(implementation = NoticeResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sem PUBLICATIONS_NOTICE_PUBLISH.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Aviso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Estado editorial incompatível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<NoticeResponseDTO> publish(@Parameter(description = "ID do aviso.") @PathVariable Long id) {
        return ResponseEntity.ok(NoticeResponseDTO.from(notices.publish(id)));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('PUBLICATIONS_NOTICE_ARCHIVE')")
    @RequiredPermission("PUBLICATIONS_NOTICE_ARCHIVE")
    @Operation(summary = "Arquivar aviso", description = "Transição PUBLISHED → ARCHIVED; o aviso permanece operacional e legível administrativamente.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Aviso arquivado.", content = @Content(schema = @Schema(implementation = NoticeResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sem PUBLICATIONS_NOTICE_ARCHIVE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Aviso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Estado editorial incompatível.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<NoticeResponseDTO> archive(@Parameter(description = "ID do aviso.") @PathVariable Long id) {
        return ResponseEntity.ok(NoticeResponseDTO.from(notices.archive(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PUBLICATIONS_NOTICE_DELETE')")
    @RequiredPermission("PUBLICATIONS_NOTICE_DELETE")
    @Operation(summary = "Excluir aviso com histórico", description = "Remove Notice em qualquer estado editorial sob lock, após snapshot histórico. deletedByUserId vem do principal; imagens globais não são apagadas.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Aviso removido e snapshot salvo."),
                    @ApiResponse(responseCode = "401", description = "Não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Sem PUBLICATIONS_NOTICE_DELETE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Aviso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            })
    public ResponseEntity<Void> delete(@Parameter(description = "ID do aviso.") @PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        notices.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    private static List<PublicationImage> images(List<PublicationImageRequestDTO> references) {
        return references.stream().map(PublicationImageRequestDTO::toDomain).toList();
    }
}
