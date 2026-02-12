# Counterparty Clean-Break

## What Changed
- AI extraction now provides an authoritative `counterparty` object for invoice, credit note, and receipt flows.
- Contact resolution consumes this authoritative snapshot directly.
- If authoritative counterparty is missing or unusable, processing sets contact resolution to `PendingReview` and skips matching.

## Matching Policy
- Deterministic contact linking remains unchanged (VAT-first, then IBAN/name, then suggestions/auto-create rules).
- AI is authoritative only for selecting the counterparty entity in extraction.
- Contact linking, auto-create, and evidence gates stay deterministic backend logic.

## Backward Compatibility
- No legacy fallback path is kept for direction-based snapshot reconstruction.
- No migration script is included by design.
