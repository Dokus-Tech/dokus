-- Prevent duplicate financial entities being created for the same document.
-- This is required for idempotent confirmation (worker retries + user actions).

CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_tenant_document_id
    ON invoices (tenant_id, document_id)
    WHERE document_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_expenses_tenant_document_id
    ON expenses (tenant_id, document_id)
    WHERE document_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_credit_notes_tenant_document_id
    ON credit_notes (tenant_id, document_id)
    WHERE document_id IS NOT NULL;

