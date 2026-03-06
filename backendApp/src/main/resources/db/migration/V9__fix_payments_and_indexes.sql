-- V9: Fix payments unique constraints and add missing indexes

-- Drop the broken unique index on payments (NULL transactionId allows duplicates)
DROP INDEX IF EXISTS payments_invoice_id_transaction_id_unique;

-- Partial unique index: prevent duplicate active payments per invoice+bank_transaction
-- (already exists from V8, keeping as-is)

-- Partial unique index: prevent duplicate active manual payments per invoice
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_invoice_manual
    ON payments (tenant_id, invoice_id)
    WHERE bank_transaction_id IS NULL AND reversed_at IS NULL;

-- Add missing index on auto_payment_audit_events for cashflow entry queries
CREATE INDEX IF NOT EXISTS idx_auto_payment_audit_events_tenant_entry
    ON auto_payment_audit_events (tenant_id, cashflow_entry_id);

-- Add created_at column to cashflow_payment_candidates
ALTER TABLE cashflow_payment_candidates
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
