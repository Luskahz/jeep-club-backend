package com.jeepclub.backend.membership.core.application.service;

import com.jeepclub.backend.authentication.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.authentication.core.domain.model.MembershipApplication;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationAlreadyProcessedException;
import com.jeepclub.backend.membership.core.application.exception.MembershipApplicationNotFoundException;
import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.core.port.MemberActivationMailSender;
import com.jeepclub.backend.membership.core.repository.MemberActivationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ApproveMembershipApplicationService {

    private static final Duration ACTIVATION_TOKEN_TTL = Duration.ofHours(72);

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final MemberActivationTokenRepository memberActivationTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenGenerator tokenGenerator;
    private final RefreshTokenHashService tokenHashService;
    private final MemberActivationMailSender mailSender;

    @Transactional
    public void approve(Long applicationId) {
        Instant now = Instant.now();

        MembershipApplication application = membershipApplicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new MembershipApplicationNotFoundException(applicationId));

        if (application.getStatus() != MembershipApplicationStatus.PENDING_ACTIVATION) {
            throw new MembershipApplicationAlreadyProcessedException(
                    applicationId,
                    application.getStatus().name()
            );
        }

        // Cria o usuário com status PENDING_FIRST_ACCESS
        User newUser = User.createPendingMember(
                application.getName(),
                application.getCpf(),
                application.getEmail(),
                application.getPhoneNumber(),
                now
        );
        userRepository.save(newUser);

        // Invalida tokens anteriores (segurança) e cria novo token
        memberActivationTokenRepository.invalidateAllByApplicationId(applicationId);

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        MemberActivationToken activationToken = MemberActivationToken.create(
                applicationId,
                tokenHash,
                ACTIVATION_TOKEN_TTL,
                now
        );
        memberActivationTokenRepository.save(activationToken);

        // Atualiza status da solicitação
        application.markAsInviteSent(now);
        membershipApplicationRepository.save(application);

        // Envia e-mail com o token raw
        mailSender.sendActivationLink(application.getEmail(), application.getName(), rawToken);
    }
}