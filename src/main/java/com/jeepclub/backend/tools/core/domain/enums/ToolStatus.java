package com.jeepclub.backend.tools.core.domain.enums;


// falta status aqui, quando for fazer a rota de selfDelete vai ver que falta um status de deletado

// esses status não estão semanticos, mude os nomes para nomes mais faceis, recomendação para manter o padrão do projeto:
// Active, Inactive, Deleted, mantenha demais status apenas se tiver regra de negocio para os mesmos
public enum ToolStatus {
    ACTIVE,    // Ferramenta pronta e disponível
    INACTIVE,  // Ferramenta em manutenção/inativa
    DELETED    // Ferramenta excluída (Soft Delete)
}