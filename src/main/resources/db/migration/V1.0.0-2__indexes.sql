-- Index composite standard pour contrats actifs (couvre end_date non nulle)
CREATE INDEX ix_contract_client_end
  ON contract (client_id, end_date);

-- 2) (Optionnel, + perf) Index partiel pour actifs à end_date NULL
CREATE INDEX ix_contract_client_end_null
  ON contract (client_id)
  WHERE end_date IS NULL;

-- 3) Filtrage par date de mise à jour
CREATE INDEX ix_contract_client_lastmod
  ON contract (client_id, last_modified);

-- 4) (Optionnel) Accès direct par client (si tu fais souvent WHERE client_id = ?)
CREATE INDEX ix_contract_client
  ON contract (client_id);

-- === CLIENTS ===

-- email est UNIQUE -> index unique implicite déjà créé via "email ... UNIQUE"
-- Si tu filtres souvent par type (ex: /customers?type=COMPANY)
CREATE INDEX ix_client_type
  ON client_common (type);