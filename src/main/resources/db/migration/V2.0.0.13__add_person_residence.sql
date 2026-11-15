ALTER TABLE contracts.person
    ADD COLUMN residence VARCHAR(2) NOT NULL DEFAULT 'CH',
    ADD CONSTRAINT chk_person_residence CHECK (residence IN ('CH', 'FR', 'DE', 'IT', 'AT'));

ALTER TABLE contracts.person
    ALTER COLUMN residence DROP DEFAULT;
