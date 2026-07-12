-- Iteration 3: donations (card via Stripe)

CREATE TABLE donations (
    id                   BIGSERIAL    PRIMARY KEY,
    -- Optional donor. ON DELETE SET NULL keeps the donation record for accounting
    -- even if the user account is later removed.
    user_id              BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    amount_minor         BIGINT       NOT NULL,
    currency             VARCHAR(3)   NOT NULL,
    status               VARCHAR(20)  NOT NULL,
    provider             VARCHAR(20)  NOT NULL,
    provider_session_id  VARCHAR(255) UNIQUE,
    provider_payment_ref VARCHAR(255),
    donor_name           VARCHAR(120),
    message              VARCHAR(500),
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP
);

CREATE INDEX idx_donations_user    ON donations (user_id);
CREATE INDEX idx_donations_session ON donations (provider_session_id);
