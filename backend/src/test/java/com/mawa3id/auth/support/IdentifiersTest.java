package com.mawa3id.auth.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentifiersTest {

    @Test
    void classifiesByShape() {
        assertThat(Identifiers.classify("alice@example.com")).isEqualTo(Identifiers.Kind.EMAIL);
        assertThat(Identifiers.classify("+212 600-000-000")).isEqualTo(Identifiers.Kind.PHONE);
        assertThat(Identifiers.classify("alice.doe")).isEqualTo(Identifiers.Kind.USERNAME);
        assertThat(Identifiers.classify(null)).isEqualTo(Identifiers.Kind.USERNAME);
    }

    @Test
    void normalizesEmailAndUsername() {
        assertThat(Identifiers.normalizeEmail("  Alice@Example.COM ")).isEqualTo("alice@example.com");
        assertThat(Identifiers.normalizeUsername("  Alice.Doe ")).isEqualTo("alice.doe");
    }

    @Test
    void normalizesPhoneStrippingFormatting() {
        assertThat(Identifiers.normalizePhone(" +212 (600) 00-00-00 ")).isEqualTo("+212600000000");
        assertThat(Identifiers.normalizePhone("0612345678")).isEqualTo("0612345678");
    }

    @Test
    void rejectsInvalidPhone() {
        assertThatThrownBy(() -> Identifiers.normalizePhone("12"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Identifiers.normalizePhone("not-a-phone"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validatesEmailAndUsername() {
        assertThat(Identifiers.isValidEmail("a@b.co")).isTrue();
        assertThat(Identifiers.isValidEmail("bad")).isFalse();
        assertThat(Identifiers.isValidUsername("ab")).isFalse();
        assertThat(Identifiers.isValidUsername("good.name_1")).isTrue();
    }

    @Test
    void masksIdentifiersRecognizablyButNotFully() {
        assertThat(Identifiers.mask("alice@example.com")).contains("@example.com").doesNotContain("alice@");
        assertThat(Identifiers.mask("+2126000000")).startsWith("+2").endsWith("00").contains("*");
        assertThat(Identifiers.mask("ab")).isEqualTo("a*");
        assertThat(Identifiers.mask("")).isEmpty();
        assertThat(Identifiers.mask(null)).isEmpty();
    }
}
