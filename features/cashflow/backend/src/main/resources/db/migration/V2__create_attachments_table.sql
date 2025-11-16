-- =============================================================================
-- ATTACHMENTS TABLE
-- =============================================================================
-- Stores document/file metadata for invoices and expenses
-- Files are stored in S3 or local filesystem, this table stores metadata
-- =============================================================================

CREATE TABLE IF NOT EXISTS attachments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,

    -- Generic entity reference (can attach to invoices, expenses, etc.)
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL, -- UUID as string for flexibility

    -- File metadata
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,

    -- Storage location
    s3_key VARCHAR(500) NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,

    -- Timestamp
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance and security
CREATE INDEX idx_attachments_tenant ON attachments(tenant_id);
CREATE INDEX idx_attachments_entity_type ON attachments(entity_type);
CREATE INDEX idx_attachments_entity_id ON attachments(entity_id);
CREATE INDEX idx_attachments_tenant_entity ON attachments(tenant_id, entity_type, entity_id);

-- =============================================================================
-- COMMENTS
-- =============================================================================
COMMENT ON TABLE attachments IS 'Document metadata - files stored in S3/local filesystem';
COMMENT ON COLUMN attachments.tenant_id IS 'CRITICAL: Multi-tenancy security - always filter by this';
COMMENT ON COLUMN attachments.entity_type IS 'Type of entity (INVOICE, EXPENSE, etc.)';
COMMENT ON COLUMN attachments.entity_id IS 'ID of the entity (invoice_id, expense_id, etc.)';
COMMENT ON COLUMN attachments.s3_key IS 'S3 object key or local filesystem path';
