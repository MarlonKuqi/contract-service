-- ====== CLIENT (racine d'identité commune) ======
CREATE TABLE contracts.client (
    id               UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    type             VARCHAR(16)    NOT NULL CHECK (type IN ('PERSON','COMPANY')),
    name             VARCHAR(200)   NOT NULL,
    email            VARCHAR(254)   NOT NULL UNIQUE,
    phone            VARCHAR(32)    NOT NULL
);

-- ====== PERSON ======
CREATE TABLE contracts.person (
    id         UUID PRIMARY KEY REFERENCES contracts.client(id) ON DELETE CASCADE,
    birth_date DATE NOT NULL
);

-- ====== COMPANY ======
CREATE TABLE contracts.company (
    id                  UUID PRIMARY KEY REFERENCES contracts.client(id) ON DELETE CASCADE,
    company_identifier  VARCHAR(64) NOT NULL UNIQUE
);

-- ====== CONTRACT ======
CREATE TABLE contracts.contract (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id      UUID REFERENCES contracts.client(id) ON DELETE SET NULL,
    start_date     TIMESTAMPTZ NOT NULL,
    end_date       TIMESTAMPTZ NULL,
    cost_amount    NUMERIC(12,2) NOT NULL CHECK (cost_amount >= 0),
    last_modified  TIMESTAMPTZ   NOT NULL
);

