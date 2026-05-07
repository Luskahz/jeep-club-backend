package com.jeepclub.backend.infra.security.authorization;

import java.util.List;

public interface UserAuthoritiesProvider {

    List<String> findAuthorityCodesByUserId(Long userId);
}