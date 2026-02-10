# Frontend Document Review: Use DocumentDraftData Directly

**Status:** Complete
**Date:** 2026-02-10

---

## Plan

### 0. Delete dead code files
- [x] Delete `DocumentReviewModels.kt` (EditableExtractedData + Editable*Fields)
- [x] Delete `DocumentReviewDraftDataMapper.kt`
- [x] Delete `components/forms/InvoiceForm.kt` (dead code)
- [x] Delete `components/forms/BillForm.kt` (dead code)
- [x] Delete `components/forms/ReviewFormCommon.kt` (dead code)
- [x] Delete `DocumentReviewFieldEditor.kt` (dead code — all handlers replaced)

### 1. Update `DocumentReviewState.kt`
- [x] Replace `editableData: EditableExtractedData` with `draftData: DocumentDraftData?`
- [x] Update all extension properties to switch on sealed type
- [x] Add `documentType` and `isContactRequired` internal extensions

### 2. Update reducers/handlers
- [x] `DocumentReviewLoader.kt` — store draftData directly
- [x] `DocumentReviewActions.kt` — remove mapper, simplify save/confirm/discard
- [x] `DocumentReviewReducer.kt` — remove mapper/editor, inline handleSelectDocumentType
- [x] `DocumentReviewLineItems.kt` — fix suspend + withState + sealed when

### 3. Update composables
- [x] `ReviewDetailsCards.kt` — switch on draftData sealed type
- [x] `ReviewAmountsCard.kt` — switch on draftData, use Money?.toString()
- [x] `CounterpartyInfoMapper.kt` — switch on draftData, use value classes
- [x] `MobileTabContent.kt` — fix items→lineItems
- [x] `ReviewContent.kt` — fix string resource import + editableData→draftData

### 4. Fix FinancialDocumentDto missing branches
- [x] `CashflowCard.kt` — add ProForma/Quote/PO branches
- [x] `CashflowExtensions.kt` — add new type branches
- [x] `FinancialDocumentTable.kt` — add new type branches
- [x] `DocumentReviewActions.kt` — handleViewEntity branches
- [x] Added `document_type_quote` and `document_type_purchase_order` string resources

### 5. Clean up intents
- [x] `DocumentReviewIntent.kt` — remove dead field update intents + enums
- [x] `DocumentReviewContainer.kt` — remove dead intent dispatches

### 6. Verify compilation
- [x] `./gradlew :features:cashflow:presentation:compileKotlinWasmJs` — 0 errors

---

## Review

### Summary
Eliminated the `EditableExtractedData` duplicate layer. The document review screen now uses `DocumentDraftData` (sealed interface with 4 subtypes) directly as its state.

### Files deleted (6)
- `DocumentReviewModels.kt` — EditableExtractedData + 4 Editable*Fields data classes
- `DocumentReviewDraftDataMapper.kt` — Mapper converting editable↔draft
- `DocumentReviewFieldEditor.kt` — Field update handlers (dead code, no composable emitted these)
- `InvoiceForm.kt`, `BillForm.kt`, `ReviewFormCommon.kt` — Dead form composables

### Files modified (14)
- **DocumentReviewState.kt** — `editableData: EditableExtractedData` → `draftData: DocumentDraftData?`; all extensions switch on sealed type
- **DocumentReviewLoader.kt** — Store draftData directly, removed EditableExtractedData.fromDraftData()
- **DocumentReviewActions.kt** — Removed mapper, use draftData directly for save/confirm/discard
- **DocumentReviewReducer.kt** — Removed mapper/editor, inlined handleSelectDocumentType
- **DocumentReviewLineItems.kt** — Made updateLineItems suspend + withState, switch on sealed type
- **DocumentReviewIntent.kt** — Removed 4 dead field update intents + 4 field enums
- **DocumentReviewContainer.kt** — Removed 4 dead intent dispatch cases
- **ReviewDetailsCards.kt** — Switch on draftData sealed type, added padding import
- **ReviewAmountsCard.kt** — Switch on draftData, use Money?.toString()
- **CounterpartyInfoMapper.kt** — Switch on draftData, use value classes (VatNumber.value, Iban.value)
- **MobileTabContent.kt** — Fixed editableData.invoice?.items → draftData is InvoiceDraftData
- **ReviewContent.kt** — Fixed editableData → draftData, added missing string import
- **CashflowCard.kt / CashflowExtensions.kt / FinancialDocumentTable.kt** — Added ProForma/Quote/PO branches
- **cashflow.xml** — Added document_type_quote and document_type_purchase_order strings
