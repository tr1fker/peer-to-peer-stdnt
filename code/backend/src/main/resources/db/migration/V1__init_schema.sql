CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE oauth_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    UNIQUE (provider, provider_user_id)
);

CREATE TABLE student_profiles (
    user_id BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    university VARCHAR(255),
    student_group VARCHAR(128),
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'
);

CREATE TABLE academic_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    grade_average NUMERIC(4, 2),
    description TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    verified_by BIGINT REFERENCES users (id)
);

CREATE TABLE loan_requests (
    id BIGSERIAL PRIMARY KEY,
    borrower_id BIGINT NOT NULL REFERENCES users (id),
    amount NUMERIC(14, 2) NOT NULL,
    term_months INT NOT NULL,
    purpose TEXT,
    status VARCHAR(32) NOT NULL,
    interest_rate_percent NUMERIC(5, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loan_requests_borrower ON loan_requests (borrower_id);
CREATE INDEX idx_loan_requests_status ON loan_requests (status);

CREATE TABLE investments (
    id BIGSERIAL PRIMARY KEY,
    loan_request_id BIGINT NOT NULL REFERENCES loan_requests (id) ON DELETE CASCADE,
    lender_id BIGINT NOT NULL REFERENCES users (id),
    amount NUMERIC(14, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_investments_request ON investments (loan_request_id);
CREATE INDEX idx_investments_lender ON investments (lender_id);

CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    loan_request_id BIGINT NOT NULL UNIQUE REFERENCES loan_requests (id),
    principal NUMERIC(14, 2) NOT NULL,
    interest_rate_percent NUMERIC(5, 2) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE TABLE loan_guarantees (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans (id) ON DELETE CASCADE,
    guarantor_user_id BIGINT REFERENCES users (id),
    guarantee_type VARCHAR(32) NOT NULL,
    coverage_amount NUMERIC(14, 2) NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE INDEX idx_loan_guarantees_loan ON loan_guarantees (loan_id);

CREATE TABLE payment_installments (
    id BIGSERIAL PRIMARY KEY,
    loan_id BIGINT NOT NULL REFERENCES loans (id) ON DELETE CASCADE,
    installment_number INT NOT NULL,
    amount_due NUMERIC(14, 2) NOT NULL,
    due_date DATE NOT NULL,
    paid_at TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,
    UNIQUE (loan_id, installment_number)
);

CREATE INDEX idx_installments_loan ON payment_installments (loan_id);
