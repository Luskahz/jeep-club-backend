package com.jeepclub.backend.dependents.infra.integration.identity;

import com.jeepclub.backend.dependents.core.port.DependentUserPort;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentIdentityAdapter implements DependentUserPort {
    private final com.jeepclub.backend.iam.identity.api.module.UserIdentityLock identityLock;


    private final UserQuery identityQuery;

    @Override
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void lockHistoryIdentities(Long ownerId, Long actorId) {
        java.util.stream.Stream.of(ownerId, actorId).distinct().sorted().forEach(id ->
                identityLock.byId(id).orElseThrow(() ->
                        new com.jeepclub.backend.iam.identity.api.module.exception.UserNotFoundException(id)));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public boolean existsById(Long userId) {
        return identityLock.byId(userId).isPresent();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public boolean existsActiveById(Long userId) {
        return identityLock.byId(userId).map(user -> user.administrativelyActive()).orElse(false);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return identityQuery.existsByCpf(cpf);
    }
}
