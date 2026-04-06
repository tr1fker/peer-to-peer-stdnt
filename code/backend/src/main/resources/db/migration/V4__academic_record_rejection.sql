ALTER TABLE academic_records
    ADD COLUMN rejected BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN rejected_at TIMESTAMPTZ,
    ADD COLUMN rejected_by BIGINT REFERENCES users (id),
    ADD COLUMN rejection_reason TEXT;
