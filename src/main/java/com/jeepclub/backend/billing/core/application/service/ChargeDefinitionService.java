package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.definition.ChargeDefinitionNotFoundException;
import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.repository.ChargeAssignmentRepository;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChargeDefinitionService {

    private final ChargeDefinitionRepository chargeDefinitionRepository;
    private final ChargeAssignmentRepository chargeAssignmentRepository;
    private final Clock clock;

    @Transactional
    public ChargeDefinitionResult create(
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required
    ) {
        Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        String normalizedName = normalizeName(name);

        if (chargeDefinitionRepository.existsByName(normalizedName)) {
            throw new ChargeDefinitionAlreadyExistsException(
                    "Charge definition name already exists."
            );
        }

        Instant now = Instant.now(clock);

        ChargeDefinition chargeDefinition = ChargeDefinition.create(
                normalizedName,
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

        Instant now = Instant.now(clock);

        chargeDefinition.archive(now);

        deactivateActiveAssignments(
                chargeDefinition.getId(),
                now
        );

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    @Transactional
    public ChargeDefinitionResult update(
            Long id,
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required
    ) {
        Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");

        ChargeDefinition chargeDefinition = findChargeDefinitionOrThrow(id);

        String normalizedName = normalizeName(name);

        if (chargeDefinitionRepository.existsByNameAndIdNot(normalizedName, id)) {
            throw new ChargeDefinitionAlreadyExistsException(
                    "Charge definition name already exists."
            );
        }

        chargeDefinition.update(
                normalizedName,
                description,
                defaultAmount,
                recurrenceType,
                required,
                Instant.now(clock)
        );

        ChargeDefinition savedChargeDefinition = chargeDefinitionRepository.save(chargeDefinition);

        return ChargeDefinitionResult.from(savedChargeDefinition);
    }

    private void deactivateActiveAssignments(
            Long chargeDefinitionId,
            Instant now
    ) {
        chargeAssignmentRepository.findByChargeDefinitionId(
                        chargeDefinitionId,
                        Pageable.unpaged()
                )
                .stream()
                .filter(ChargeAssignment::isActive)
                .forEach(chargeAssignment -> {
                    chargeAssignment.deactivate(now);
                    chargeAssignmentRepository.save(chargeAssignment);
                });
    }

    private ChargeDefinition findChargeDefinitionOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return chargeDefinitionRepository.findById(id)
                .orElseThrow(() -> new ChargeDefinitionNotFoundException(
                        "Charge definition not found."
                ));
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Charge definition name cannot be blank.");
        }

        return name.trim().toLowerCase(Locale.ROOT);
    }
}