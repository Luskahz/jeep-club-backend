package com.jeepclub.backend.dependencemanager.core.application.service;

import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.dependencemanager.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependencemanager.core.domain.exception.DependentException;
import com.jeepclub.backend.dependencemanager.core.domain.model.Dependent;
import com.jeepclub.backend.dependencemanager.core.domain.model.MedicalProfile;
import com.jeepclub.backend.dependencemanager.core.repository.DependentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class UpdateDependentService {

    private final DependentRepository dependentRepository;
    private final UserRepository userRepository;

    public UpdateDependentService(
            DependentRepository dependentRepository,
            UserRepository userRepository
    ) {
        this.dependentRepository = dependentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Dependent update(
            Long id,
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            MedicalProfile medicalProfile,
            boolean consentAccepted,
            Long requestingUserId,
            boolean isDirector
    ) {
        // 1. Buscar o dependente existente
        Dependent dependent = dependentRepository.findById(id)
                .orElseThrow(() -> new DependentException("Dependente não encontrado com o ID fornecido."));

        // 2. Validar permissão (Apenas o Sócio titular dono ou um Diretor/Admin pode alterar)
        if (!isDirector && !dependent.getSocioId().equals(requestingUserId)) {
            throw new DependentException("Você não tem permissão para alterar os dados deste dependente.");
        }

        // 3. Normalizar CPF e validar unicidade
        String cleanCpf = null;
        if (cpf != null && !cpf.isBlank()) {
            cleanCpf = cpf.replaceAll("\\D", "");

            // Se o CPF mudou, validar unicidade
            if (!cleanCpf.equals(dependent.getCpf())) {
                if (userRepository.existsByCpf(cleanCpf)) {
                    throw new DependentException("Já existe um sócio cadastrado com este CPF.");
                }

                if (dependentRepository.existsByCpfAndIdNot(cleanCpf, id)) {
                    throw new DependentException("Já existe outro dependente cadastrado com este CPF.");
                }
            }
        }

        Instant now = Instant.now();

        // 4. Executar a alteração no modelo de domínio puro (validações LGPD de consentimento ocorrem internamente)
        dependent.update(
                name,
                cleanCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                medicalProfile,
                consentAccepted,
                now
        );

        // 5. Salvar na base de dados
        return dependentRepository.save(dependent);
    }
}

