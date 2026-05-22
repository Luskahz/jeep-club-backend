package com.jeepclub.backend.membershipKauan.core.application.service;

import com.jeepclub.backend.membershipKauan.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.membershipKauan.core.domain.model.MembershipApplication;
import com.jeepclub.backend.membershipKauan.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class CreateMembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final UserRepository userRepository;

    public CreateMembershipApplicationService(
            MembershipApplicationRepository membershipApplicationRepository,
            UserRepository userRepository
    ) {
        this.membershipApplicationRepository = membershipApplicationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MembershipApplication create(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message
    ) {
        String normalizedCpf = normalizeCpf(cpf);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhoneNumber = normalizePhoneNumber(phoneNumber);
        String normalizedName = normalizeRequiredText(name, "Nome");
        String normalizedMessage = normalizeRequiredText(message, "Mensagem");

        validateCpf(normalizedCpf);
        validatePhoneNumber(normalizedPhoneNumber);

        if (userRepository.existsByCpf(normalizedCpf)) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este CPF.");
        }

        boolean cpfAlreadyPending = membershipApplicationRepository.existsByCpfAndStatus(
                normalizedCpf,
                MembershipApplicationStatus.PENDING_ACTIVATION
        );

        if (cpfAlreadyPending) {
            throw new IllegalArgumentException("Já existe uma candidatura pendente para este CPF.");
        }

        boolean emailAlreadyPending = membershipApplicationRepository.existsByEmailAndStatus(
                normalizedEmail,
                MembershipApplicationStatus.PENDING_ACTIVATION
        );

        if (emailAlreadyPending) {
            throw new IllegalArgumentException("Já existe uma candidatura pendente para este e-mail.");
        }

        Instant now = Instant.now();

        MembershipApplication application = MembershipApplication.create(
                normalizedName,
                normalizedCpf,
                normalizedEmail,
                normalizedPhoneNumber,
                normalizedMessage,
                now
        );

        return membershipApplicationRepository.save(application);
    }

    private String normalizeCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("CPF é obrigatório.");
        }

        return cpf.replaceAll("\\D", "");
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }

        return email.trim().toLowerCase();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Telefone é obrigatório.");
        }

        return phoneNumber.replaceAll("\\D", "");
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " é obrigatório.");
        }

        return value.trim();
    }

    private void validateCpf(String cpf) {
        if (cpf.length() != 11) {
            throw new IllegalArgumentException("CPF deve conter 11 dígitos.");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.length() < 10 || phoneNumber.length() > 11) {
            throw new IllegalArgumentException("Telefone deve conter 10 ou 11 dígitos.");
        }
    }
}