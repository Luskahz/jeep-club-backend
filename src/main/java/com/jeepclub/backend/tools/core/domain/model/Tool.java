package com.jeepclub.backend.tools.core.domain.model;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolAccessDeniedException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * Entidade de Domínio Rico de Ferramentas.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE) // Exigido pelo JPA/Hibernate e padronizado pelo projeto
public class Tool {

    private Long id;
    private String name;
    private String description;
    private ToolStatus status;
    private Long userId;

    // Construtor privado: Ninguém fora da classe dá um "new Tool()" diretamente
    private Tool(String name, String description, ToolStatus status, Long userId) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.userId = userId;
    }

    /**
     * Factory method para CRIAR uma NOVA ferramenta (acionado no POST)
     */
    public static @NonNull Tool create(
            String name,
            String description,
            ToolStatus status,
            Long userId
    ) {
        // Aqui dentro ficam as regras de negócio de criação
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da ferramenta não pode ser vazio.");
        }

        // Se a ferramenta recém-criada sempre tiver um status padrão, você poderia fixar aqui.
        ToolStatus initialStatus = (status != null) ? status : ToolStatus.AVAILABLE;

        return new Tool(name, description, initialStatus, userId);
    }

    /**
     * Factory method para RECONSTITUIR uma ferramenta existente que veio do banco de dados
     */
    public static @NonNull Tool reconstitute(
            Long id,
            String name,
            String description,
            ToolStatus status,
            Long userId
    ) {
        Tool tool = new Tool();
        tool.id = id;
        tool.name = name;
        tool.description = description;
        tool.status = status;
        tool.userId = userId;
        return tool;
    }

    // =========================================================
    // MÉTODOS DE NEGÓCIO (Comportamentos da entidade)
    // =========================================================

    /**
     * Atualiza os detalhes da ferramenta (acionado no PUT)
     */
    public void updateDetails(String newName, String newDescription) {
        if (newName != null && !newName.isBlank()) {
            this.name = newName;
        }
        if (newDescription != null) {
            this.description = newDescription;
        }
    }

    /**
     * Altera o status da ferramenta de forma controlada
     */
    public void changeStatus(ToolStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("O novo status não pode ser nulo.");
        }
        this.status = newStatus;
    }

    /**
     * Validação crucial de negócio: Garante que a ferramenta pertence ao usuário.
     * Isso será muito usado no Service antes de Editar ou Deletar.
     */
    public void assertBelongsTo(Long currentUserId) {
        if (!this.userId.equals(currentUserId)) {
            // Lança exceção de DOMÍNIO (regra de negócio quebrada)
            throw new ToolAccessDeniedException("Acesso negado: Esta ferramenta pertence a outro usuário.");
        }
    }

    // Exemplos de métodos booleanos úteis para lógicas futuras no Service
    public boolean isAvailable() {
        return this.status == ToolStatus.AVAILABLE; // Adapte para os enums reais que você tem
    }
}