ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS website_url VARCHAR(500);

CREATE TABLE IF NOT EXISTS business_profiles (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subject_type VARCHAR(50) NOT NULL,
    subject_id UUID NOT NULL,
    website_url VARCHAR(500),
    business_summary TEXT,
    business_activities_json TEXT,
    verification_state VARCHAR(50) NOT NULL DEFAULT 'UNSET',
    evidence_score INTEGER NOT NULL DEFAULT 0,
    evidence_checks_json TEXT,
    logo_storage_key VARCHAR(500),
    website_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    summary_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    activities_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    logo_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    last_run_at TIMESTAMP,
    last_error_code VARCHAR(80),
    last_error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_business_profiles_subject
    ON business_profiles (tenant_id, subject_type, subject_id);

CREATE INDEX IF NOT EXISTS ix_business_profiles_tenant_subject_type
    ON business_profiles (tenant_id, subject_type);

CREATE INDEX IF NOT EXISTS ix_business_profiles_tenant_subject_id
    ON business_profiles (tenant_id, subject_id);

CREATE TABLE IF NOT EXISTS business_profile_enrichment_jobs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subject_type VARCHAR(50) NOT NULL,
    subject_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    trigger_reason VARCHAR(64) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    next_attempt_at TIMESTAMP NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    processing_started_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_business_profile_enrichment_jobs_subject
    ON business_profile_enrichment_jobs (tenant_id, subject_type, subject_id);

CREATE INDEX IF NOT EXISTS ix_business_profile_enrichment_jobs_status_next_attempt
    ON business_profile_enrichment_jobs (status, next_attempt_at);
