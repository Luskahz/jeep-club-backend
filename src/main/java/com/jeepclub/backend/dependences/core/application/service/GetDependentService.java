package com.jeepclub.backend.dependences.core.application.service;

import com.jeepclub.backend.dependences.core.domain.exception.DependentException;
import com.jeepclub.backend.dependences.core.domain.model.Dependent;
import com.jeepclub.backend.dependences.core.repository.DependentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetDependentService {

    private final DependentRepository dependentRepository;

    public GetDependentService(DependentRepository dependentRepository) {
        this.dependentRepository = dependentRepository;
    }

    @Transactional(readOnly = true)
    public Dependent getById(Long id, Long requestingUserId, boolean isDirector) {
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new DependentException("Dependente não encontrado com o ID fornecido."));

        // Validar permissão de visualização (RN012: Dados sensíveis restritos ao titular e diretores)
        if (!isDirector && !dependent.getSocioId().equals(requestingUserId)) {
            throw new DependentException("Você não tem permissão para visualizar este dependente.");
        }

        return dependent;
    }

    @Transactional(readOnly = true)
    public List<Dependent> getBySocioId(Long socioId, Long requestingUserId, boolean isDirector) {
        // Se não for diretor, o sócio só pode visualizar a sua própria lista de dependentes
        if (!isDirector && !socioId.equals(requestingUserId)) {
            throw new DependentException("Você não tem permissão para listar dependentes de outro sócio.");
        }

        return dependentRepository.findAllBySocioId(socioId);
    }
}

