package com.jeepclub.backend.tools.infra.persistence.mapper;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.stereotype.Component;

@Component
public class ToolMapper {

    // Converte da Entidade do Banco (Infra) para o Modelo de Domínio (Core)
    public Tool toDomain(ToolEntity entity) {
        if (entity == null) return null;

        // AQUI ESTÁ O SEGREDO: Usamos o método reconstitute que criamos no Domínio Rico!
        return Tool.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getUserId()
        );
    }

    // Converte do Modelo de Domínio (Core) para a Entidade do Banco (Infra)
    public ToolEntity toEntity(Tool domain) {
        if (domain == null) return null;

        ToolEntity entity = new ToolEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setStatus(domain.getStatus());
        entity.setUserId(domain.getUserId());

        return entity;
    }
}