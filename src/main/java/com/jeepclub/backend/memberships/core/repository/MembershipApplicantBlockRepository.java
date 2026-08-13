package com.jeepclub.backend.memberships.core.repository;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplicantBlock;

import java.util.Optional;

public interface MembershipApplicantBlockRepository {

    MembershipApplicantBlock save(MembershipApplicantBlock block);

    boolean existsActiveByCpf(String cpf);

    Optional<MembershipApplicantBlock> findActiveByCpf(String cpf);
}
