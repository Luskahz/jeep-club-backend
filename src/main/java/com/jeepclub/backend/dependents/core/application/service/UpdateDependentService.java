package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.domain.model.MedicalProfile;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class UpdateDependentService {

    private final DependentRepository dependentRepository;
    private final UserRepository userRepository;
    private final Clock clock;


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
        Instant now = Instant.now(clock);


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

