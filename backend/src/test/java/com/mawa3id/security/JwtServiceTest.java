package com.mawa3id.security;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Fresh random Base64 secrets per run — no credentials committed to source.
    private static final String SECRET = randomSecret();
    private static final String OTHER_SECRET = randomSecret();

    private final JwtService jwtService = new JwtService(SECRET, 86_400_000L, false);

    private static String randomSecret() {
        byte[] bytes = new byte[48]; // 384-bit key
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void generatesTokenAndExtractsSubject() {
        String token = jwtService.generateToken("alice@example.com",
                Map.of("uid", 1L, "role", "PATIENT"));

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractSubject(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("alice@example.com", Map.of());
        // Flip the last character of the signature.
        char last = token.charAt(token.length() - 1);
        String tampered = token.substring(0, token.length() - 1) + (last == 'a' ? 'b' : 'a');

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void rejectsGarbageString() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        // Negative expiry -> token is already expired the moment it is issued.
        JwtService shortLived = new JwtService(SECRET, -1_000L, false);
        String token = shortLived.generateToken("alice@example.com", Map.of());

        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService otherSigner = new JwtService(OTHER_SECRET, 86_400_000L, false);
        String foreignToken = otherSigner.generateToken("alice@example.com", Map.of());

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void exposesConfiguredExpiration() {
        assertThat(jwtService.getExpirationMs()).isEqualTo(86_400_000L);
    }

    @Test
    void failsFastWhenSecretMissingAndRequired() {
        // Simulates a production boot with JWT_FAIL_ON_MISSING_SECRET=true and no secret.
        assertThatThrownBy(() -> new JwtService("  ", 86_400_000L, true))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generatesEphemeralKeyWhenSecretMissingAndNotRequired() {
        // Default (local/dev/test) behavior: no secret, ephemeral key, still usable.
        JwtService ephemeral = new JwtService("", 86_400_000L, false);
        String token = ephemeral.generateToken("alice@example.com", Map.of());

        assertThat(ephemeral.isValid(token)).isTrue();
        assertThat(ephemeral.extractSubject(token)).isEqualTo("alice@example.com");
    }
}
