-- Per-window "RDVs Only" (by-appointment / no walk-in) policy flag.
-- Informational only: bookable slots are still generated for these windows.
-- Existing rows default to FALSE (walk-in welcome), preserving current behavior.
ALTER TABLE doctor_availability
    ADD COLUMN rdv_only BOOLEAN NOT NULL DEFAULT FALSE;
