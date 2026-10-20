package com.jeepclub.backend.memberships.infra.mail;

import com.jeepclub.backend.memberships.core.port.MemberActivationLinkFactory;
import com.jeepclub.backend.memberships.core.port.MembershipActivationUrlProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class ConfiguredMemberActivationLinkFactory implements MemberActivationLinkFactory {
    private final MembershipActivationUrlProperties properties;
    @Override public String create(String rawToken) {
        return UriComponentsBuilder.fromUriString(properties.activationUrl())
                .replaceQueryParam("token", "{token}").encode().buildAndExpand(rawToken).toUriString();
    }
}
