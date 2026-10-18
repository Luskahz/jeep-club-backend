package com.jeepclub.backend.memberships.infra.integration.identity;
import com.jeepclub.backend.memberships.core.port.MemberIdentityPort;
import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import com.jeepclub.backend.iam.identity.api.module.UserIdentityLock;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class MemberIdentityAdapter implements MemberIdentityPort {
    private final UserIdentityLock identities;
    private final UserQuery query;
    public boolean exists(Long id) { return query.existsById(id); }
    public boolean isActive(Long id) { return query.isAdministrativelyActive(id); }
    public boolean lockAndIsActive(Long id) {
        return identities.byId(id).orElseThrow(() -> new MemberRegistryException(MemberRegistryException.Kind.NOT_FOUND)).administrativelyActive();
    }
}
