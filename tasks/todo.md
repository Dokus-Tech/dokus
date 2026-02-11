# Fix: BILL misclassified as INVOICE — wrong counterparty

## Plan

### 1. Add `associatedPersonNames` to `InputWithTenantContext` interface
- [x] Add `val associatedPersonNames: List<String> get() = emptyList()` to `InputWithTenantContext`
- [x] Update `tenantContextInjectorNode` prompt to include person names + fuzzy matching guidance

### 2. Add `associatedPersonNames` field to `AcceptDocumentInput`
- [x] Add `override val associatedPersonNames: List<String> = emptyList()` to `AcceptDocumentInput`

### 3. Fetch tenant member names in `DocumentProcessingWorker`
- [x] Inject `UserRepository` dependency
- [x] Fetch active member names and pass to `AcceptDocumentInput`

### 4. Strengthen classification prompt in `ClassifyDocumentSubGraph`
- [x] Remove `@Suppress("UnusedReceiverParameter")`, use `tenant` in prompt
- [x] Add fuzzy name matching guidance
- [x] Make INVOICE vs BILL distinction explicit with tenant name

### 5. Verify
- [x] Compile: `./gradlew :features:ai:backend:compileKotlin` — BUILD SUCCESSFUL
- [x] Compile: `./gradlew :backendApp:compileKotlin` — BUILD SUCCESSFUL

---

## Review

### Changes Summary

**4 files modified:**

| File | Change |
|------|--------|
| `TenantContextInjectorNode.kt` | Added `associatedPersonNames` to `InputWithTenantContext` interface (default `emptyList()`). Converted `Tenant.prompt` extension to `buildTenantPrompt(tenant, names)` function. Added person names block + fuzzy name matching guidance to the prompt. |
| `AcceptDocumentStrategy.kt` | Added `associatedPersonNames: List<String> = emptyList()` to `AcceptDocumentInput` |
| `DocumentProcessingWorker.kt` | Injected `UserRepository`. Fetches active member names before creating `AcceptDocumentInput`. |
| `ClassifyDocumentSubGraph.kt` | Removed `@Suppress("UnusedReceiverParameter")`. Now uses `tenant.legalName` and `tenant.vatNumber` explicitly in INVOICE vs BILL distinction. Added fuzzy name matching guidance. |

### What the fix addresses

1. **Ambiguous prompt** → Classification prompt now says "Issued BY *Invoid Vision*" instead of "Issued BY the business"
2. **Missing person names** → Tenant member names (e.g., "Artem Kuznetsov") are injected into context, so the model knows they belong to the tenant
3. **Strict name matching** → Both prompts now instruct the model to match by similarity, not exact string, and treat VAT number as the strongest identifier
