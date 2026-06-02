package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.repository.ChargeAssignmentRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.assignment.ChargeAssignmentEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.assignment.AllMembersChargeAssignmentJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.jpa.assignment.ChargeAssignmentJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.jpa.assignment.EventParticipantsChargeAssignmentJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.jpa.assignment.RoleChargeAssignmentJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.jpa.assignment.UserChargeAssignmentJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.ChargeAssignmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChargeAssignmentAdapter implements ChargeAssignmentRepository {

    private final ChargeAssignmentJpaRepository chargeAssignmentJpa;
    private final AllMembersChargeAssignmentJpaRepository allMembersChargeAssignmentJpa;
    private final UserChargeAssignmentJpaRepository userChargeAssignmentJpa;
    private final RoleChargeAssignmentJpaRepository roleChargeAssignmentJpa;
    private final EventParticipantsChargeAssignmentJpaRepository eventParticipantsChargeAssignmentJpa;
    private final ChargeAssignmentMapper mapper;

    @Override
    public ChargeAssignment save(ChargeAssignment chargeAssignment) {
        ChargeAssignmentEntity entity = mapper.toEntity(chargeAssignment);
        ChargeAssignmentEntity savedEntity = chargeAssignmentJpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChargeAssignment> findById(Long id) {
        return chargeAssignmentJpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<ChargeAssignment> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    ) {
        return chargeAssignmentJpa.findByChargeDefinitionId(chargeDefinitionId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsAllMembersAssignmentByChargeDefinitionId(Long chargeDefinitionId) {
        return allMembersChargeAssignmentJpa.existsByChargeDefinitionId(chargeDefinitionId);
    }

    @Override
    public boolean existsUserAssignmentByChargeDefinitionIdAndUserId(
            Long chargeDefinitionId,
            Long userId
    ) {
        return userChargeAssignmentJpa.existsByChargeDefinitionIdAndUserId(
                chargeDefinitionId,
                userId
        );
    }

    @Override
    public boolean existsRoleAssignmentByChargeDefinitionIdAndRoleId(
            Long chargeDefinitionId,
            Long roleId
    ) {
        return roleChargeAssignmentJpa.existsByChargeDefinitionIdAndRoleId(
                chargeDefinitionId,
                roleId
        );
    }

    @Override
    public boolean existsEventParticipantsAssignmentByChargeDefinitionIdAndEventId(
            Long chargeDefinitionId,
            Long eventId
    ) {
        return eventParticipantsChargeAssignmentJpa.existsByChargeDefinitionIdAndEventId(
                chargeDefinitionId,
                eventId
        );
    }
}