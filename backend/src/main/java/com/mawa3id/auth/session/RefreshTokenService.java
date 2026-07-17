package com.mawa3id.auth.session;

import com.mawa3id.auth.AuthProperties;
import com.mawa3id.auth.support.Secrets;
import com.mawa3id.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages refresh-token sessions: issuing them at login, rotating them on use, and
 * revoking them on logout. Only the SHA-256 hash of each token is stored.
 *
 * <p>Rotation returns a {@link RotationResult} rather than throwing on failure so the
 * important side effect — revoking a whole family when a rotated token is replayed —
 * commits with the surrounding transaction instead of being rolled back by a thrown
 * exception. The caller maps the outcome to an HTTP response and audits it.
 */
@Service
public class RefreshTokenService {

    private final AuthSessionRepository repository;
    private final AuthProperties properties;
    private final Clock clock;

    public RefreshTokenService(AuthSessionRepository repository, AuthProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    /** A freshly issued refresh token paired with its persisted session. */
    public record IssuedRefresh(String token, AuthSession session) {
    }

    /** Outcome of a rotation attempt. Only {@code ROTATED} carries a new token. */
    public record RotationResult(Status status, String token, AuthSession session, User user) {
        public enum Status { ROTATED, REUSE, INVALID }

        public static RotationResult rotated(String token, AuthSession session, User user) {
            return new RotationResult(Status.ROTATED, token, session, user);
        }

        public static RotationResult reuse(User user) {
            return new RotationResult(Status.REUSE, null, null, user);
        }

        public static RotationResult invalid() {
            return new RotationResult(Status.INVALID, null, null, null);
        }
    }

    @Transactional
    public IssuedRefresh issue(User user, String deviceName, String userAgent, String ipAddress) {
        String token = Secrets.randomToken();
        Instant now = clock.instant();
        AuthSession session = new AuthSession(user, Secrets.sha256Hex(token), UUID.randomUUID().toString(),
                deviceName, userAgent, ipAddress, expiryFrom(now));
        session.touch(now);
        repository.save(session);
        return new IssuedRefresh(token, session);
    }

    @Transactional
    public RotationResult rotate(String presentedToken, String userAgent, String ipAddress) {
        Instant now = clock.instant();
        Optional<AuthSession> found = repository.findByRefreshTokenHash(Secrets.sha256Hex(presentedToken));
        if (found.isEmpty()) {
            return RotationResult.invalid();
        }
        AuthSession session = found.get();

        if (session.isRevoked()) {
            // A token that was already rotated (has a replacement) is being replayed:
            // treat as theft and revoke the entire family. A token revoked by an explicit
            // logout has no replacement and is simply rejected.
            if (session.getReplacedById() != null) {
                repository.revokeFamily(session.getFamilyId(), now);
                return RotationResult.reuse(session.getUser());
            }
            return RotationResult.invalid();
        }
        if (session.isExpired(now)) {
            return RotationResult.invalid();
        }

        String newToken = Secrets.randomToken();
        AuthSession replacement = new AuthSession(session.getUser(), Secrets.sha256Hex(newToken),
                session.getFamilyId(), session.getDeviceName(), userAgent, ipAddress, expiryFrom(now));
        replacement.touch(now);
        repository.save(replacement);

        session.revoke(now);
        session.setReplacedById(replacement.getId());
        repository.save(session);

        return RotationResult.rotated(newToken, replacement, session.getUser());
    }

    /** Revoke a single session by its token (logout). Idempotent; returns the owner if found. */
    @Transactional
    public Optional<User> revoke(String presentedToken) {
        Instant now = clock.instant();
        return repository.findByRefreshTokenHash(Secrets.sha256Hex(presentedToken))
                .map(session -> {
                    session.revoke(now);
                    repository.save(session);
                    return session.getUser();
                });
    }

    /** Revoke every live session for a user (logout everywhere / password change). */
    @Transactional
    public int revokeAllForUser(Long userId) {
        return repository.revokeAllForUser(userId, clock.instant());
    }

    private Instant expiryFrom(Instant now) {
        return now.plus(properties.getRefresh().getTtlDays(), ChronoUnit.DAYS);
    }
}
