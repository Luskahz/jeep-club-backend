package com.jeepclub.backend.platform.logging;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientPlatformResolverTest {

    private final ClientPlatformResolver resolver = new ClientPlatformResolver();

    @ParameterizedTest
    @CsvSource({"WEB,WEB", "android,ANDROID", "Ios,IOS"})
    void normalizesSupportedExplicitHeader(String supplied, ClientPlatform expected) {
        var request = requestWithHeader(supplied);

        assertThat(resolver.resolve(request)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"DESKTOP", "'web browser'", "'XXXXXXXXXXXXXXXXX'"})
    void mapsUnknownOrOversizedValuesToUnknown(String supplied) {
        assertThat(resolver.resolve(requestWithHeader(supplied)))
                .isEqualTo(ClientPlatform.UNKNOWN);
    }

    @Test
    void keepsLegacyClientsWithoutHeaderCompatibleAndDoesNotInspectUserAgent() {
        var request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "sensitive-full-user-agent");

        assertThat(resolver.resolve(request)).isEqualTo(ClientPlatform.UNKNOWN);
    }

    private MockHttpServletRequest requestWithHeader(String value) {
        var request = new MockHttpServletRequest();
        request.addHeader(ClientPlatformResolver.HEADER_NAME, value);
        return request;
    }
}
