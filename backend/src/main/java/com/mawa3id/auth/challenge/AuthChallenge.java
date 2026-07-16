package com.mawa3id.auth.challenge;

import com.mawa3id.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single-use, short-lived secret backing email verification, phone verification
 * and password reset. Only the SHA-256 hash of the secret is stored; the raw value
 * lives only in the delivered message. Verification is rate-limited by
 * {@code attempt_count}/{@code max_attempts} and bounded by {@code expires_at}.
 */
@Entity
@Table(name = "auth_challenges")
public class AuthChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable: a reset requested for an unknown identifier still records a row so
    // timing/behaviour is uniform, but it is not tied to a user.
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChallengePurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChallengeChannel channel;

    // Normalized destination the secret was sent to (lowercased email / E.164 phone).
    @Column
    private String destination;

    // SHA-256 hex of the raw secret.
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthChallenge() {
    }

    public AuthChallenge(User user, ChallengePurpose purpose, ChallengeChannel channel,
                         String destination, String tokenHash, Instant expiresAt, int maxAttempts) {
        this.user = user;
        this.purpose = purpose;
        this.channel = channel;
        this.destination = destination;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.maxAttempts = maxAttempts;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ChallengePurpose getPurpose() {
        return purpose;
    }

    public ChallengeChannel getChannel() {
        return channel;
    }

    public String getDestination() {
        return destination;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void incrementAttempts() {
        this.attemptCount++;
    }

    public boolean attemptsExhausted() {
        return attemptCount >= maxAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public void consume(Instant when) {
        this.consumedAt = when;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsable(Instant now) {
        return !isConsumed() && !isExpired(now) && !attemptsExhausted();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
