ALTER TABLE contracts.client ADD COLUMN deleted_at TIMESTAMPTZ NULL;

ALTER TABLE contracts.contract DROP CONSTRAINT contract_client_id_fkey;
ALTER TABLE contracts.contract
    ADD CONSTRAINT fk_contract_client FOREIGN KEY (client_id) REFERENCES contracts.client (id) ON DELETE RESTRICT;
