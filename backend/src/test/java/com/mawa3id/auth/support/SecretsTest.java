package com.mawa3id.auth.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretsTest {

    @Test
    void sha256HexIsDeterministicAndHexEncoded() {
        assertThat(Secrets.sha256Hex("abc")).isEqualTo(Secrets.sha256Hex("abc"));
        assertThat(Secrets.sha256Hex("abc")).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(Secrets.sha256Hex("abc")).isNotEqualTo(Secrets.sha256Hex("abd"));
    }

    @Test
    void randomTokenIsUrlSafeAndUnique() {
        String a = Secrets.randomToken();
        String b = Secrets.randomToken();
        assertThat(a).isNotEqualTo(b);
        assertThat(a).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void numericCodeHasRequestedLength() {
        for (int i = 0; i < 50; i++) {
            String code = Secrets.numericCode(6);
            assertThat(code).hasSize(6).matches("[0-9]{6}");
        }
    }

    @Test
    void numericCodeRejectsOutOfRangeLengths() {
        assertThatThrownBy(() -> Secrets.numericCode(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Secrets.numericCode(10)).isInstanceOf(IllegalArgumentException.class);
    }
}
