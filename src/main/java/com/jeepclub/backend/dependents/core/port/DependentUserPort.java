package com.jeepclub.backend.dependents.core.port;

public interface DependentUserPort {

    boolean existsById(Long userId);

    boolean existsActiveById(Long userId);

    boolean existsByCpf(String cpf);
}
