package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.membership.api.dto.CreateMembershipApplicationRequestDTO;
import com.jeepclub.backend.membership.api.dto.MembershipApplicationResponseDTO;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyExistsException;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CreateMembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final Clock clock;

    @Transactional
    public MembershipApplicationResponseDTO create(CreateMembershipApplicationRequestDTO request) {
        Instant now = Instant.now(clock);
        // passa essa validação pro controller.
        String normalizedCpf = request.cpf().replaceAll("[^0-9]", "");


        //seria mais interessante, se a request já existe retornar a propria request ao invez de gerar um erro
        // se fizer essa mudança mude o nome do service de create pra um nome mais semântico.
        if (membershipApplicationRepository.existsByCpf(normalizedCpf)) {
            throw new MembershipApplicationAlreadyExistsException(normalizedCpf);
        }

        MembershipApplication application = MembershipApplication.create(
                request.name(),
                normalizedCpf,
                request.email(),
                request.phoneNumber(),
                request.message(),
                now
        );

        MembershipApplication saved = membershipApplicationRepository.save(application);
        return MembershipApplicationResponseDTO.fromDomain(saved);
    }
}