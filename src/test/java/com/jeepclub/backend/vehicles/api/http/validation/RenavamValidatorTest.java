package com.jeepclub.backend.vehicles.api.http.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RenavamValidatorTest {

    private final RenavamValidator validator = new RenavamValidator();

    @Test
    void acceptsOnlyTheDocumentedDigitAndPunctuatedSyntaxesBeforeChecksumValidation() {
        assertThat(validator.isValid("38249206428", null)).isTrue();
        assertThat(validator.isValid("382.492.064-28", null)).isTrue();
    }

    @Test
    void rejectsCharactersThatWouldOtherwiseBeSilentlyDiscardedDuringCanonicalization() {
        assertThat(validator.isValid("3x8249206428", null)).isFalse();
        assertThat(validator.isValid("abc38249206428", null)).isFalse();
        assertThat(validator.isValid("38249💥206428", null)).isFalse();
        assertThat(validator.isValid("382-492-064-28", null)).isFalse();
    }
}
