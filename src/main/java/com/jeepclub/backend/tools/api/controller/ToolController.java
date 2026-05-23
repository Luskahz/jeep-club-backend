package com.jeepclub.backend.tools.api.controller;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.tools.api.dto.ToolResponseDTO;
import com.jeepclub.backend.tools.core.application.service.ToolService;
// Importe a classe que representa o seu usuário logado (pela sua estrutura, deve ser essa abaixo ou JwtAuthenticatedUser)

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Tag(name = "Tools", description = "Gerenciamento de ferramentas e equipamentos do usuário.")
public class ToolController {

    // pendente as seguintes rotas:
    // - Usuário criar uma ferramenta
    // - Usuário editar uma ferramenta DELE (o user n pode ter acesso a ferramentas alheias)
    // - usuario deve conseguir realizar o SelfDelete de uma ferramenta dele
    // - a rota get em /tools hoje retorna todos os campos de uma tool. isso é pesado faça o DTO diferente para retornar apenas alguns campos principais da ferramenta e aplique logica de paginação com pageble do spring
    // - a rota get tools/{id} retorna uma ferramenta especifica ai sim ela pode retornar todos os campos de uma ferramenta.
    // - verifique se hoje os nomes das rotas estão semanticos padrão restAPI

    //ESTE CONTROLLER DEVE COMPOR APENAS ROTAS QUE SÓ PRECISAM DA AUTHENTICAÇÃO DO USUARIO, NÃO CRIE ROTAS QUE REQUEREM PERMISSIONS AQUI.

    private final ToolService toolQueryService;

    //Retirei as permissions que voce colocou nas rotas pois não faz sentido o user precisar de uma permissão (Role) para conseguir ver as ferramentas dele, ele já logou, já colocou a senha, não é necessario mais permissões;
    @GetMapping
    @Operation(summary = "Listar ferramentas do usuário", description = "Retorna todas as ferramentas pertencentes ao usuário logado.")
    public ResponseEntity<List<ToolResponseDTO>> getAvailableTools(
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // boa

        // necessario ajustar seu metodo pois estava usando uma dto como classe e não como record que é o correto.
        List<ToolResponseDTO> tools = toolQueryService.listUserTools(userPrincipal.getUserId())
                .stream()
                .map(ToolResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes da ferramenta", description = "Retorna os dados de uma ferramenta específica do usuário.")
    public ResponseEntity<ToolResponseDTO> getToolById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // boa.

        // necessario ajustar seu metodo pois estava usando uma dto como classe e não como record que é o correto.
        ToolResponseDTO tool = new ToolResponseDTO(toolQueryService.getToolDetails(id, userPrincipal.getUserId()));

        return ResponseEntity.ok(tool);
    }
}