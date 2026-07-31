-- App-level feedback from signed-in users (distinct from per-appointment doctor reviews).

CREATE TABLE feedback (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category    VARCHAR(20)  NOT NULL,
    message     VARCHAR(2000) NOT NULL,
    app_version VARCHAR(40),
    platform    VARCHAR(20),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_feedback_user ON feedback (user_id);
CREATE INDEX idx_feedback_created_at ON feedback (created_at);
