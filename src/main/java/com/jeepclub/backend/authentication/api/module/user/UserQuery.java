package com.jeepclub.backend.authentication.api.module.user;

import java.util.List;

public interface UserQuery {

    boolean existsById(Long userId);

    List<Long> findActiveUserIds();

    boolean existsActiveUserById(Long userId);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}