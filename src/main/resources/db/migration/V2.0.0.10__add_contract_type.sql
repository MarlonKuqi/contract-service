ALTER TABLE contracts.contract
    ADD COLUMN type VARCHAR(16) NOT NULL,
    ADD CONSTRAINT chk_contract_type CHECK (type IN ('LAMAL', 'LAA', 'LPP'));
