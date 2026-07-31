-- Doctor-side v1 findability/trust fields + localized specialties.

-- CNAM (Tunisian national health insurance): whether the doctor is conventionné.
ALTER TABLE doctors ADD COLUMN cnam_conventionne BOOLEAN NOT NULL DEFAULT FALSE;

-- Governorate stored as the enum name (e.g. SFAX); cabinet_address stays the free-text street line.
ALTER TABLE doctors ADD COLUMN governorate VARCHAR(40);

-- Display-only consultation fee in whole Tunisian dinars (never collected in-app).
ALTER TABLE doctors ADD COLUMN consultation_fee INTEGER;

-- Comma-joined language codes drawn from {AR, FR, EN}, e.g. 'AR,FR'.
ALTER TABLE doctors ADD COLUMN languages VARCHAR(60);

CREATE INDEX idx_doctors_governorate ON doctors (governorate);
CREATE INDEX idx_doctors_cnam ON doctors (cnam_conventionne);

-- Localized specialty labels for the trilingual (AR/FR/EN) client.
ALTER TABLE specialties ADD COLUMN name_ar VARCHAR(120);
ALTER TABLE specialties ADD COLUMN name_fr VARCHAR(120);

UPDATE specialties SET name_ar = 'الطب العام',        name_fr = 'Médecine générale' WHERE name = 'General Medicine';
UPDATE specialties SET name_ar = 'أمراض القلب',       name_fr = 'Cardiologie'        WHERE name = 'Cardiology';
UPDATE specialties SET name_ar = 'الأمراض الجلدية',   name_fr = 'Dermatologie'       WHERE name = 'Dermatology';
UPDATE specialties SET name_ar = 'طب الأطفال',        name_fr = 'Pédiatrie'          WHERE name = 'Pediatrics';
UPDATE specialties SET name_ar = 'أمراض النساء',      name_fr = 'Gynécologie'        WHERE name = 'Gynecology';
UPDATE specialties SET name_ar = 'جراحة العظام',      name_fr = 'Orthopédie'         WHERE name = 'Orthopedics';
UPDATE specialties SET name_ar = 'طب الأعصاب',        name_fr = 'Neurologie'         WHERE name = 'Neurology';
UPDATE specialties SET name_ar = 'طب العيون',         name_fr = 'Ophtalmologie'      WHERE name = 'Ophthalmology';
UPDATE specialties SET name_ar = 'أنف وأذن وحنجرة',   name_fr = 'ORL'                WHERE name = 'ENT';
UPDATE specialties SET name_ar = 'طب الأسنان',        name_fr = 'Dentisterie'        WHERE name = 'Dentistry';
UPDATE specialties SET name_ar = 'الطب النفسي',       name_fr = 'Psychiatrie'        WHERE name = 'Psychiatry';
UPDATE specialties SET name_ar = 'الغدد الصماء',      name_fr = 'Endocrinologie'     WHERE name = 'Endocrinology';
