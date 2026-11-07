package com.jeepclub.backend.authentication.infra.adapter;

import com.jeepclub.backend.iam.authentication.infra.adapter.SecureRandomManualTemporaryPasswordGenerator;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SecureRandomManualTemporaryPasswordGeneratorTest {
    @Test void producesManualFormatIncludingLeadingZeroes() {
        var generator = new SecureRandomManualTemporaryPasswordGenerator();
        var passwords = java.util.stream.IntStream.range(0, 1000).mapToObj(i -> generator.generate()).toList();
        assertThat(passwords).allMatch(p -> p.matches("[a-z]{5}[0-9]{3}"));
        assertThat(passwords).anyMatch(p -> p.charAt(5) == '0');
        assertThat(passwords.stream().distinct().count()).isGreaterThan(990);
    }
}
