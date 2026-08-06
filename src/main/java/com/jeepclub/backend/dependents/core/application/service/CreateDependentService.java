package com.jeepclub.backend.dependents.core.application.service;

import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CreateDependentService {

    private final DependentRepository dependentRepository;
    // errado, vc n pode importar nada do authentication aqui, precisa resolver de outra forma, usando um port adapter.
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public Dependent create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            boolean consentAccepted,
            Long socioId
    ) {
        Instant now = Instant.now(clock);

        // 1. Validar se o Sócio existe na base de dados
        if (!userRepository.existsById(socioId)) {
            throw DependentException.notFound();
        }

        // 2. Normalizar e validar unicidade de CPF se fornecido
        String cleanCpf = null;
        // essa validação é mesmo necessaria aqui? acho que consegue validar isso lá no DTO quando o cpf chega no backend
        // digo a validação de not null e not blank
        if (cpf != null && !cpf.isBlank()) {
            cleanCpf = cpf.replaceAll("\\D", "");
            
            // Validar se CPF existe na tabela de Sócios
            // vc n deveria ter chamado o user repository aqui, ele não pode ser importado entre modulos
            // faça um port lá em dependents.core.port e um adapter lá no infra do authentication para implementar oque seu port precisa
            // pesquise sobre como fazer
            if (userRepository.existsByCpf(cleanCpf)) {
                throw DependentException.conflict();
            }

            // Validar se CPF existe na tabela de Dependentes
            if (dependentRepository.existsByCpf(cleanCpf)) {
                throw DependentException.conflict();
            }
        }

        // 3. Criar entidade de domínio puro (validações internas ocorrem no construtor)
        Dependent dependent = Dependent.create(
                name,
                cleanCpf,
                birthDate,
                relationshipType,
                phoneNumber,
                consentAccepted,
                socioId,
                now
        );

        // 4. Salvar na base de dados
        return dependentRepository.save(dependent);
    }
}
