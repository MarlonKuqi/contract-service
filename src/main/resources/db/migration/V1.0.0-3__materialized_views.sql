CREATE MATERIALIZED VIEW IF NOT EXISTS mv_active_contracts AS
SELECT
    c.id               AS contract_id,
    c.client_id        AS client_id,
    c.start_date       AS start_date,
    c.end_date         AS end_date,
    c.cost_amount      AS cost_amount,
    c.last_modified    AS last_modified
FROM contract c
WHERE c.end_date IS NULL OR c.end_date > CURRENT_DATE;

CREATE INDEX IF NOT EXISTS ix_mv_active_contracts_client_lastmod
  ON mv_active_contracts (client_id, last_modified);

CREATE INDEX IF NOT EXISTS ix_mv_active_contracts_client
  ON mv_active_contracts (client_id);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_active_contracts_sum AS
SELECT
    c.client_id                    AS client_id,
    COALESCE(SUM(c.cost_amount), 0)::numeric(14,2) AS total_active_cost
FROM
    contract c
WHERE
    c.end_date IS NULL OR c.end_date > CURRENT_DATE
GROUP BY
    c.client_id;

-- UNIQUE index requis pour REFRESH CONCURRENTLY + lookup direct
CREATE UNIQUE INDEX IF NOT EXISTS ux_mv_active_contracts_sum_client
  ON mv_active_contracts_sum (client_id);