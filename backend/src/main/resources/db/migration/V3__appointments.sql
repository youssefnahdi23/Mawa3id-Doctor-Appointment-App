-- Iteration 2 (Phase B): appointments (RDV)

CREATE TABLE appointments (
    id         BIGSERIAL PRIMARY KEY,
    doctor_id  BIGINT       NOT NULL REFERENCES doctors (user_id)  ON DELETE CASCADE,
    patient_id BIGINT       NOT NULL REFERENCES patients (user_id) ON DELETE CASCADE,
    start_time TIMESTAMP    NOT NULL,
    end_time   TIMESTAMP    NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    reason     VARCHAR(500),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_appointments_doctor  ON appointments (doctor_id);
CREATE INDEX idx_appointments_patient ON appointments (patient_id);
CREATE INDEX idx_appointments_start   ON appointments (start_time);

-- Concurrency backstop: at most one active (PENDING/ACCEPTED) booking per doctor+slot.
-- The transactional service check is authoritative; this guards against races.
CREATE UNIQUE INDEX uq_appointments_active_slot
    ON appointments (doctor_id, start_time)
    WHERE status IN ('PENDING', 'ACCEPTED');
