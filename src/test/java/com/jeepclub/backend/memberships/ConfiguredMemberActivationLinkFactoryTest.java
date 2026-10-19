package com.jeepclub.backend.memberships;

import com.jeepclub.backend.memberships.infra.mail.ConfiguredMemberActivationLinkFactory;
import com.jeepclub.backend.memberships.core.port.AccessLinkApproval;
import com.jeepclub.backend.memberships.api.http.dto.AccessLinkApprovalResponseDTO;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredMemberActivationLinkFactoryTest {
    @Test void configuredUrlKeepsExistingParametersAndStrictlyEncodesTheSingleBearerParameter() {
        var factory = new ConfiguredMemberActivationLinkFactory(() -> "https://app.example/activate?lang=pt&token=stale");
        assertThat(factory.create("a+b/&=?"))
                .isEqualTo("https://app.example/activate?lang=pt&token=a%2Bb%2F%26%3D%3F");
    }
    @Test void ephemeralResponsesRedactTheirStringRepresentation() {
        String secret = "https://app.example/activate?token=bearer-test";
        assertThat(new AccessLinkApproval(12L, secret).toString()).doesNotContain(secret, "bearer-test");
        assertThat(new AccessLinkApprovalResponseDTO(12L, secret).toString()).doesNotContain(secret, "bearer-test");
    }
}
