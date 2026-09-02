package com.jeepclub.backend.vehicles.infra.integration.identity;

import com.jeepclub.backend.identity.api.module.IdentityQuery;
import com.jeepclub.backend.vehicles.core.port.UserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleIdentityAdapter implements UserPort {

    private final IdentityQuery identityQuery;

    @Override
    public boolean existsById(Long userId) {
        return identityQuery.existsById(userId);
    }
}
