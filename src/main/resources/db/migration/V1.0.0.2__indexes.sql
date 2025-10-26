-- ========================================
-- Indexes pour Performance
-- ========================================

-- === CONTRACTS ===

-- 1) Index composite pour contrats actifs (requêtes fréquentes par client_id + end_date)
CREATE INDEX ix_contract_client_end
  ON contracts.contract (client_id, end_date);

-- 2) Index partiel pour contrats actifs sans end_date (optimisation)
CREATE INDEX ix_contract_client_end_null
  ON contracts.contract (client_id)
  WHERE end_date IS NULL;

-- 3) Index pour filtrage par date de mise à jour
CREATE INDEX ix_contract_client_lastmod
  ON contracts.contract (client_id, last_modified);

-- 4) Index sur client_id seul (pour les requêtes générales)
CREATE INDEX ix_contract_client
  ON contracts.contract (client_id);

-- === CLIENTS ===

-- Index sur le type de client (pour filtres par type si besoin futur)
CREATE INDEX ix_client_type
  ON contracts.client (type);

-- Note: email est déjà indexé via UNIQUE constraint
-- Note: company_identifier est déjà indexé via UNIQUE constraint
