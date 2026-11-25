CREATE TABLE contracts.contract_import_job (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    version         BIGINT       NOT NULL DEFAULT 0,
    file_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    total_lines     INTEGER      NOT NULL,
    processed_lines INTEGER      NOT NULL DEFAULT 0,
    imported_count  INTEGER      NOT NULL DEFAULT 0,
    rejected_count  INTEGER      NOT NULL DEFAULT 0,
    lease_until     TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ  NULL,
    CONSTRAINT chk_contract_import_job_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_contract_import_job_to_run ON contracts.contract_import_job (created_at)
    WHERE status IN ('PENDING', 'RUNNING');

CREATE TABLE contracts.contract_import_job_file (
    job_id  UUID PRIMARY KEY,
    content BYTEA NOT NULL,
    CONSTRAINT fk_contract_import_job_file_job FOREIGN KEY (job_id)
        REFERENCES contracts.contract_import_job (id) ON DELETE CASCADE
);

CREATE TABLE contracts.contract_import_job_line (
    job_id            UUID        NOT NULL,
    line_number       INTEGER     NOT NULL,
    client_id         UUID        NULL,
    contract_id       UUID        NULL,
    rejection_reason  VARCHAR(30) NULL,
    rejection_message TEXT        NULL,
    PRIMARY KEY (job_id, line_number),
    CONSTRAINT fk_contract_import_job_line_job FOREIGN KEY (job_id)
        REFERENCES contracts.contract_import_job (id) ON DELETE CASCADE,
    CONSTRAINT fk_contract_import_job_line_contract FOREIGN KEY (contract_id)
        REFERENCES contracts.contract (id) ON DELETE RESTRICT,
    CONSTRAINT chk_contract_import_job_line_outcome CHECK ((contract_id IS NULL) <> (rejection_reason IS NULL))
);
