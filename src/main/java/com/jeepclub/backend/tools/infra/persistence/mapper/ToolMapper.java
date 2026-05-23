package com.jeepclub.backend.tools.infra.persistence.mapper;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.stereotype.Component;

@Component
// tem que criar o toEntity tbm, seu modulo tá começando ainda mas vai precisar transformar do dominio em entidade futuramente
// principalmente quando for salvar uma ferramenta no banco.
public class ToolMapper {
    public Tool toDomain(ToolEntity entity) {
        return new Tool(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getUserId() // <- PASSANDO O USER ID AQUI
        );
    }
}