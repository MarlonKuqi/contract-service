ALTER TABLE contracts.contract_import_job
    ADD COLUMN submitted_by    VARCHAR(255) NULL,
    ADD COLUMN idempotency_key VARCHAR(255) NULL,
    ADD COLUMN content_sha256  VARCHAR(64)  NULL,
    ADD COLUMN cancelled_at    TIMESTAMPTZ  NULL,
    ADD COLUMN cancelled_by    VARCHAR(255) NULL;

UPDATE contracts.contract_import_job job
SET content_sha256 = encode(sha256(file.content), 'hex')
FROM contracts.contract_import_job_file file
WHERE file.job_id = job.id;

ALTER TABLE contracts.contract_import_job
    ALTER COLUMN content_sha256 SET NOT NULL;

ALTER TABLE contracts.contract_import_job
    DROP CONSTRAINT chk_contract_import_job_status;

ALTER TABLE contracts.contract_import_job
    ADD CONSTRAINT chk_contract_import_job_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED'));

CREATE UNIQUE INDEX ux_contract_import_job_idempotency_key
    ON contracts.contract_import_job (submitted_by, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
