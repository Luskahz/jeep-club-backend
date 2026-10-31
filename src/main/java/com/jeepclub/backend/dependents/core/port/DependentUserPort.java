package com.jeepclub.backend.dependents.core.port;

public interface DependentUserPort {

    /** Lock retained history identities before taking a dependent lock. */
    void lockHistoryIdentities(Long ownerId, Long actorId);

    boolean existsById(Long userId);

    boolean existsActiveById(Long userId);

    boolean existsByCpf(String cpf);
}
