package com.jeepclub.backend.iam.identity.core.application.service.user;

import com.jeepclub.backend.iam.identity.api.module.UserDetails;
import com.jeepclub.backend.iam.identity.api.module.UserIdentityLock;
import com.jeepclub.backend.iam.identity.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
class UserIdentityLockService implements UserIdentityLock {
    private final UserRepository users;
    @Override
    public Optional<UserDetails> byId(Long id) {
        return users.findByIdForUpdate(id).map(UserQueryService::toDetails);
    }
    @Override
    public Optional<UserDetails> byCpf(String cpf) {
        return users.findByCpfForUpdate(cpf).map(UserQueryService::toDetails);
    }
}
