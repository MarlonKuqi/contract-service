ALTER TABLE contracts.lamal
    ADD COLUMN exemption_start_date DATE NULL,
    ADD COLUMN exemption_end_date   DATE NULL,
    ADD CONSTRAINT chk_lamal_exemption_period CHECK (
        (exemption_start_date IS NOT NULL OR exemption_end_date IS NULL)
        AND (exemption_end_date IS NULL OR exemption_end_date > exemption_start_date)
    );
