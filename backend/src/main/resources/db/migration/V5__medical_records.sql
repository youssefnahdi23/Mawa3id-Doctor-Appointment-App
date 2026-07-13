-- Iteration 4: medical records / visit notes

CREATE TABLE medical_records (
    id             BIGSERIAL     PRIMARY KEY,
    appointment_id BIGINT        NOT NULL UNIQUE REFERENCES appointments (id) ON DELETE CASCADE,
    diagnosis      VARCHAR(1000) NOT NULL,
    notes          VARCHAR(4000),
    prescription   VARCHAR(2000),
    follow_up_date DATE,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medical_records_appointment ON medical_records (appointment_id);
