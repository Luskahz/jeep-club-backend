package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.chargeDefinition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChargeDefinitionService {

    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final Clock clock;

    @Transactional
    public ChargeDefinitionResult create(
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            boolean required
    ) {
        Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        String normalizedName = name.trim().toLowerCase();

        if (chargeDefinitionRepository.existsByName(name)) {
            throw new ChargeDefinitionAlreadyExistsException(
                    "Charge definition name already exists."
            );
        }

        Instant now = Instant.now(clock);

        ChargeDefinition chargeDefinition = ChargeDefinition.create(
                name,
                description,
                defaultAmount,
                recurrenceType,
                required,
                now
        );

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional(readOnly = true)
    public Page<ChargeDefinitionResult> findAll(Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable cannot be null");

        return chargeDefinitionRepository.findAll(pageable)
                .map(ChargeDefinitionResult::from);
    }

    @Transactional(readOnly = true)
    public ChargeDefinitionResult findById(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionOrThrow(id);

        return ChargeDefinitionResult.from(chargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult activate(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionOrThrow(id);

        chargeDefinition.activate(Instant.now(clock));

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult deactivate(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionOrThrow(id);

        chargeDefinition.deactivate(Instant.now(clock));

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult archive(Long id) {
        ChargeDefinition chargeDefinition = findChargeDefinitionOrThrow(id);

        chargeDefinition.archive(Instant.now(clock));

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    private ChargeDefinition findChargeDefinitionOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return chargeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException(
                        "Charge definition not found."
                ));
    }
}