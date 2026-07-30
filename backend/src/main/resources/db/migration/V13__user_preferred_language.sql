-- Preferred UI/notification language per user (en/fr/ar); null falls back to the server default.
ALTER TABLE users ADD COLUMN preferred_language VARCHAR(5);
