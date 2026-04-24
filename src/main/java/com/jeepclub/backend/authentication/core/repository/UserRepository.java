package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.User;

import java.util.Optional;

/**
 * Interface do Repositório (Porta de Saída / Outbound Port) para a entidade User.
 * Define APENAS a abstração de persistência. A implementação será feita na Infraestrutura.
 */
public interface UserRepository {
    
    User save(User user);
    
    Optional<User> findById(Long id);
    
    Optional<User> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
    
}
