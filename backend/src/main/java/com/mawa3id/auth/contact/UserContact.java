package com.mawa3id.auth.contact;

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
 * A verified-or-pending email/phone identifier owned by a user. This is the source
 * of truth for login identifiers and password-recovery eligibility; {@code users.email}
 * is a nullable mirror of the primary EMAIL contact kept for backwards compatibility.
 */
@Entity
@Table(name = "user_contacts")
public class UserContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ContactType type;

    // Display form as entered (e.g. "User@Example.com").
    @Column(name = "contact_value", nullable = false)
    private String contactValue;

    // Canonical form used for uniqueness and lookup: lowercased email / E.164 phone.
    @Column(name = "normalized_value", nullable = false)
    private String normalizedValue;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserContact() {
    }

    public UserContact(User user, ContactType type, String contactValue, String normalizedValue,
                       boolean primary) {
        this.user = user;
        this.type = type;
        this.contactValue = contactValue;
        this.normalizedValue = normalizedValue;
        this.primary = primary;
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

    public ContactType getType() {
        return type;
    }

    public String getContactValue() {
        return contactValue;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void markVerified(Instant when) {
        this.verifiedAt = when;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
