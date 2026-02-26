# Invoice Creation Refactor — Fix Plan

## Fix Order (product-prioritized)

### Critical (must fix before merge)

- [x] **1. Reinstate PEPPOL send guard**
  - `PeppolRoutes.kt` — restored `else if (invoice.status == Draft)` branch

- [x] **2. Fix SaveDraft delivery method mismatch**
  - `CreateInvoiceContainer.kt` — clarified variable naming: `persistedPreference` (for DB) vs `deliveryMethod` (action control)

- [x] **3. Add retry story for delivery failures**
  - Added `DeliveryFailed(invoiceId, error)` to `SubmitInvoiceWithDeliveryResult`
  - Use case catches delivery failures separately, preserves the created invoice
  - Container navigates to the existing invoice with error message for retry

- [x] **4. Fix PEPPOL status magic string**
  - Added `isFound` property + string constants to `PeppolStatusResponse`
  - Container uses `isFound` instead of `== "found"`
  - Backend uses `STATUS_UNKNOWN` constant

### Medium (product-critical)

- [x] **5. Fix PDF line item truncation**
  - Rewrote `InvoicePdfService` with `PdfWriter` helper that auto-creates new pages at bottom margin
  - Also fixed font thread-safety (fonts now scoped per document)

- [x] **6. Fix contact lookup failure in PDF**
  - PDF endpoint now fails fast if contact cannot be loaded
  - Generic error messages returned to client (no internal detail leakage)

- [x] **7. Add dates/terms controls to desktop layout**
  - Extracted `DatesTermsEditor` composable (date pickers, dueDateMode toggle, payment terms)
  - Used by both desktop and mobile layouts

- [x] **8. Fix due date recompute on invoice update**
  - `updateInvoice` now derives effectiveDueDate from paymentTermsDays, matching createInvoice

### Medium (quality)

- [x] **9. Fix race condition in handleSelectClient**
  - Added stale-data guards in both latest invoice callback and PEPPOL status callback
  - Checks `selectedClient?.id == client.id` before applying results

- [x] **10. Fix PDFBox font thread safety**
  - Fixed as part of item 5 (fonts scoped per document)

- [x] **11. Fix internal error message leakage**
  - Fixed as part of item 6 (generic messages to client)

### Low (schedule, don't block)

- [x] **12. Fix IBAN empty string handling in mapper**
  - Added `.takeIf { it.isNotBlank() }` guard matching BIC pattern

- [x] **13. Fix empty body POST content-type**
  - Removed `contentType(Json)` from bodyless PDF generation POST

- [ ] **14. Hardcoded English strings → string resources**
  - `CreateInvoiceScreen.kt` — 30+ raw literals need `stringResource(Res.string.*)` calls
  - Deferred: requires adding resource entries, can be phased

- [ ] **15. Preview dialog placeholder**
  - Replace raw-values debug view with proper preview or remove button until ready
  - Deferred: UX design decision needed

## Review

### Files modified (13 files)
1. `PeppolRoutes.kt` — restored draft status guard
2. `CreateInvoiceContainer.kt` — delivery mismatch fix, race condition guards, magic string removal, delivery failure handling
3. `CashflowDocumentUseCases.kt` — added `DeliveryFailed` result variant
4. `CashflowDocumentUseCaseImpls.kt` — separated delivery failure from create failure
5. `Peppol.kt` — added `isFound`, string constants to `PeppolStatusResponse`
6. `PeppolRecipientResolver.kt` — uses `STATUS_UNKNOWN` constant
7. `InvoicePdfService.kt` — complete rewrite with multi-page support, per-document fonts
8. `InvoiceRoutes.kt` — fail-fast contact lookup, generic error messages
9. `CreateInvoiceScreen.kt` — extracted `DatesTermsEditor`, added to desktop layout
10. `InvoiceRepository.kt` — due date derivation on update
11. `CreateInvoiceRequestMapper.kt` — IBAN empty string guard
12. `CashflowRemoteDataSourceImpl.kt` — removed unnecessary content-type header

### Remaining items (deferred)
- i18n (item 14): 30+ hardcoded English strings in CreateInvoiceScreen.kt
- Preview dialog (item 15): placeholder debug view behind user-visible button
