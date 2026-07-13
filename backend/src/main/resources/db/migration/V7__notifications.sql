-- Iteration 6: in-app notifications & reminders

CREATE TABLE notifications (
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type           VARCHAR(40)  NOT NULL,
    message        VARCHAR(500) NOT NULL,
    appointment_id BIGINT       REFERENCES appointments (id) ON DELETE CASCADE,
    read_at        TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_unread    ON notifications (user_id) WHERE read_at IS NULL;
