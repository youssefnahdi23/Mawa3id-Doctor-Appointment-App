package com.mawa3id.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TunisianPhoneValidatorTest {

    private final TunisianPhoneValidator validator = new TunisianPhoneValidator();

    @ParameterizedTest
    @ValueSource(strings = {"20123456", "+21620123456", "+216 20 123 456", "98765432"})
    void acceptsTunisianNumbers(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "+212600000000", "2012345", "201234567", "abcdefgh", "+21612"})
    void rejectsNonTunisianNumbers(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void treatsNullAndBlankAsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
