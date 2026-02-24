-- P0 outbound reliability hardening for PEPPOL transmissions.

ALTER TABLE peppol_transmissions
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

UPDATE peppol_transmissions
SET idempotency_key = COALESCE(idempotency_key, 'legacy-' || id::text);

ALTER TABLE peppol_transmissions
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE peppol_transmissions
    ADD COLUMN IF NOT EXISTS attempt_count INT NOT NULL DEFAULT 0;

ALTER TABLE peppol_transmissions
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP NULL;

ALTER TABLE peppol_transmissions
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP NULL;

ALTER TABLE peppol_transmissions
    ADD COLUMN IF NOT EXISTS provider_error_code VARCHAR(100) NULL;

ALTER TABLE peppol_transmissions
    ADD COLUMN IF NOT EXISTS provider_error_message TEXT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_peppol_transmissions_tenant_idempotency
    ON peppol_transmissions (tenant_id, idempotency_key);
