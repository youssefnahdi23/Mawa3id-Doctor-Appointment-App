-- Mawa3id initial schema: users, patients, specialties, doctors

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE specialties (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE patients (
    user_id       BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    full_name     VARCHAR(200) NOT NULL,
    date_of_birth DATE         NOT NULL,
    patient_code  VARCHAR(20)  NOT NULL UNIQUE
);

CREATE TABLE doctors (
    user_id         BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    specialty_id    BIGINT       REFERENCES specialties (id),
    cabinet_address VARCHAR(300),
    working_hours   VARCHAR(500),
    phone           VARCHAR(40),
    bio             VARCHAR(1000),
    acceptance_mode VARCHAR(20)  NOT NULL DEFAULT 'MANUAL'
);

CREATE INDEX idx_doctors_specialty ON doctors (specialty_id);

-- Seed common specialties
INSERT INTO specialties (name, description) VALUES
    ('General Medicine', 'General practitioners and family medicine'),
    ('Cardiology', 'Heart and cardiovascular system'),
    ('Dermatology', 'Skin, hair and nails'),
    ('Pediatrics', 'Medical care of infants, children and adolescents'),
    ('Gynecology', 'Female reproductive health'),
    ('Orthopedics', 'Bones, joints and musculoskeletal system'),
    ('Neurology', 'Brain and nervous system'),
    ('Ophthalmology', 'Eyes and vision'),
    ('ENT', 'Ear, nose and throat'),
    ('Dentistry', 'Teeth and oral health'),
    ('Psychiatry', 'Mental health'),
    ('Endocrinology', 'Hormones and metabolism');
