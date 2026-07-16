package com.mawa3id.auth.challenge;

import com.mawa3id.auth.AuthProperties;
import com.mawa3id.auth.support.Secrets;
import com.mawa3id.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock private AuthChallengeRepository repository;

    private final AuthProperties properties = new AuthProperties();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T10:00:00Z"), ZoneOffset.UTC);

    private ChallengeService service;

    @BeforeEach
    void setUp() {
        service = new ChallengeService(repository, properties, CLOCK);
    }

    private AuthChallenge challenge(String secret, ChallengePurpose purpose, int maxAttempts, Instant expiresAt) {
        return new AuthChallenge(null, purpose, ChallengeChannel.PHONE, "+212600000000",
                Secrets.sha256Hex(secret), expiresAt, maxAttempts);
    }

    @Test
    void issueCodePersistsHashedSecretWithTtl() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String code = service.issueCode(null, ChallengePurpose.VERIFY_PHONE,
                ChallengeChannel.PHONE, "+212600000000");

        ArgumentCaptor<AuthChallenge> captor = ArgumentCaptor.forClass(AuthChallenge.class);
        verify(repository).save(captor.capture());
        assertThat(code).hasSize(properties.getChallenge().getCodeDigits());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(Secrets.sha256Hex(code));
        assertThat(captor.getValue().getExpiresAt())
                .isEqualTo(CLOCK.instant().plus(properties.getChallenge().getTtlSeconds(), ChronoUnit.SECONDS));
    }

    @Test
    void verifyCodeConsumesOnMatch() {
        AuthChallenge c = challenge("123456", ChallengePurpose.VERIFY_PHONE, 5,
                CLOCK.instant().plusSeconds(300));
        when(repository.findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(
                ChallengePurpose.VERIFY_PHONE, "+212600000000")).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthChallenge result = service.verifyCode(ChallengePurpose.VERIFY_PHONE, "+212600000000", "123456");

        assertThat(result.isConsumed()).isTrue();
    }

    @Test
    void verifyCodeWrongGuessBurnsAttemptAndRejects() {
        AuthChallenge c = challenge("123456", ChallengePurpose.VERIFY_PHONE, 5,
                CLOCK.instant().plusSeconds(300));
        when(repository.findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(
                ChallengePurpose.VERIFY_PHONE, "+212600000000")).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.verifyCode(ChallengePurpose.VERIFY_PHONE, "+212600000000", "000000"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(c.getAttemptCount()).isEqualTo(1);
        assertThat(c.isConsumed()).isFalse();
    }

    @Test
    void verifyCodeExhaustingAttemptsIsTooManyRequests() {
        AuthChallenge c = challenge("123456", ChallengePurpose.VERIFY_PHONE, 1,
                CLOCK.instant().plusSeconds(300));
        when(repository.findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(
                ChallengePurpose.VERIFY_PHONE, "+212600000000")).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.verifyCode(ChallengePurpose.VERIFY_PHONE, "+212600000000", "000000"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getStatus())
                        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
    }

    @Test
    void verifyCodeExpiredChallengeIsRejected() {
        AuthChallenge c = challenge("123456", ChallengePurpose.VERIFY_PHONE, 5,
                CLOCK.instant().minusSeconds(1));
        when(repository.findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(
                ChallengePurpose.VERIFY_PHONE, "+212600000000")).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.verifyCode(ChallengePurpose.VERIFY_PHONE, "+212600000000", "123456"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void verifyCodeNoChallengeIsRejected() {
        when(repository.findFirstByPurposeAndDestinationAndConsumedAtIsNullOrderByCreatedAtDesc(
                any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCode(ChallengePurpose.VERIFY_PHONE, "+212600000000", "123456"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void verifyTokenConsumesOnMatch() {
        AuthChallenge c = challenge("the-token", ChallengePurpose.RESET_EMAIL, 5,
                CLOCK.instant().plusSeconds(300));
        when(repository.findByPurposeAndTokenHash(eq(ChallengePurpose.RESET_EMAIL),
                eq(Secrets.sha256Hex("the-token")))).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthChallenge result = service.verifyToken(ChallengePurpose.RESET_EMAIL, "the-token");

        assertThat(result.isConsumed()).isTrue();
    }

    @Test
    void verifyTokenUnknownIsRejected() {
        when(repository.findByPurposeAndTokenHash(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyToken(ChallengePurpose.RESET_EMAIL, "nope"))
                .isInstanceOf(ApiException.class);
    }
}
