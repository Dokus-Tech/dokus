-- Move tenant company address + VAT from tenant_settings (legacy) to tenants.
-- Safe to run on fresh DBs where tables may not exist yet (no-op until tables exist).

-- Ensure target columns exist on tenants (SchemaUtils also creates them, but this supports pure-Flyway installs).
ALTER TABLE IF EXISTS tenants
    ADD COLUMN IF NOT EXISTS company_address TEXT NOT NULL DEFAULT '';

ALTER TABLE IF EXISTS tenants
    ADD COLUMN IF NOT EXISTS vat_number VARCHAR(50);

-- Backfill tenants.company_address from tenant_settings.company_address (legacy) when tenant value is blank.
UPDATE tenants t
SET company_address = ts.company_address
FROM tenant_settings ts
WHERE ts.tenant_id = t.id
  AND ts.company_address IS NOT NULL
  AND ts.company_address <> ''
  AND (t.company_address IS NULL OR t.company_address = '');

-- Backfill tenants.vat_number from tenant_settings.company_vat_number (legacy) when tenant value is missing.
UPDATE tenants t
SET vat_number = ts.company_vat_number
FROM tenant_settings ts
WHERE ts.tenant_id = t.id
  AND ts.company_vat_number IS NOT NULL
  AND ts.company_vat_number <> ''
  AND t.vat_number IS NULL;

-- Drop legacy columns from tenant_settings.
ALTER TABLE IF EXISTS tenant_settings DROP COLUMN IF EXISTS company_address;
ALTER TABLE IF EXISTS tenant_settings DROP COLUMN IF EXISTS company_vat_number;

