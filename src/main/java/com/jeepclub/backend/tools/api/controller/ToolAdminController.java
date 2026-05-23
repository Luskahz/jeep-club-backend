package com.jeepclub.backend.tools.api.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tools")
@RequiredArgsConstructor
@Tag(name = "Admin - Tools", description = "Gerenciamento administrativo de ferramentas e equipamentos dos usuários do sistema.")
public class ToolAdminController {
    // aqui voce cria as rotas que possuem o PreAuthorize
    // o padrão das tags é: "NomeModulo_NomeClasse_AcaoConcedida tudo em capslock exemplo: TOOLS_TOOL_READ,

    // rotas minimas previstas pra este modulo:

    // - consultar todas as ferramentas, usar pagable, trazer no minimo id ferramenta, nome ferramenta, id dono ferramenta, e alguns outros campos que achar necessarios, porem lembre de não trazer todos os campos pois pesa o front
    // - consultar todas as ferramentas de um user, aplicar logica do pagable do spring e limitar os campos
    // - consultar uma ferramenta especifica de um user especifico
    // - selfDelete de uma ferramenta especifica de um user especifico
    // - criar uma ferramenta para um user especifico
    // - atualizar uma ferramenta de um user especifico
}
