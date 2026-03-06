CREATE TABLE IF NOT EXISTS imported_bank_transactions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    row_hash VARCHAR(64) NOT NULL,
    transaction_date DATE NOT NULL,
    signed_amount NUMERIC(12, 2) NOT NULL,
    counterparty_name VARCHAR(255),
    counterparty_iban VARCHAR(34),
    structured_communication_raw VARCHAR(64),
    normalized_structured_communication VARCHAR(32),
    description_raw TEXT,
    row_confidence NUMERIC(5, 4),
    large_amount_flag BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'UNMATCHED',
    linked_cashflow_entry_id UUID REFERENCES cashflow_entries(id) ON DELETE SET NULL,
    suggested_cashflow_entry_id UUID REFERENCES cashflow_entries(id) ON DELETE SET NULL,
    suggested_score NUMERIC(5, 4),
    suggested_tier VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT imported_bank_transactions_unique_row UNIQUE (tenant_id, document_id, row_hash)
);

CREATE TABLE IF NOT EXISTS cashflow_payment_candidates (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    cashflow_entry_id UUID NOT NULL REFERENCES cashflow_entries(id) ON DELETE CASCADE,
    imported_bank_transaction_id UUID NOT NULL REFERENCES imported_bank_transactions(id) ON DELETE CASCADE,
    score NUMERIC(5, 4) NOT NULL,
    tier VARCHAR(50) NOT NULL,
    signal_snapshot_json TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT cashflow_payment_candidates_unique_entry UNIQUE (tenant_id, cashflow_entry_id)
);

CREATE INDEX IF NOT EXISTS idx_imported_bank_transactions_picker
    ON imported_bank_transactions (tenant_id, status, transaction_date);

CREATE INDEX IF NOT EXISTS idx_imported_bank_transactions_suggested
    ON imported_bank_transactions (tenant_id, suggested_cashflow_entry_id);

CREATE INDEX IF NOT EXISTS idx_imported_bank_transactions_linked
    ON imported_bank_transactions (tenant_id, linked_cashflow_entry_id);

CREATE INDEX IF NOT EXISTS idx_imported_bank_transactions_structured_comm
    ON imported_bank_transactions (tenant_id, normalized_structured_communication);

CREATE INDEX IF NOT EXISTS idx_cashflow_payment_candidates_tx
    ON cashflow_payment_candidates (tenant_id, imported_bank_transaction_id);

CREATE INDEX IF NOT EXISTS idx_cashflow_payment_candidates_tier
    ON cashflow_payment_candidates (tenant_id, tier);
