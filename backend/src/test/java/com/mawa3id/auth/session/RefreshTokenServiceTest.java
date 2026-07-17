package com.mawa3id.auth.session;

import com.mawa3id.auth.AuthProperties;
import com.mawa3id.auth.support.Secrets;
import com.mawa3id.user.Role;
import com.mawa3id.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private AuthSessionRepository repository;

    private final AuthProperties properties = new AuthProperties();
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T10:00:00Z"), ZoneOffset.UTC);
    private final User user = new User("bob", "bob@example.com", "hash", Role.DOCTOR);

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, properties, CLOCK);
        lenient().when(repository.save(any(AuthSession.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private AuthSession session(Instant expiresAt) {
        return new AuthSession(user, "stored-hash", "fam-1", "Pixel", "ua", "ip", expiresAt);
    }

    @Test
    void issuePersistsHashedTokenWithConfiguredExpiry() {
        RefreshTokenService.IssuedRefresh issued = service.issue(user, "Pixel", "ua", "ip");

        ArgumentCaptor<AuthSession> captor = ArgumentCaptor.forClass(AuthSession.class);
        verify(repository).save(captor.capture());
        AuthSession saved = captor.getValue();
        // The raw token is returned but only its hash is stored.
        assertThat(saved.getRefreshTokenHash()).isEqualTo(Secrets.sha256Hex(issued.token()));
        assertThat(saved.getExpiresAt())
                .isEqualTo(CLOCK.instant().plus(properties.getRefresh().getTtlDays(), ChronoUnit.DAYS));
    }

    @Test
    void rotateUnknownTokenIsInvalid() {
        when(repository.findByRefreshTokenHash(any())).thenReturn(Optional.empty());

        RefreshTokenService.RotationResult result = service.rotate("tok", "ua", "ip");

        assertThat(result.status()).isEqualTo(RefreshTokenService.RotationResult.Status.INVALID);
    }

    @Test
    void rotateActiveTokenIssuesReplacementAndRevokesOld() {
        AuthSession current = session(CLOCK.instant().plus(1, ChronoUnit.DAYS));
        when(repository.findByRefreshTokenHash(Secrets.sha256Hex("tok"))).thenReturn(Optional.of(current));

        RefreshTokenService.RotationResult result = service.rotate("tok", "ua2", "ip2");

        assertThat(result.status()).isEqualTo(RefreshTokenService.RotationResult.Status.ROTATED);
        assertThat(result.token()).isNotBlank();
        assertThat(current.isRevoked()).isTrue();
        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void rotateRotatedTokenReplayRevokesFamily() {
        AuthSession current = session(CLOCK.instant().plus(1, ChronoUnit.DAYS));
        current.revoke(CLOCK.instant());
        current.setReplacedById(42L); // was rotated -> replay is theft
        when(repository.findByRefreshTokenHash(Secrets.sha256Hex("tok"))).thenReturn(Optional.of(current));

        RefreshTokenService.RotationResult result = service.rotate("tok", "ua", "ip");

        assertThat(result.status()).isEqualTo(RefreshTokenService.RotationResult.Status.REUSE);
        assertThat(result.user()).isEqualTo(user);
        verify(repository).revokeFamily(eq("fam-1"), any());
    }

    @Test
    void rotateLoggedOutTokenIsInvalidWithoutFamilyRevocation() {
        AuthSession current = session(CLOCK.instant().plus(1, ChronoUnit.DAYS));
        current.revoke(CLOCK.instant()); // revoked by logout, never replaced
        when(repository.findByRefreshTokenHash(Secrets.sha256Hex("tok"))).thenReturn(Optional.of(current));

        RefreshTokenService.RotationResult result = service.rotate("tok", "ua", "ip");

        assertThat(result.status()).isEqualTo(RefreshTokenService.RotationResult.Status.INVALID);
        verify(repository, never()).revokeFamily(any(), any());
    }

    @Test
    void rotateExpiredTokenIsInvalid() {
        AuthSession current = session(CLOCK.instant().minus(1, ChronoUnit.DAYS));
        when(repository.findByRefreshTokenHash(Secrets.sha256Hex("tok"))).thenReturn(Optional.of(current));

        RefreshTokenService.RotationResult result = service.rotate("tok", "ua", "ip");

        assertThat(result.status()).isEqualTo(RefreshTokenService.RotationResult.Status.INVALID);
    }

    @Test
    void revokeReturnsOwnerAndMarksRevoked() {
        AuthSession current = session(CLOCK.instant().plus(1, ChronoUnit.DAYS));
        when(repository.findByRefreshTokenHash(Secrets.sha256Hex("tok"))).thenReturn(Optional.of(current));

        Optional<User> owner = service.revoke("tok");

        assertThat(owner).contains(user);
        assertThat(current.isRevoked()).isTrue();
    }

    @Test
    void revokeUnknownTokenReturnsEmpty() {
        when(repository.findByRefreshTokenHash(any())).thenReturn(Optional.empty());

        assertThat(service.revoke("tok")).isEmpty();
    }
}
