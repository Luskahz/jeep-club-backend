package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.membership.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import com.jeepclub.backend.membership.core.port.MembershipTimeProperties;
import com.jeepclub.backend.membership.core.repository.MemberActivationTokenRepository;
import com.jeepclub.backend.membership.core.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApproveMembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
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

        // Cria o usuário com status PENDING_FIRST_ACCESS
        String temporaryPassword = UUID.randomUUID().toString();
        String passwordHash = passwordHasher.hash(temporaryPassword);

        //necessario criar um port no modulo membership que tenha um metodo para criar um usuário com status "PENDING_FIRST_ACCESS"
        //E a implementação deste port deveser feita no infra do modulo authentication com um adapter. esse adapter deve ficar na
        // pasta authentication.infra.adapter e o port aqui no membership em membership.core.port

        User newUser = User.reconstitute(
                null,
                application.getName(),
                null,
                application.getEmail(),
                application.getCpf(),
                null,
                passwordHash,
                application.getPhoneNumber(),
                null,
                UserStatus.PENDING_FIRST_ACCESS,
                null,
                now,
                null,
                null,
                null,
                0
        );
        userRepository.save(newUser);

        // Invalida tokens anteriores e cria novo
        memberActivationTokenRepository.invalidateAllByApplicationId(applicationId);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        // aqui vc tá criando um tokenHash pra fazer um challend em algum lugar, porem não está usando este token pra validação
        // garanta que haja uma rota nos controllers que receba este token raw do usuario para quando ele clicar no link do email o sistema valide
        // que é ele antes de criar o novo usuario.

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