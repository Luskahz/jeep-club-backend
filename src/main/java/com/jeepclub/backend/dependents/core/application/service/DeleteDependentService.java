package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteDependentService {

    private final DependentRepository dependentRepository;

    public DeleteDependentService(DependentRepository dependentRepository) {
        this.dependentRepository = dependentRepository;
    }

    @Transactional
    public void delete(Long id, Long requestingUserId, boolean isDirector) {
        // 1. Buscar dependente
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new DependentException("Dependente não encontrado com o ID fornecido."));

        // 2. Validar permissão (Apenas o Sócio titular dono ou um Diretor pode deletar)
        if (!isDirector && !dependent.getSocioId().equals(requestingUserId)) {
            throw new DependentException("Você não tem permissão para remover este dependente.");
        }

        // 3. Excluir dependente
        dependentRepository.deleteById(id);
    }
}

