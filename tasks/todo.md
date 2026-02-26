# Invoice Creation Refactor — Fix Plan

## Fix Order (product-prioritized)

### Critical (must fix before merge)

- [ ] **1. Reinstate PEPPOL send guard**
  - `PeppolRoutes.kt` — restore draft-status check for invoices without `documentId`
  - Rule: extraction-linked → must be confirmed; manual outbound → only on explicit submit, not draft save
  - Guard: `if (invoice.status == Draft && sourceDocumentId == null) throw PeppolSendRequiresConfirmedDocument`

- [ ] **2. Fix SaveDraft delivery method mismatch**
  - `CreateInvoiceContainer.kt:383-386` — draft save currently sends user's delivery preference (e.g. Peppol) in the request body while the use case treats `null` as draft
  - Fix: when `deliveryMethod` is null, pass `null` (or a safe default) to `toCreateInvoiceRequest()`, not the user's preference
  - Separate concern: `preferredDeliveryMethod` (persisted for UX) vs `requestedAction` (what actually happens)

- [ ] **3. Add retry story for delivery failures**
  - `CashflowDocumentUseCaseImpls.kt` — when create succeeds but delivery fails, return structured error with `invoiceId`
  - `SubmitInvoiceWithDeliveryResult` — add a `DeliveryFailed(invoiceId, error)` variant
  - Container/UI — show retry option for delivery on existing invoice instead of generic error

- [ ] **4. Fix PEPPOL status magic string**
  - `CreateInvoiceContainer.kt:465` — replace `== "found"` with typed enum or constant
  - Preferred: change `PeppolStatusResponse.status` to `PeppolLookupStatus` enum (already exists in codebase)
  - Fallback: if unknown value arrives, show "status unknown, refresh" not silent PDF fallback

### Medium (product-critical)

- [ ] **5. Fix PDF line item truncation**
  - `InvoicePdfService.kt:92-107` — implement multi-page rendering
  - If not feasible now: hard-fail with clear error "too many line items" rather than silent truncation

- [ ] **6. Fix contact lookup failure in PDF**
  - `InvoiceRoutes.kt:171-174` — abort PDF generation if contact cannot be loaded
  - Return user-safe error: "Couldn't load client details, please retry"

- [ ] **7. Add dates/terms controls to desktop layout**
  - `CreateInvoiceScreen.kt` — `DesktopInvoiceCreateContent` is missing `InvoiceDatesSection`, dueDateMode toggle, payment terms field
  - Extract into a reusable `InvoiceDatesEditor` composable, add to desktop

- [ ] **8. Fix due date recompute on invoice update**
  - `InvoiceRepository.kt:390-391` — `updateInvoice` doesn't recalculate dueDate when paymentTermsDays changes
  - Apply same derivation logic as `createInvoice`

### Medium (quality)

- [ ] **9. Fix race condition in handleSelectClient**
  - `CreateInvoiceContainer.kt:252-290` — guard `onSuccess` block to check selectedClient still matches
  - Cancel previous job or ignore stale results

- [ ] **10. Fix PDFBox font thread safety**
  - `InvoicePdfService.kt:26-27` — move font instantiation inside `renderPdf`, scoped per document

- [ ] **11. Fix internal error message leakage**
  - `InvoiceRoutes.kt:167-181` — log detailed error server-side, return generic message to client

### Low (schedule, don't block)

- [ ] **12. Fix IBAN empty string handling in mapper**
  - `CreateInvoiceRequestMapper.kt:48` — add `.takeIf { it.isNotBlank() }` guard like BIC already has

- [ ] **13. Fix empty body POST content-type**
  - `CashflowRemoteDataSourceImpl.kt:198` — remove `contentType(Json)` from bodyless POST

- [ ] **14. Hardcoded English strings → string resources**
  - `CreateInvoiceScreen.kt` — 30+ raw literals need `stringResource(Res.string.*)` calls

- [ ] **15. Preview dialog placeholder**
  - Replace raw-values debug view with proper preview or remove button until ready

## Review
(to be filled after completion)
