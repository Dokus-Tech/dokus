-- Recommand-first webhook hardening:
-- - keep per-tenant token auth
-- - add DB-backed webhook poll debounce guard
-- - enforce unique enabled company routing

ALTER TABLE peppol_settings
    ADD COLUMN IF NOT EXISTS last_webhook_poll_triggered_at TIMESTAMP NULL;

UPDATE peppol_settings
SET webhook_token = substring(md5(random()::text || clock_timestamp()::text || id::text), 1, 64)
WHERE webhook_token IS NULL OR webhook_token = '';

ALTER TABLE peppol_settings
    ALTER COLUMN webhook_token SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_peppol_settings_webhook_token
    ON peppol_settings (webhook_token);

DO
$$
DECLARE
    duplicate_company_ids TEXT;
BEGIN
    SELECT string_agg(company_id, ', ')
    INTO duplicate_company_ids
    FROM (
        SELECT company_id
        FROM peppol_settings
        WHERE is_enabled = true
        GROUP BY company_id
        HAVING COUNT(*) > 1
    ) duplicates;

    IF duplicate_company_ids IS NOT NULL THEN
        RAISE EXCEPTION 'Cannot enforce unique enabled PEPPOL company mapping. Duplicate company_id values: %',
            duplicate_company_ids;
    END IF;
END;
$$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_peppol_settings_company_id_enabled
    ON peppol_settings (company_id)
    WHERE is_enabled = true;

CREATE INDEX IF NOT EXISTS ix_peppol_settings_enabled_company_id
    ON peppol_settings (is_enabled, company_id);
