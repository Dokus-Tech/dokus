-- HNSW index for purpose similarity search on document_purpose_examples.embedding
-- Uses cosine distance operator for semantic similarity queries.
CREATE INDEX CONCURRENTLY IF NOT EXISTS document_purpose_examples_embedding_idx
    ON document_purpose_examples USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
