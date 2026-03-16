-- Special indexes that Exposed cannot create natively.
-- Partial unique indexes (WHERE clause) and HNSW vector index (pgvector).

-- Prevent duplicate active bank-matched payments
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_tenant_bank_txn_active
    ON payments (tenant_id, bank_transaction_id)
    WHERE bank_transaction_id IS NOT NULL AND reversed_at IS NULL;

-- Prevent duplicate active manual payments per invoice
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_invoice_manual
    ON payments (tenant_id, invoice_id)
    WHERE bank_transaction_id IS NULL AND reversed_at IS NULL;

-- Prevent duplicate active match links per bank transaction
CREATE UNIQUE INDEX IF NOT EXISTS uq_transaction_match_links_active_tx
    ON transaction_match_links (tenant_id, imported_bank_transaction_id)
    WHERE reversed_at IS NULL;

-- HNSW index for purpose similarity search (pgvector cosine distance)
CREATE INDEX IF NOT EXISTS document_purpose_examples_embedding_idx
    ON document_purpose_examples USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
