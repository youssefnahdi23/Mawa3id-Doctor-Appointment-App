-- Iteration 2 (Phase A): per-doctor slot duration + weekly availability windows

ALTER TABLE doctors
    ADD COLUMN slot_duration_minutes INT NOT NULL DEFAULT 30;

CREATE TABLE doctor_availability (
    id          BIGSERIAL PRIMARY KEY,
    doctor_id   BIGINT      NOT NULL REFERENCES doctors (user_id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL
);

CREATE INDEX idx_doctor_availability_doctor ON doctor_availability (doctor_id);
