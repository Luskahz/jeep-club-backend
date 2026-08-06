package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
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
                .orElseThrow(DependentException::notFound);

        // Validar permissão de visualização (RN012: Dados sensíveis restritos ao titular e diretores)
        if (!isDirector && !dependent.getSocioId().equals(requestingUserId)) {
            throw DependentException.accessDenied();
        }

        return dependent;
    }

    @Transactional(readOnly = true)
    public List<Dependent> getBySocioId(Long socioId, Long requestingUserId, boolean isDirector) {
        // Se não for diretor, o sócio só pode visualizar a sua própria lista de dependentes
        if (!isDirector && !socioId.equals(requestingUserId)) {
            throw DependentException.accessDenied();
        }

        return dependentRepository.findAllBySocioId(socioId);
    }
}

