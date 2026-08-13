package com.jeepclub.backend.memberships.infra.persistence.adapter;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplicantBlock;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicantBlockRepository;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipApplicantBlockJpaRepository;
import com.jeepclub.backend.memberships.infra.persistence.mapper.MembershipApplicantBlockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MembershipApplicantBlockRepositoryAdapter
        implements MembershipApplicantBlockRepository {

    private final MembershipApplicantBlockJpaRepository jpaRepository;

    @Override
    public MembershipApplicantBlock save(MembershipApplicantBlock block) {
        return MembershipApplicantBlockMapper.toDomain(
                jpaRepository.save(MembershipApplicantBlockMapper.toEntity(block))
        );
    }

    @Override
    public boolean existsActiveByCpf(String cpf) {
        return jpaRepository.existsByCpfAndUnblockedAtIsNull(cpf);
    }

    @Override
    public Optional<MembershipApplicantBlock> findActiveByCpf(String cpf) {
        return jpaRepository.findByCpfAndUnblockedAtIsNull(cpf)
                .map(MembershipApplicantBlockMapper::toDomain);
    }
}
