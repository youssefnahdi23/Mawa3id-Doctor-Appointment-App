package com.mawa3id.donation;

import com.mawa3id.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A single donation towards the project. Donations may be anonymous ({@link #user}
 * {@code null}) or attributed to a registered {@link User}. The lifecycle is driven by
 * the payment provider: a record is created {@link DonationStatus#PENDING} alongside a
 * checkout session, then transitions on the provider's webhook.
 */
@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optional donor. Null for anonymous donations. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Amount in the currency's minor units (e.g. cents). */
    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationProvider provider;

    /** Provider checkout-session identifier (Stripe Checkout Session id). */
    @Column(name = "provider_session_id", unique = true)
    private String providerSessionId;

    /** Provider payment reference set once the payment succeeds (Stripe PaymentIntent id). */
    @Column(name = "provider_payment_ref")
    private String providerPaymentRef;

    @Column(name = "donor_name", length = 120)
    private String donorName;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Donation() {
    }

    public Donation(User user, long amountMinor, String currency, DonationProvider provider,
                    String donorName, String message) {
        this.user = user;
        this.amountMinor = amountMinor;
        this.currency = currency;
        this.provider = provider;
        this.donorName = donorName;
        this.message = message;
        this.status = DonationStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Record the provider checkout-session id once the session has been created. */
    public void attachSession(String providerSessionId) {
        this.providerSessionId = providerSessionId;
        touch();
    }

    /** Mark the donation as paid, capturing the provider payment reference. */
    public void markSucceeded(String providerPaymentRef) {
        this.status = DonationStatus.SUCCEEDED;
        this.providerPaymentRef = providerPaymentRef;
        touch();
    }

    public void markFailed() {
        this.status = DonationStatus.FAILED;
        touch();
    }

    public void markExpired() {
        this.status = DonationStatus.EXPIRED;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public DonationProvider getProvider() {
        return provider;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public String getProviderPaymentRef() {
        return providerPaymentRef;
    }

    public String getDonorName() {
        return donorName;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
