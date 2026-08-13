package com.jeepclub.backend.memberships.core.port;

public interface UserExistencePort {

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}