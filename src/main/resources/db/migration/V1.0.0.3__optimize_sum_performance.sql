CREATE INDEX IF NOT EXISTS ix_contract_sum_performance
  ON contracts.contract (client_id, end_date, cost_amount)
  WHERE end_date IS NULL;

