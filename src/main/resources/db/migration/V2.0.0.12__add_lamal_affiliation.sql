CREATE TABLE contracts.lamal (
    person_id UUID PRIMARY KEY REFERENCES contracts.person (id) ON DELETE CASCADE,
    version   BIGINT NOT NULL DEFAULT 0
);
