package com.jeepclub.backend.platform.security.authorization;

import java.util.List;

public interface UserAuthoritiesProvider {

    List<String> findAuthorityCodesByUserId(Long userId);
}