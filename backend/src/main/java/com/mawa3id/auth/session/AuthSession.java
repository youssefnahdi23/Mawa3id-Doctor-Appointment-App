package com.mawa3id.auth.session;

import com.mawa3id.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A refresh-token session. Only the SHA-256 hash of the refresh token is stored.
 * Tokens rotate on every use: the old session is revoked and points at its
 * replacement via {@code replaced_by_id}. All sessions produced by a single login
 * share a {@code family_id}; presenting an already-rotated (revoked) token is treated
 * as theft and revokes the whole family.
 */
@Entity
@Table(name = "auth_sessions")
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    protected AuthSession() {
    }

    public AuthSession(User user, String refreshTokenHash, String familyId, String deviceName,
                       String userAgent, String ipAddress, Instant expiresAt) {
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.familyId = familyId;
        this.deviceName = deviceName;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
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

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public String getFamilyId() {
        return familyId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void touch(Instant when) {
        this.lastUsedAt = when;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant when) {
        if (revokedAt == null) {
            this.revokedAt = when;
        }
    }

    public Long getReplacedById() {
        return replacedById;
    }

    public void setReplacedById(Long replacedById) {
        this.replacedById = replacedById;
    }

    /** Live = neither revoked nor past expiry; the only state a refresh is accepted in. */
    public boolean isActive(Instant now) {
        return !isRevoked() && !isExpired(now);
    }
}
