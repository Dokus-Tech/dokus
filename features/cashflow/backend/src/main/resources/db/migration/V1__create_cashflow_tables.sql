-- =============================================================================
-- CASHFLOW MODULE - INITIAL SCHEMA
-- =============================================================================
-- This migration creates the core tables for invoicing and expense tracking
-- Critical: All tables include tenant_id for multi-tenancy security
-- =============================================================================

-- -----------------------------------------------------------------------------
-- INVOICES TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    client_id UUID NOT NULL,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,

    -- Dates
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,

    -- Amounts (NUMERIC for exact decimal arithmetic - NEVER Float!)
    subtotal_amount NUMERIC(12, 2) NOT NULL,
    vat_amount NUMERIC(12, 2) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,

    -- Status and currency
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(10) NOT NULL DEFAULT 'EUR',

    -- Optional text fields
    notes TEXT,
    terms_and_conditions TEXT,

    -- Peppol e-invoicing (Belgium 2026 mandate)
    peppol_id VARCHAR(255),
    peppol_sent_at TIMESTAMP,
    peppol_status VARCHAR(50),

    -- Payment
    payment_link VARCHAR(500),
    payment_link_expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    payment_method VARCHAR(50),

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance and security
CREATE INDEX idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX idx_invoices_client ON invoices(client_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_issue_date ON invoices(issue_date);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
CREATE INDEX idx_invoices_tenant_status ON invoices(tenant_id, status);
CREATE INDEX idx_invoices_tenant_client ON invoices(tenant_id, client_id);

-- -----------------------------------------------------------------------------
-- INVOICE ITEMS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,

    -- Item details
    description TEXT NOT NULL,
    quantity NUMERIC(10, 2) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    vat_rate NUMERIC(5, 4) NOT NULL, -- e.g., 0.2100 for 21%

    -- Calculated amounts (stored for audit trail)
    line_total NUMERIC(12, 2) NOT NULL,
    vat_amount NUMERIC(12, 2) NOT NULL,

    -- Ordering
    sort_order INTEGER NOT NULL DEFAULT 0
);

-- Indexes
CREATE INDEX idx_invoice_items_invoice ON invoice_items(invoice_id);

-- -----------------------------------------------------------------------------
-- EXPENSES TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expenses (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,

    -- Expense details
    date DATE NOT NULL,
    merchant VARCHAR(255) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    vat_amount NUMERIC(12, 2),
    vat_rate NUMERIC(5, 4), -- e.g., 0.2100 for 21%

    -- Categorization
    category VARCHAR(50) NOT NULL,
    description TEXT,

    -- Receipt/document
    receipt_url VARCHAR(500),
    receipt_filename VARCHAR(255),

    -- Tax deduction
    is_deductible BOOLEAN NOT NULL DEFAULT TRUE,
    deductible_percentage NUMERIC(5, 2) NOT NULL DEFAULT 100.00,

    -- Payment
    payment_method VARCHAR(50),

    -- Recurring
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,

    -- Notes
    notes TEXT,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance and security
CREATE INDEX idx_expenses_tenant ON expenses(tenant_id);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_date ON expenses(date);
CREATE INDEX idx_expenses_merchant ON expenses(merchant);
CREATE INDEX idx_expenses_tenant_category ON expenses(tenant_id, category);
CREATE INDEX idx_expenses_tenant_date ON expenses(tenant_id, date);

-- =============================================================================
-- COMMENTS
-- =============================================================================
COMMENT ON TABLE invoices IS 'Customer invoices - CRITICAL: All queries MUST filter by tenant_id';
COMMENT ON COLUMN invoices.tenant_id IS 'CRITICAL: Multi-tenancy security - always filter by this';
COMMENT ON COLUMN invoices.subtotal_amount IS 'NUMERIC type for exact decimal arithmetic - NEVER use Float!';

COMMENT ON TABLE invoice_items IS 'Line items for invoices - one invoice can have multiple items';
COMMENT ON COLUMN invoice_items.line_total IS 'Stored for audit trail - recalculate for display';

COMMENT ON TABLE expenses IS 'Business expenses - CRITICAL: All queries MUST filter by tenant_id';
COMMENT ON COLUMN expenses.tenant_id IS 'CRITICAL: Multi-tenancy security - always filter by this';
COMMENT ON COLUMN expenses.amount IS 'NUMERIC type for exact decimal arithmetic - NEVER use Float!';
