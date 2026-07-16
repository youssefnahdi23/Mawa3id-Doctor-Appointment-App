-- Auth system: unified identifiers (email/username/phone), social identities,
-- verification/reset challenges, refresh sessions and security audit events.
--
-- users.email becomes a nullable mirror of the primary EMAIL contact (kept so
-- existing consumers keep working); user_contacts is the source of truth for
-- login identifiers and recovery eligibility.

-- ---------------------------------------------------------------------------
-- users: username + status + auth timestamps; email/password become optional
-- (phone-only registration and social-only accounts respectively).
-- ---------------------------------------------------------------------------
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN username VARCHAR(30);
ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN password_changed_at TIMESTAMP;

-- Backfill usernames from the email local part (lowercased, restricted to the
-- allowed charset), suffixing duplicates with ".<id>" to keep them unique.
UPDATE users
SET username = sub.candidate
FROM (
    SELECT id,
           CASE WHEN length(base) >= 3 THEN left(base, 24) ELSE 'user' END
               || CASE WHEN rn = 1 THEN '' ELSE '.' || id END AS candidate
    FROM (
        SELECT id,
               regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._-]', '', 'g') AS base,
               ROW_NUMBER() OVER (
                   PARTITION BY regexp_replace(lower(split_part(email, '@', 1)), '[^a-z0-9._-]', '', 'g')
                   ORDER BY id) AS rn
        FROM users
    ) bases
) sub
WHERE users.id = sub.id;

-- Safety pass: anything still colliding (or too short after cleaning) falls
-- back to a name derived from the primary key. The UNIQUE constraint below is
-- the final arbiter: if a pathological collision survives both passes the
-- migration fails loudly instead of corrupting data.
UPDATE users u
SET username = 'user.' || u.id
WHERE length(u.username) < 3
   OR length(u.username) > 30
   OR EXISTS (SELECT 1 FROM users d WHERE d.username = u.username AND d.id < u.id);

ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);

-- Social sign-ups create the patient row before a date of birth is known.
ALTER TABLE patients ALTER COLUMN date_of_birth DROP NOT NULL;

-- ---------------------------------------------------------------------------
-- user_contacts: verified email/phone identifiers per user.
-- ---------------------------------------------------------------------------
CREATE TABLE user_contacts (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type             VARCHAR(10)  NOT NULL,           -- EMAIL | PHONE
    contact_value    VARCHAR(255) NOT NULL,           -- display form as entered
    normalized_value VARCHAR(255) NOT NULL,           -- lowercased email / E.164 phone
    is_primary       BOOLEAN      NOT NULL DEFAULT FALSE,
    verified_at      TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_contacts_value UNIQUE (type, normalized_value)
);

CREATE INDEX idx_user_contacts_user ON user_contacts (user_id);

-- Existing emails predate contact verification and have been used to log in;
-- marking them unverified would lock every existing user out of password
-- recovery. New registrations start unverified.
INSERT INTO user_contacts (user_id, type, contact_value, normalized_value, is_primary, verified_at, created_at)
SELECT id, 'EMAIL', email, lower(email), TRUE, created_at, created_at
FROM users
WHERE email IS NOT NULL;

-- ---------------------------------------------------------------------------
-- auth_identities: linked social logins (Google / Facebook).
-- ---------------------------------------------------------------------------
CREATE TABLE auth_identities (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider         VARCHAR(20)  NOT NULL,           -- GOOGLE | FACEBOOK
    provider_subject VARCHAR(191) NOT NULL,           -- verified sub / app-scoped id
    display_name     VARCHAR(200),
    linked_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at     TIMESTAMP,
    CONSTRAINT uq_auth_identities_subject UNIQUE (provider, provider_subject),
    CONSTRAINT uq_auth_identities_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_auth_identities_user ON auth_identities (user_id);

-- ---------------------------------------------------------------------------
-- auth_challenges: short-lived single-use secrets (email links, SMS OTPs,
-- reset authorizations). Only SHA-256 hashes are stored, never raw secrets.
-- ---------------------------------------------------------------------------
CREATE TABLE auth_challenges (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users (id) ON DELETE CASCADE,
    purpose       VARCHAR(30)  NOT NULL,  -- VERIFY_EMAIL | VERIFY_PHONE | RESET_EMAIL | RESET_SMS | RESET_AUTHORIZATION
    channel       VARCHAR(10)  NOT NULL,  -- EMAIL | PHONE | NONE
    destination   VARCHAR(255),           -- normalized destination the secret was sent to
    token_hash    VARCHAR(64)  NOT NULL,  -- SHA-256 hex
    expires_at    TIMESTAMP    NOT NULL,
    attempt_count INT          NOT NULL DEFAULT 0,
    max_attempts  INT          NOT NULL DEFAULT 5,
    consumed_at   TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_challenges_hash ON auth_challenges (purpose, token_hash);
CREATE INDEX idx_auth_challenges_dest ON auth_challenges (purpose, destination, created_at);

-- ---------------------------------------------------------------------------
-- auth_sessions: hashed refresh tokens with rotation lineage. family_id ties
-- rotated tokens together so replay of a rotated token revokes the family.
-- ---------------------------------------------------------------------------
CREATE TABLE auth_sessions (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(64) NOT NULL UNIQUE,
    family_id          VARCHAR(36) NOT NULL,
    device_name        VARCHAR(120),
    user_agent         VARCHAR(255),
    ip_address         VARCHAR(45),
    created_at         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at       TIMESTAMP,
    expires_at         TIMESTAMP   NOT NULL,
    revoked_at         TIMESTAMP,
    replaced_by_id     BIGINT REFERENCES auth_sessions (id)
);

CREATE INDEX idx_auth_sessions_user ON auth_sessions (user_id);
CREATE INDEX idx_auth_sessions_family ON auth_sessions (family_id);

-- ---------------------------------------------------------------------------
-- auth_audit_events: security event trail (no secrets, masked identifiers).
-- ---------------------------------------------------------------------------
CREATE TABLE auth_audit_events (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users (id) ON DELETE SET NULL,
    event_type VARCHAR(40)  NOT NULL,
    detail     VARCHAR(500),
    ip_address VARCHAR(45),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_audit_user ON auth_audit_events (user_id, created_at);
