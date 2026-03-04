-- Business enrichment descriptions table
CREATE TABLE IF NOT EXISTS business_descriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    website_url VARCHAR(500),
    summary TEXT,
    activities TEXT,
    enrichment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ix_business_desc_entity ON business_descriptions(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS ix_business_desc_tenant ON business_descriptions(tenant_id, entity_type);

-- Add company website to tenant settings
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS company_website VARCHAR(500);

-- Add avatar storage key to contacts
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS avatar_storage_key VARCHAR(500);
