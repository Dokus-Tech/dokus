ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS session_id UUID;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_session_id
    ON refresh_tokens (session_id);

UPDATE refresh_tokens
SET session_id = access_token_jti::uuid
WHERE session_id IS NULL
  AND access_token_jti IS NOT NULL
  AND access_token_jti ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
