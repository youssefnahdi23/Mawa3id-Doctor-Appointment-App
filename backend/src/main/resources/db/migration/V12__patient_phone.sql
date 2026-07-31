-- Patients gain an optional contact phone (doctors already have one).
ALTER TABLE patients ADD COLUMN phone VARCHAR(40);
