package com.jeepclub.backend.memberships.core.port;

public interface MemberActivationTokenHashPort {

    String hash(String rawToken);
}
