CREATE INDEX ix_contract_client_end
  ON contracts.contract (client_id, end_date);

CREATE INDEX ix_contract_client_end_null
  ON contracts.contract (client_id)
  WHERE end_date IS NULL;

CREATE INDEX ix_contract_client_lastmod
  ON contracts.contract (client_id, last_modified);

CREATE INDEX ix_contract_client
  ON contracts.contract (client_id);

CREATE INDEX ix_client_type
  ON contracts.client (type);

