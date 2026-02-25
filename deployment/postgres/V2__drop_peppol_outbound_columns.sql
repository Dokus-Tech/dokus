-- Migration: Drop Peppol outbound queue columns
-- Context: The outbound queue workers (PeppolOutboundWorker, PeppolOutboundReconciliationWorker)
-- have been removed. Peppol sends are now synchronous. These columns are no longer referenced
-- by application code.
--
-- Run AFTER deploying the new application version (columns are already unused by new code).
-- Safe for zero-downtime: old code never writes to these columns during the deploy window.

-- 1. Drop the stale unique index on idempotency_key
DROP INDEX IF EXISTS ux_peppol_transmissions_tenant_idempotency;

-- 2. Drop outbound-queue columns from peppol_transmissions
ALTER TABLE peppol_transmissions
    DROP COLUMN IF EXISTS idempotency_key,
    DROP COLUMN IF EXISTS provider_error_code,
    DROP COLUMN IF EXISTS provider_error_message,
    DROP COLUMN IF EXISTS attempt_count,
    DROP COLUMN IF EXISTS next_retry_at,
    DROP COLUMN IF EXISTS last_attempt_at;

-- 3. Drop webhook poll timestamp from peppol_settings (no longer tracked per-row)
ALTER TABLE peppol_settings
    DROP COLUMN IF EXISTS last_webhook_poll_triggered_at;

-- 4. Make webhook_token nullable (already nullable in new schema definition)
-- No action needed if column is already nullable. This is a safety no-op.
ALTER TABLE peppol_settings
    ALTER COLUMN webhook_token DROP NOT NULL;
