-- Iteration 5: doctor reviews & ratings

CREATE TABLE reviews (
    id             BIGSERIAL     PRIMARY KEY,
    appointment_id BIGINT        NOT NULL UNIQUE REFERENCES appointments (id) ON DELETE CASCADE,
    rating         INT           NOT NULL,
    comment        VARCHAR(2000),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reviews_appointment ON reviews (appointment_id);

-- Denormalised aggregate surfaced on doctor browse/detail; recomputed on each review write.
ALTER TABLE doctors ADD COLUMN rating_count   INT              NOT NULL DEFAULT 0;
ALTER TABLE doctors ADD COLUMN rating_average DOUBLE PRECISION NOT NULL DEFAULT 0;
