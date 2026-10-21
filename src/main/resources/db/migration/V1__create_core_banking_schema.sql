CREATE TABLE IF NOT EXISTS accounts (
    id UUID NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(255) NOT NULL,
    balance NUMERIC(19,4) NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uk_accounts_account_number UNIQUE (account_number)
);

CREATE TABLE IF NOT EXISTS transfers (
    id UUID NOT NULL,
    reference VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    source_account_number VARCHAR(255) NOT NULL,
    destination_account_number VARCHAR(255) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payload_hash VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT uk_transfer_reference UNIQUE (reference),
    CONSTRAINT uk_transfer_idempotency_key UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS transfer_postings (
    id UUID NOT NULL,
    transfer_reference VARCHAR(255) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_transfer_postings PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS transfer_audit_entries (
    id UUID NOT NULL,
    transfer_reference VARCHAR(255) NOT NULL,
    action_name VARCHAR(255) NOT NULL,
    outcome VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_transfer_audit_entries PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS loans (
    id UUID NOT NULL,
    loan_reference VARCHAR(255) NOT NULL,
    borrower_account_number VARCHAR(255) NOT NULL,
    principal NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    annual_interest_rate NUMERIC(19,6) NOT NULL,
    term_months INTEGER NOT NULL,
    monthly_payment NUMERIC(19,4) NOT NULL,
    outstanding_balance NUMERIC(19,4) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_loans PRIMARY KEY (id),
    CONSTRAINT uk_loans_loan_reference UNIQUE (loan_reference)
);

CREATE TABLE IF NOT EXISTS loan_repayments (
    id UUID NOT NULL,
    loan_reference VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    principal_component NUMERIC(19,4) NOT NULL,
    interest_component NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_loan_repayments PRIMARY KEY (id),
    CONSTRAINT uk_loan_repayment_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_transfer_postings_transfer_reference ON transfer_postings (transfer_reference);
CREATE INDEX IF NOT EXISTS idx_transfer_audit_entries_transfer_reference ON transfer_audit_entries (transfer_reference);
CREATE INDEX IF NOT EXISTS idx_loan_repayments_loan_reference ON loan_repayments (loan_reference);
