package com.jeepclub.backend.platform.export;
import java.lang.annotation.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
 @ApiResponse(responseCode="200", description="Download administrativo. CSV: UTF-8 com BOM e ponto e vírgula. PDF: relatório paginado. Limites configuráveis: 20.000 linhas CSV, 500 linhas PDF, 16 MiB; exceder retorna 413, nunca trunca.", content={@Content(mediaType="text/csv",schema=@Schema(type="string",format="binary")),@Content(mediaType="application/pdf",schema=@Schema(type="string",format="binary"))}),
 @ApiResponse(responseCode="400",description="Filtro ou formato inválido",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
 @ApiResponse(responseCode="401",description="Não autenticado",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
 @ApiResponse(responseCode="403",description="Sem permissão de exportação",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
 @ApiResponse(responseCode="404",description="Recurso referenciado inexistente",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
 @ApiResponse(responseCode="413",description="Limite síncrono excedido; refine os filtros",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class))),
 @ApiResponse(responseCode="500",description="Falha controlada de geração",content=@Content(mediaType="application/problem+json",schema=@Schema(implementation=ApiErrorResponse.class)))
})
public @interface ExportResponse {}
