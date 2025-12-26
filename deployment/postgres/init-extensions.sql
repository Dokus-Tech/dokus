-- Enable pgvector extension for vector embeddings (RAG/AI features)
-- This script runs on database initialization

CREATE EXTENSION IF NOT EXISTS vector;
