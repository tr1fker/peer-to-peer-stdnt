CREATE TABLE loan_request_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    loan_request_id BIGINT NOT NULL REFERENCES loan_requests (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, loan_request_id)
);

CREATE INDEX idx_loan_request_favorites_user ON loan_request_favorites (user_id);
