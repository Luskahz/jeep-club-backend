package com.jeepclub.backend.membershipKauan.infra.persistence.jpa;

import com.jeepclub.backend.membershipKauan.infra.persistence.entity.DependentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DependentJpaRepository extends JpaRepository<DependentEntity, Long> {
    List<DependentEntity> findAllBySocioId(Long socioId);
    boolean existsByCpf(String cpf);
    boolean existsByCpfAndIdNot(String cpf, Long id);
}
