package com.jeepclub.backend.vehicles.infra.integration.identity;

import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.vehicles.core.port.UserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleIdentityAdapter implements UserPort {

    private final UserQuery identityQuery;

    @Override
    public java.util.Map<Long, String> findNamesByIds(java.util.Collection<Long> userIds) {
        if (userIds.isEmpty()) return java.util.Map.of();
        return identityQuery.findByIds(userIds).stream().collect(java.util.stream.Collectors.toMap(
                com.jeepclub.backend.iam.identity.api.module.UserDetails::id,
                com.jeepclub.backend.iam.identity.api.module.UserDetails::name));
    }

    @Override
    public boolean existsById(Long userId) {
        return identityQuery.existsById(userId);
    }

    @Override
    public boolean existsActiveById(Long userId) {
        return identityQuery.isAdministrativelyActive(userId);
    }
}
