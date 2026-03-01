-- Extend invoice schema for unified create flow metadata and contact defaults history.
-- Keep defaults aligned with domain model fallback behavior.

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS payment_terms_days INTEGER NOT NULL DEFAULT 30;

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS due_date_mode VARCHAR(50) NOT NULL DEFAULT 'TERMS';

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS structured_communication VARCHAR(32);

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS sender_iban VARCHAR(34);

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS sender_bic VARCHAR(11);

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS delivery_method VARCHAR(50) NOT NULL DEFAULT 'PDF_EXPORT';

CREATE INDEX IF NOT EXISTS ix_invoices_tenant_contact_issue_date
    ON invoices (tenant_id, contact_id, issue_date);
