package com.mawa3id.feedback;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A piece of app-level feedback submitted by a signed-in user. Unlike a
 * {@link com.mawa3id.review.Review} (which rates a specific appointment/doctor),
 * this is about the product itself. The submitter is kept as a plain user id for
 * lightweight auditing; {@code appVersion}/{@code platform} help triage reports.
 */
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackCategory category;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(length = 20)
    private String platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Feedback() {
    }

    public Feedback(Long userId, FeedbackCategory category, String message,
                    String appVersion, String platform) {
        this.userId = userId;
        this.category = category;
        this.message = message;
        this.appVersion = appVersion;
        this.platform = platform;
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

    public Long getUserId() {
        return userId;
    }

    public FeedbackCategory getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getPlatform() {
        return platform;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
