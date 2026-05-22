package com.jeepclub.backend.membershipKauan.core.application.service;

import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.membershipKauan.core.domain.enums.RelationshipType;
import com.jeepclub.backend.membershipKauan.core.domain.exception.DependentException;
import com.jeepclub.backend.membershipKauan.core.domain.model.Dependent;
import com.jeepclub.backend.membershipKauan.core.domain.model.MedicalProfile;
import com.jeepclub.backend.membershipKauan.core.repository.DependentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class CreateDependentService {

    private final DependentRepository dependentRepository;
    private final UserRepository userRepository;

    public CreateDependentService(
            DependentRepository dependentRepository,
            UserRepository userRepository
    ) {
        this.dependentRepository = dependentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Dependent create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            MedicalProfile medicalProfile,
            boolean consentAccepted,
            Long socioId
    ) {
        // 1. Validar se o Sócio existe na base de dados
        if (!userRepository.existsById(socioId)) {
            throw new DependentException("Sócio titular não encontrado com o ID fornecido.");
        }

        // 2. Normalizar e validar unicidade de CPF se fornecido
        String cleanCpf = null;
        if (cpf != null && !cpf.isBlank()) {
            cleanCpf = cpf.replaceAll("\\D", "");
            
            // Validar se CPF existe na tabela de Sócios
            if (userRepository.existsByCpf(cleanCpf)) {
                throw new DependentException("Já existe um sócio cadastrado com este CPF.");
            }

            // Validar se CPF existe na tabela de Dependentes
            if (dependentRepository.existsByCpf(cleanCpf)) {
                throw new DependentException("Já existe um dependente cadastrado com este CPF.");
            }
        }

        Instant now = Instant.now();

        // 3. Criar entidade de domínio puro (validações internas ocorrem no construtor)
        Dependent dependent = Dependent.create(
                name,
                cleanCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                medicalProfile,
                consentAccepted,
                socioId,
                now
        );

        // 4. Salvar na base de dados
        return dependentRepository.save(dependent);
    }
}
