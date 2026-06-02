package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.core.port.CreateUserWithPendingFirstAccessPort;
import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import com.jeepclub.backend.membership.core.port.MembershipTimeProperties;
import com.jeepclub.backend.membership.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.membership.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ApproveMembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final CreateUserWithPendingFirstAccessPort createUserPort;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final MemberActivationMailSender mailSender;
    private final MembershipTimeProperties membershipTimeProperties;
    private final Clock clock;

    @Transactional
    public void approve(Long applicationId) {
        Instant now = Instant.now(clock);

        MembershipApplication application = membershipApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));

        if (application.getStatus() != MembershipApplicationStatus.PENDING) {
            throw new MembershipApplicationAlreadyProcessedException(
                    applicationId,
                    application.getStatus().name()
            );
        }

        // Cria o usuário via port — a implementação vive no módulo authentication.
        createUserPort.createPendingUser(
                application.getName(),
                application.getEmail(),
                application.getCpf(),
                application.getPhoneNumber()
        );

        // Invalida tokens anteriores e gera um novo par raw/hash.
        // O rawToken é enviado por e-mail; o tokenHash é persistido para validação
        // quando o usuário clicar no link de ativação.
        memberActivationTokenRepository.invalidateAllByApplicationId(applicationId);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        MemberActivationToken activationToken = MemberActivationToken.create(
                applicationId,
                tokenHash,
                membershipTimeProperties.activationTokenTtl(),
                now
        );
        memberActivationTokenRepository.save(activationToken);

        // Atualiza status da solicitação
        application.markAsInviteSent(now);
        membershipApplicationRepository.save(application);

        mailSender.sendActivationLink(application.getEmail(), application.getName(), rawToken);
    }
}