# Per-Type Draft Tables — Complete

## All phases done
- [x] Draft + confirmed tables created (102 table objects in DokusSchema)
- [x] Per-type entities (102 types) with companion objects
- [x] Per-type mappers (102 `Entity.Companion.from(row)` functions)
- [x] DraftRepository with type-dispatched CRUD (read, write, delete)
- [x] DocumentRecordLoader reads from draft tables (no JSON fallback)
- [x] AI pipeline writes to draft tables (DocumentProcessingWorker + DocumentTruthService)
- [x] User draft edits write to draft tables (PATCH /documents/{id}/draft)
- [x] Document type change: old type deleted, new type inserted
- [x] Confirmation deletes draft after creating confirmed entity
- [x] `extracted_data` JSON column removed from DocumentsTable
- [x] All references to extractedData/canonicalData removed (24 files)
- [x] Full compile check (frontend + backend + tests)

## Architecture
```
AI extraction → ingestion_runs.raw_extraction_json (immutable audit trail)
             → per-type draft table (structured, queryable, editable)

User edits   → updates draft table row (+ metadata on DocumentsTable)
Type change  → deletes old type draft, inserts new type draft

User confirms → confirmation service reads draft table
              → writes to confirmed entity table
              → deletes draft table row

Frontend     → receives DocDto (Draft or Confirmed variant via DocumentRecordLoader)
```
