ALTER TABLE imported_bank_transactions
    ADD COLUMN IF NOT EXISTS transaction_fingerprint VARCHAR(64);

UPDATE imported_bank_transactions
SET transaction_fingerprint = row_hash
WHERE transaction_fingerprint IS NULL;

ALTER TABLE imported_bank_transactions
    ALTER COLUMN transaction_fingerprint SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_imported_bank_transactions_fingerprint
    ON imported_bank_transactions (tenant_id, transaction_fingerprint);

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS bank_transaction_id UUID REFERENCES imported_bank_transactions(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(50) NOT NULL DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reversed_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS reversal_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_payments_bank_transaction_id
    ON payments (tenant_id, bank_transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_tenant_bank_txn_active
    ON payments (tenant_id, bank_transaction_id)
    WHERE bank_transaction_id IS NOT NULL AND reversed_at IS NULL;

CREATE TABLE IF NOT EXISTS invoice_bank_match_links (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    cashflow_entry_id UUID NOT NULL REFERENCES cashflow_entries(id) ON DELETE CASCADE,
    imported_bank_transaction_id UUID NOT NULL REFERENCES imported_bank_transactions(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    created_by VARCHAR(50) NOT NULL DEFAULT 'AUTO',
    confidence_score NUMERIC(5, 4),
    score_margin NUMERIC(5, 4),
    reasons_json TEXT,
    rules_json TEXT,
    matched_at TIMESTAMP NOT NULL,
    autopaid_at TIMESTAMP,
    reversed_at TIMESTAMP,
    reversed_by_user_id UUID,
    reversal_reason TEXT,
    invoice_status_before VARCHAR(50),
    invoice_paid_amount_before NUMERIC(12, 2),
    invoice_paid_at_before TIMESTAMP,
    cashflow_status_before VARCHAR(50),
    cashflow_remaining_before NUMERIC(12, 2),
    cashflow_paid_at_before TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_invoice_bank_match_links_pair UNIQUE (tenant_id, invoice_id, imported_bank_transaction_id)
);

CREATE INDEX IF NOT EXISTS idx_invoice_bank_match_links_entry
    ON invoice_bank_match_links (tenant_id, cashflow_entry_id);

CREATE INDEX IF NOT EXISTS idx_invoice_bank_match_links_status
    ON invoice_bank_match_links (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_invoice_bank_match_links_tx
    ON invoice_bank_match_links (tenant_id, imported_bank_transaction_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_bank_match_links_active_tx
    ON invoice_bank_match_links (tenant_id, imported_bank_transaction_id)
    WHERE reversed_at IS NULL;

CREATE TABLE IF NOT EXISTS auto_payment_audit_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id UUID REFERENCES invoices(id) ON DELETE SET NULL,
    cashflow_entry_id UUID REFERENCES cashflow_entries(id) ON DELETE SET NULL,
    imported_bank_transaction_id UUID REFERENCES imported_bank_transactions(id) ON DELETE SET NULL,
    trigger_source VARCHAR(50) NOT NULL,
    decision VARCHAR(50) NOT NULL,
    score NUMERIC(5, 4),
    margin NUMERIC(5, 4),
    reasons_json TEXT,
    rules_json TEXT,
    actor_user_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auto_payment_audit_events_created
    ON auto_payment_audit_events (tenant_id, created_at);

CREATE INDEX IF NOT EXISTS idx_auto_payment_audit_events_invoice
    ON auto_payment_audit_events (tenant_id, invoice_id);

CREATE INDEX IF NOT EXISTS idx_auto_payment_audit_events_tx
    ON auto_payment_audit_events (tenant_id, imported_bank_transaction_id);
