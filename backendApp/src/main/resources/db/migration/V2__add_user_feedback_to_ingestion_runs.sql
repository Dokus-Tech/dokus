-- Add user_feedback column to document_ingestion_runs
-- Used when a user re-analyzes a document with correction feedback via "Something's wrong" dialog
ALTER TABLE document_ingestion_runs ADD COLUMN IF NOT EXISTS user_feedback TEXT;
