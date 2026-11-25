ALTER TABLE contracts.contract
    ADD COLUMN franchise INTEGER NULL,
    ADD CONSTRAINT chk_contract_franchise_only_for_lamal CHECK ((type = 'LAMAL') = (franchise IS NOT NULL));
