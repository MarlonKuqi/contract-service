-- ====== CLIENT (racine d'identité commune) ======
CREATE TABLE client (
    id               UUID      PRIMARY KEY,
    type             VARCHAR(16)    NOT NULL CHECK (type IN ('PERSON','COMPANY')),
    name             VARCHAR(200)   NOT NULL,
    email            VARCHAR(254)   NOT NULL UNIQUE,
    phone            VARCHAR(32)    NOT NULL
);

-- ====== PERSON ======
CREATE TABLE person (
    id         UUID PRIMARY KEY REFERENCES client(id) ON DELETE CASCADE,
    birth_date DATE NOT NULL
);

-- ====== COMPANY ======
CREATE TABLE company (
    id                  UUID PRIMARY KEY REFERENCES client(id) ON DELETE CASCADE,
    company_identifier  VARCHAR(64) NOT NULL UNIQUE
);

-- ====== CONTRACT ======
CREATE TABLE contract (
    id             UUID PRIMARY KEY,
    client_id      UUID REFERENCES client(id) ON DELETE SET NULL,
    start_date     TIMESTAMPTZ NOT NULL,
    end_date       TIMESTAMPTZ NULL,
    cost_amount    NUMERIC(12,2) NOT NULL CHECK (cost_amount >= 0),
    last_modified  TIMESTAMPTZ   NOT NULL
);

