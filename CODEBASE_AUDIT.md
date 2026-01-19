# CODEBASE AUDIT REPORT
Generated: 2026-01-18 23:43

## EXECUTIVE SUMMARY
- Total files analyzed: 3390
- Critical issues (blocks shipping): 1
- High priority (fix within 2 weeks): 5
- Low priority (fix when profitable): 120
- Estimated cleanup effort: 208 hours

## 1. ARCHITECTURE HEALTH
### 1.1 Module Structure
- Top-level directories: .auto-claude, .claude, .fleet, .github, .kotlin, backendApp, build-logic, ci, claude, composeApp, config, deployment, docs, features, foundation, gradle, iosApp, scope, scripts, tasks
- Responsibilities (inferred):
  - `.auto-claude`: Other
  - `.claude`: Other
  - `.fleet`: Other
  - `.github`: Other
  - `.kotlin`: Other
  - `backendApp`: Backend Ktor application
  - `build-logic`: Gradle conventions/plugins
  - `ci`: CI tooling
  - `claude`: Other
  - `composeApp`: KMP client app
  - `config`: Static config (detekt, etc.)
  - `deployment`: Docker/ops manifests
  - `docs`: Documentation
  - `features`: Feature slices (auth, cashflow, contacts, AI)
  - `foundation`: Shared modules (domain, design system, db, platform, etc.)
  - `gradle`: Other
  - `iosApp`: Other
  - `scope`: Other
  - `scripts`: Dev scripts
  - `tasks`: Other
- Circular dependencies: not detected via static path scan (requires Gradle dependency graph to confirm).

### 1.2 Layer Violations
| File | Line | Violation | Should Be |
|------|------|-----------|-----------|
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/IdentityRoutes.kt | 1 | Route contains direct service orchestration (heuristic) | Move orchestration into service/use case |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/Routes.kt | 1 | Route contains direct service orchestration (heuristic) | Move orchestration into service/use case |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/LookupRoutes.kt | 1 | Route contains direct service orchestration (heuristic) | Move orchestration into service/use case |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/TeamRoutes.kt | 1 | Route contains direct service orchestration (heuristic) | Move orchestration into service/use case |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/AvatarRoutes.kt | 1 | Route contains direct service orchestration (heuristic) | Move orchestration into service/use case |
| (heuristic) | - | DB queries outside repository not automatically detected | Ensure DB access is limited to repository layer |
| (heuristic) | - | UI logic in shared modules not automatically detected | Keep UI in presentation modules only |
| (actual) | - | Domain entities exposed in API responses (e.g., `PeppolRegistrationDto`) | Wrap in response models or API DTOs if you want strict boundaries |

### 1.3 God Files (>300 lines)
| File | Lines | Responsibilities | Suggested Split |
|------|------|------------------|-----------------|
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/WorkspaceSettingsScreen.kt | 1182 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/enums/FinancialEnums.kt | 1177 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/exceptions/DokusException.kt | 1013 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/coordinator/AutonomousProcessingCoordinator.kt | 927 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSourceImpl.kt | 906 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/DocumentRecordRoutes.kt | 757 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactAutocomplete.kt | 725 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSource.kt | 714 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiDocumentSchemas.kt | 714 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/prompts/AgentPrompts.kt | 710 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactFormContainer.kt | 707 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/ContactEditSheet.kt | 707 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/contacts/ContactRepository.kt | 694 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/InvoiceRepository.kt | 682 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/components/CashflowDetailPane.kt | 661 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactDetailsContainer.kt | 656 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| backendApp/src/main/kotlin/tech/dokus/backend/worker/DocumentProcessingWorker.kt | 644 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/NotesBottomSheet.kt | 642 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/create/LookupStepContent.kt | 618 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/mvi/CashflowLedgerContainer.kt | 573 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/upload/UploadOverlayContent.kt | 572 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/TeamSettingsScreen.kt | 572 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/CreateInvoiceContainer.kt | 570 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/background/AnimatedBackground.kt | 570 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| backendApp/src/main/kotlin/tech/dokus/backend/services/documents/DocumentConfirmationService.kt | 556 | Multiple concerns (inferred from path) | Split into smaller, focused files |
| ... | ... | 100 more files >300 lines | Split by concern |

## 2. TYPE SYSTEM HEALTH
### 2.1 Duplicate Definitions
| Type | Location 1 | Location 2 | Identical? | Canonical Location |
|------|------------|------------|------------|-------------------|
| ScreenshotViewport | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/ScreenshotViewport.kt | features/contacts/presentation/src/androidUnitTest/kotlin/tech/dokus/features/contacts/presentation/screenshot/ScreenshotViewport.kt | Unknown | foundation/domain (if shared) |
| Content | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceSelectContract.kt | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactsContract.kt | Unknown | foundation/domain (if shared) |
| Error | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceSelectContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceCreateContract.kt | Unknown | foundation/domain (if shared) |
| UpdateCompanyName | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceCreateContract.kt | composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/WorkspaceSettingsContract.kt | Unknown | foundation/domain (if shared) |
| UpdateVatNumber | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceCreateContract.kt | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactFormContract.kt | Unknown | foundation/domain (if shared) |
| UpdateAddress | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceCreateContract.kt | composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/WorkspaceSettingsContract.kt | Unknown | foundation/domain (if shared) |
| Idle | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/LoginContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/NewPasswordContract.kt | Unknown | foundation/domain (if shared) |
| UpdateEmail | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/LoginContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/RegisterContract.kt | Unknown | foundation/domain (if shared) |
| UpdatePassword | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/LoginContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/NewPasswordContract.kt | Unknown | foundation/domain (if shared) |
| Editing | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ProfileSettingsContract.kt | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactFormContract.kt | Unknown | foundation/domain (if shared) |
| Saving | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ProfileSettingsContract.kt | features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/CreateInvoiceContract.kt | Unknown | foundation/domain (if shared) |
| UpdateFirstName | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ProfileSettingsContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/RegisterContract.kt | Unknown | foundation/domain (if shared) |
| UpdateLastName | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ProfileSettingsContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/RegisterContract.kt | Unknown | foundation/domain (if shared) |
| Submitting | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/NewPasswordContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ForgotPasswordContract.kt | Unknown | foundation/domain (if shared) |
| Success | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/NewPasswordContract.kt | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ForgotPasswordContract.kt | Unknown | foundation/domain (if shared) |
| Params | features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ServerConnectionContainer.kt | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactDetailsContainer.kt | Unknown | foundation/domain (if shared) |
| UpdateSearchQuery | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactsContract.kt | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactMergeIntent.kt | Unknown | foundation/domain (if shared) |
| SelectContact | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactsContract.kt | features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewIntent.kt | Unknown | foundation/domain (if shared) |
| NavigateToEditContact | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactsContract.kt | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactDetailsContract.kt | Unknown | foundation/domain (if shared) |
| ShowError | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactsContract.kt | features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/CreateContactContract.kt | Unknown | foundation/domain (if shared) |
| ... | ... | ... | ... | 52 more duplicates |

### 2.2 Type Safety Issues
| File | Line | Issue | Severity |
|------|------|-------|----------|
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/CurrentServerSection.kt | 106 | Non-null assertion (!!) | High |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactFormContainer.kt | 493 | Non-null assertion (!!) | High |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/route/CashflowLedgerRoute.kt | 49 | Non-null assertion (!!) | High |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/mvi/CashflowLedgerContainer.kt | 176 | Non-null assertion (!!) | High |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceClientSidePanel.kt | 368 | Non-null assertion (!!) | High |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/validation/MathValidatorTest.kt | 81 | Non-null assertion (!!) | High |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/validation/MathValidatorTest.kt | 82 | Non-null assertion (!!) | High |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/validation/MathValidatorTest.kt | 83 | Non-null assertion (!!) | High |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/ensemble/ConsensusEngine.kt | 48 | Non-null assertion (!!) | High |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/ensemble/ConsensusEngine.kt | 127 | Non-null assertion (!!) | High |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/ensemble/ConsensusEngine.kt | 201 | Non-null assertion (!!) | High |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/judgment/JudgmentAgent.kt | 164 | Non-null assertion (!!) | High |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/judgment/JudgmentAgent.kt | 164 | Non-null assertion (!!) | High |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/judgment/JudgmentCriteria.kt | 72 | Non-null assertion (!!) | High |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/judgment/JudgmentCriteria.kt | 104 | Non-null assertion (!!) | High |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/DocumentUploadTask.kt | 65 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewIntent.kt | 24 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewIntent.kt | 29 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewIntent.kt | 34 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewFieldEditor.kt | 12 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewFieldEditor.kt | 42 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewFieldEditor.kt | 73 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewReducer.kt | 55 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewReducer.kt | 58 | Any/Any? usage | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewReducer.kt | 61 | Any/Any? usage | Medium |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/ValidateServerUseCaseImpl.kt | 74 | Unsafe cast (`as`) | Medium |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/datasource/ContactLocalDataSourceImpl.kt | 21 | Unsafe cast (`as`) | Medium |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactFormPane.kt | 73 | Unsafe cast (`as`) | Medium |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/screen/ContactsScreen.kt | 148 | Unsafe cast (`as`) | Medium |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/screen/ContactsScreen.kt | 150 | Unsafe cast (`as`) | Medium |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSource.kt | 260 | Unsafe cast (`as`) | Medium |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSource.kt | 277 | Unsafe cast (`as`) | Medium |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSource.kt | 326 | Unsafe cast (`as`) | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/DeliveryMethodOption.kt | 28 | Unsafe cast (`as`) | Medium |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/mvi/CashflowLedgerContainer.kt | 24 | Unsafe cast (`as`) | Medium |

### 2.3 Missing Abstractions
- Repeated `when` branches on status enums (e.g., Peppol and cashflow state mapping) suggest a single mapper or sealed UI model could reduce drift.
- Multiple null checks on tenant context (VAT, address) indicate a stronger “workspace readiness” value object could simplify flow logic.
- Stringly-typed identifiers in HTTP routes (IDs as Strings) could be wrapped earlier in value classes to avoid invalid usage.

## 3. DEAD CODE INVENTORY
### 3.1 Unused Imports
| File | Unused Import Count | List |
|------|---------------------|------|
| (heuristic) | - | Use IDE or detekt to compute unused imports accurately |

### 3.2 Unreferenced Code
| File | Type | Name | Last Modified |
|------|------|------|---------------|
| (heuristic) | - | Full static call-graph analysis not run. Recommend IDE inspection or detekt unused symbols. | - |

### 3.3 Commented Code
| File | Lines | Description |
|------|-------|-------------|
| (heuristic) | - | Not scanned for commented-out blocks >5 lines. |

### 3.4 Dead Database Artifacts
| Table/Column | Defined In | Referenced By |
|--------------|------------|---------------|
| RefundClaimsTable | foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/RefundClaimsTable.kt | 2 refs (table + schema only) |
| DocumentLineItemsTable | foundation/database/src/main/kotlin/tech/dokus/database/tables/documents/DocumentLineItemsTable.kt | 2 refs (table + schema only) |

## 4. DUPLICATION ANALYSIS
### 4.1 Copy-Paste Code
| Pattern | Occurrences | Files | Lines Each |
|---------|-------------|-------|------------|
| 48c79e9d | 8 | features/auth/domain/build.gradle.kts, features/auth/presentation/build.gradle.kts, features/cashflow/domain/build.gradle.kts | ~12 |
| ec8c72c4 | 7 | composeApp/build.gradle.kts, features/auth/data/build.gradle.kts, features/cashflow/data/build.gradle.kts | ~12 |
| 2cfec0e6 | 7 | composeApp/build.gradle.kts, features/auth/data/build.gradle.kts, features/cashflow/data/build.gradle.kts | ~12 |
| 6389b84f | 7 | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | ~12 |
| 399dc367 | 7 | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | ~12 |
| 0fd47d4f | 7 | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | ~12 |
| ff9a17ca | 7 | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | ~12 |
| 36c8961d | 7 | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | ~12 |
| 47358185 | 7 | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | ~12 |
| 44c86472 | 7 | features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt, features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | ~12 |
| ... | ... | 856 more duplicated blocks | ... |

### 4.2 Similar Functions
| Function 1 | Function 2 | Similarity | Could Extract |
|------------|------------|------------|---------------|
| (heuristic) | - | Not computed; recommend structural similarity pass or jscpd | - |

## 5. NAMING & CONSISTENCY
### 5.1 Naming Convention Violations
| File | Name | Issue | Suggested Fix |
|------|------|-------|--------------|
| (heuristic) | - | Automated naming checks not run | Use detekt naming rules |

### 5.2 Package Structure Issues
- Packages are primarily feature- and layer-oriented; some `foundation` modules mix infra + domain (acceptable for KMP but should be monitored).
- No obvious misplaced files detected by path heuristics. Manual review recommended for generated or legacy modules.

## 6. ERROR HANDLING
### 6.1 Error Handling Patterns
- `Result<T>` return types (repositories/services)
- Exceptions via `DokusException`
- `runCatching` wrappers
- Ktor `call.respond` with status codes

### 6.2 Inconsistencies
| File | Pattern Used | Expected Pattern |
|------|--------------|------------------|
| (heuristic) | Mixed Result + exceptions in service layer | Standardize on Result or exception propagation |

### 6.3 Swallowed Exceptions
| File | Line | Exception Type |
|------|------|----------------|
| (heuristic) | - | Not scanned for empty catch blocks |

## 7. SECURITY SCAN
### 7.1 Hardcoded Secrets (CRITICAL)
| File | Line | Type | Value (partial) |
|------|------|------|-----------------|
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/storage/TokenStorage.kt | 16 | literal in config/code | private const val KEY_ACCESS_TOKEN = "auth.access_token" |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/storage/TokenStorage.kt | 17 | literal in config/code | private const val KEY_REFRESH_TOKEN = "auth.refresh_token" |
| features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | 77 | literal in config/code | password = "password123", |
| features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | 88 | literal in config/code | password = "password123", |
| features/cashflow/data/src/jvmTest/kotlin/tech/dokus/features/cashflow/gateway/PeppolGatewayImplsTest.kt | 35 | literal in config/code | apiKey = "key", |
| features/cashflow/data/src/jvmTest/kotlin/tech/dokus/features/cashflow/gateway/PeppolGatewayImplsTest.kt | 36 | literal in config/code | apiSecret = "secret", |
| features/cashflow/data/src/commonTest/kotlin/tech/dokus/features/cashflow/usecase/PeppolUseCasesTest.kt | 42 | literal in config/code | apiKey = "key", |
| features/cashflow/data/src/commonTest/kotlin/tech/dokus/features/cashflow/usecase/PeppolUseCasesTest.kt | 43 | literal in config/code | apiSecret = "secret", |
| backendApp/src/main/resources/application-local.conf | 15 | literal in config/code | password = "devpassword" |
| backendApp/src/main/resources/application.conf | 62 | literal in config/code | secret = "local-secret-key" |
| backendApp/src/main/resources/application.conf | 256 | literal in config/code | apiSecret = "rK1Enmltoey8DYnw5raPnPNFN1xuoGxG" |

### 7.2 SQL Injection Risks
| File | Line | Query |
|------|------|-------|
| build.gradle.kts | 156 | description = "Delete all screenshot baseline images." |
| features/cashflow/presentation/src/androidUnitTest/kotlin/tech/dokus/features/cashflow/presentation/screenshot/CashflowScreenshotTest.kt | 299 | text = "Select a client...", |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/ai/DocumentChunksRepository.kt | 86 | append("SELECT ") |
| foundation/app-common/src/desktopMain/kotlin/tech/dokus/foundation/app/picker/FilePicker.desktop.kt | 78 | FilePickerType.Image -> "Select Image" |
| foundation/app-common/src/desktopMain/kotlin/tech/dokus/foundation/app/picker/FilePicker.desktop.kt | 79 | FilePickerType.Document -> "Select Documents" |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/DialogScreenshotTest.kt | 110 | text = "Delete Item", |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/DatePickerScreenshotTest.kt | 50 | displayValue = "Select date", |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/DatePickerScreenshotTest.kt | 93 | displayValue = "Select date", |

### 7.3 Missing Auth Checks
| Route | Method | Has Auth? |
|-------|--------|-----------|
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/IdentityRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/Routes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/LookupRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/TeamRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/AvatarRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/TenantRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/AccountRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/payment/Routes.kt | (various) | Unknown |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/payment/PaymentRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/contacts/Routes.kt | (various) | Unknown |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/contacts/ContactRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/MultipartUpload.kt | (various) | Unknown |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/CashflowRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/CashflowEntriesRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/BillRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/PeppolWebhookRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/PeppolRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/ChatRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/InvoiceRoutes.kt | (various) | Likely |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/DocumentPageRoutes.kt | (various) | Likely |
| ... | ... | 7 more routes |

## 8. DEPENDENCY HEALTH
### 8.1 Circular Dependencies
Not detected via static scan. Use Gradle dependency report to confirm.
### 8.2 God Objects
| Class | Dependency Count | Depended On By |
|-------|------------------|----------------|
| (heuristic) | - | Identify via DI graph / Koin modules |
### 8.3 Tight Coupling
- Direct repository usage in services (expected) but some features bypass gateways (spot-check needed).

## 9. DATABASE LAYER
### 9.1 Schema Analysis
| Table | Columns | Has Created/Updated | Has Soft Delete | Repository Exists |
|-------|---------|---------------------|-----------------|-------------------|
| TenantInvitationsTable | 11 | Yes | No | Unknown |
| TenantTable | 11 | Yes | No | Yes |
| PasswordResetTokensTable | 5 | No | No | Unknown |
| UsersTable | 11 | Yes | No | Unknown |
| RefreshTokensTable | 5 | No | No | Unknown |
| TenantSettingsTable | 21 | Yes | No | Unknown |
| TenantMembersTable | 6 | Yes | No | Unknown |
| AddressTable | 8 | Yes | No | Yes |
| PeppolSettingsTable | 11 | Yes | No | Yes |
| PeppolDirectoryCacheTable | 14 | Yes | No | Yes |
| PeppolTransmissionsTable | 16 | Yes | No | Unknown |
| PeppolRegistrationTable | 12 | Yes | No | Yes |
| PaymentsTable | 8 | No | No | Unknown |
| ContactNotesTable | 7 | Yes | No | Unknown |
| ContactAddressesTable | 6 | Yes | No | Unknown |
| ContactsTable | 17 | Yes | No | Unknown |
| CreditNotesTable | 16 | Yes | No | Unknown |
| InvoicesTable | 23 | Yes | No | Unknown |
| InvoiceItemsTable | 10 | Yes | No | Unknown |
| BillsTable | 22 | Yes | No | Unknown |
| RefundClaimsTable | 11 | Yes | No | Unknown |
| InvoiceNumberSequencesTable | 5 | Yes | No | Unknown |
| ExpensesTable | 17 | Yes | No | Unknown |
| CashflowEntriesTable | 15 | Yes | No | Yes |
| BankConnectionsTable | 13 | Yes | No | Unknown |
| BankTransactionsTable | 13 | No | No | Unknown |
| DocumentChunksTable | 15 | No | No | Yes |
| ChatMessagesTable | 16 | No | No | Unknown |
| DocumentLinksTable | 6 | No | No | Unknown |
| DocumentIngestionRunsTable | 19 | No | No | Unknown |
| DocumentLineItemsTable | 14 | Yes | No | Unknown |
| DocumentDraftsTable | 21 | Yes | No | Unknown |
| DocumentsTable | 8 | No | No | Unknown |

### 9.2 Query Patterns
- Potential N+1 risks not automatically detected; review repository loops over rows with per-row queries.
- Index coverage: many tables define unique indexes on IDs; review high-volume filters (tenantId, status) for indexes.
- Transactions: Exposed DSL is wrapped in `dbQuery` (ok); ensure long workflows use explicit transactions where needed.

## 10. PRIORITIZED ACTION PLAN
### CRITICAL (Fix before any customer)
| Issue | File | Effort | Why Critical |
|-------|------|--------|--------------|
| Hardcoded secrets/config defaults | application*.conf | 1-2h | Risk of deploying with insecure defaults |

### HIGH (Fix within 2 weeks of launch)
| Issue | File | Effort | Impact |
|-------|------|--------|--------|
| Large god files (>450 LOC) | Multiple | 8-20h | Hard to maintain, higher defect risk |
| Mixed error handling (Result + exceptions) | Services/routes | 6-12h | Inconsistent failure behavior |
| Auth coverage uncertain in some routes | backendApp routes | 4-8h | Potential security gaps |

### MEDIUM (Fix when you have revenue)
| Issue | File | Effort | Impact |
|-------|------|--------|--------|
| Duplicate type names | Multiple | 2-6h | Confusion + accidental misuse |
| Copy-paste blocks | Multiple | 4-8h | Bug fixes require many edits |

### LOW (Nice to have)
| Issue | File | Effort | Impact |
|-------|------|--------|--------|
| Unused imports (if any) | Multiple | <1h | Readability |
| Minor naming inconsistencies | Multiple | 2-4h | Clarity |

## 11. QUICK WINS
1. Run detekt/IDE inspection to clean unused imports and simplify warnings.
2. Add missing indexes on frequently filtered columns (tenantId, status).
3. Remove hardcoded secrets from `application*.conf` and enforce env-only in prod.
4. Consolidate repeated status-to-UI mapping into a shared mapper function.
5. Split top 3 largest files (>600 LOC) into smaller cohesive units.

## 12. METRICS SUMMARY

Total Kotlin files: 1077  Total lines of code: 148127  Average file size: 137.54 lines  Largest file: composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/WorkspaceSettingsScreen.kt (1182 lines)  Duplicate code blocks: 866  Unused functions: Unknown  Type safety issues: 273  Security concerns: 11

## APPENDIX: FILE INDEX
| Path | Lines | Primary Responsibility | Health Score (1-5) |
|------|-------|------------------------|-------------------|
| backendApp/build.gradle.kts | 142 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/Application.kt | 73 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/config/DependencyInjection.kt | 533 | Config | 3 |
| backendApp/src/main/kotlin/tech/dokus/backend/plugins/BackgroundWorkers.kt | 41 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/plugins/Database.kt | 28 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/plugins/Routing.kt | 37 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/AccountRoutes.kt | 103 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/AvatarRoutes.kt | 154 | HTTP routes | 4 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/IdentityRoutes.kt | 94 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/LookupRoutes.kt | 66 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/Routes.kt | 21 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/TeamRoutes.kt | 248 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/auth/TenantRoutes.kt | 214 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/banking/BankingRoutes.kt | 15 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/AttachmentRoutes.kt | 346 | HTTP routes | 3 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/BillRoutes.kt | 149 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/CashflowEntriesRoutes.kt | 185 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/CashflowOverviewRoutes.kt | 41 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/CashflowRoutes.kt | 88 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/ChatRoutes.kt | 480 | HTTP routes | 3 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/DocumentPageRoutes.kt | 130 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/DocumentRecordRoutes.kt | 757 | HTTP routes | 1 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/DocumentUploadRoutes.kt | 278 | HTTP routes | 4 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/ExpenseRoutes.kt | 115 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/InvoiceRoutes.kt | 172 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/MultipartUpload.kt | 31 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/PeppolRoutes.kt | 470 | HTTP routes | 3 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/cashflow/PeppolWebhookRoutes.kt | 93 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/common/CommonRoutes.kt | 21 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/contacts/ContactRoutes.kt | 353 | HTTP routes | 4 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/contacts/Routes.kt | 22 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/payment/PaymentRoutes.kt | 69 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/routes/payment/Routes.kt | 22 | HTTP routes | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/AuthService.kt | 503 | Source file | 3 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/DisabledEmailService.kt | 75 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/EmailConfig.kt | 92 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/EmailService.kt | 65 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/EmailVerificationService.kt | 146 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/PasswordResetService.kt | 188 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/RateLimitServiceInterface.kt | 57 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/RedisRateLimitService.kt | 166 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/SmtpEmailService.kt | 428 | Source file | 4 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/auth/TeamService.kt | 257 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/cashflow/BillService.kt | 195 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/cashflow/CashflowEntriesService.kt | 274 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/cashflow/CashflowOverviewService.kt | 252 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/cashflow/CreditNoteService.kt | 245 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/cashflow/ExpenseService.kt | 171 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/cashflow/InvoiceService.kt | 174 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/contacts/ContactEnrichmentService.kt | 216 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/contacts/ContactMatchingService.kt | 171 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/contacts/ContactNoteService.kt | 106 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/contacts/ContactService.kt | 193 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/documents/DocumentConfirmationService.kt | 556 | Source file | 3 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/documents/ProFormaService.kt | 195 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/pdf/PdfPreviewService.kt | 194 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/services/peppol/PeppolRecipientResolver.kt | 267 | Source file | 5 |
| backendApp/src/main/kotlin/tech/dokus/backend/worker/DocumentProcessingWorker.kt | 644 | Source file | 2 |
| backendApp/src/main/kotlin/tech/dokus/backend/worker/PeppolPollingWorker.kt | 380 | Source file | 4 |
| backendApp/src/main/kotlin/tech/dokus/backend/worker/RateLimitCleanupWorker.kt | 52 | Source file | 5 |
| backendApp/src/test/kotlin/tech/dokus/backend/cashflow/InvoiceNumberConcurrencyTest.kt | 431 | Source file | 3 |
| backendApp/src/test/kotlin/tech/dokus/backend/cashflow/InvoiceNumberYearRolloverTest.kt | 423 | Source file | 3 |
| backendApp/src/test/kotlin/tech/dokus/backend/contacts/ContactMatchingServiceTest.kt | 144 | Source file | 5 |
| backendApp/src/test/kotlin/tech/dokus/backend/pdf/PdfPreviewServiceTest.kt | 181 | Source file | 5 |
| backendApp/src/test/kotlin/tech/dokus/backend/peppol/DocumentConfirmationPolicyTest.kt | 61 | Source file | 5 |
| build-logic/convention/build.gradle.kts | 19 | Source file | 5 |
| build-logic/convention/src/main/kotlin/tech/dokus/convention/VersioningPlugin.kt | 90 | Source file | 5 |
| build-logic/convention/src/main/kotlin/tech/dokus/utils/Versions.kt | 27 | Source file | 5 |
| build-logic/convention/src/main/kotlin/tech/dokus/utils/WebCacheBuster.kt | 43 | Source file | 5 |
| build-logic/settings.gradle.kts | 32 | Source file | 5 |
| build.gradle.kts | 205 | Source file | 5 |
| composeApp/build.gradle.kts | 218 | Source file | 5 |
| composeApp/src/androidMain/kotlin/tech/dokus/app/DokusApplication.kt | 20 | Source file | 5 |
| composeApp/src/androidMain/kotlin/tech/dokus/app/MainActivity.kt | 64 | Source file | 5 |
| composeApp/src/androidMain/kotlin/tech/dokus/app/local/KoinProvided.android.kt | 19 | Source file | 5 |
| composeApp/src/androidMain/kotlin/tech/dokus/app/utils/KoinInitializer.kt | 30 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/App.kt | 59 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/AppModules.kt | 60 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/DiModule.kt | 106 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/infrastructure/ServerConfigManagerImpl.kt | 88 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/local/AppModulesInitializer.kt | 32 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/local/AppModulesProvided.kt | 13 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/local/DefaultLocalDatabaseCleaner.kt | 41 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/local/KoinProvided.kt | 14 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/module/AppDataMainModuleDi.kt | 57 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/module/AppMainModule.kt | 88 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/navigation/AppNavigationProvider.kt | 41 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/navigation/DokusNavHost.kt | 131 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/navigation/ExternalUriHandler.kt | 33 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/navigation/HomeNavigationProvider.kt | 81 | Source file | 4 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/navigation/NavDefinition.kt | 290 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/DocumentsPlaceholderScreen.kt | 60 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/EmptyScreen.kt | 12 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/HomeScreen.kt | 278 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/MoreScreen.kt | 151 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/SettingsScreen.kt | 513 | Source file | 3 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/SplashScreen.kt | 210 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/TodayScreen.kt | 345 | Source file | 4 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/UnderDevelopmentScreen.kt | 317 | Source file | 4 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/AppearanceSettingsScreen.kt | 141 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/TeamSettingsScreen.kt | 572 | Source file | 3 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/WorkspaceSettingsScreen.kt | 1182 | Source file | 2 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/route/AppearanceSettingsRoute.kt | 9 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/route/TeamSettingsRoute.kt | 89 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/screens/settings/route/WorkspaceSettingsRoute.kt | 76 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/BootstrapContainer.kt | 113 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/BootstrapContract.kt | 100 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/HomeContainer.kt | 41 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/HomeContract.kt | 49 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/SettingsContainer.kt | 69 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/SettingsContract.kt | 84 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/TeamSettingsContainer.kt | 410 | Source file | 4 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/TeamSettingsContract.kt | 155 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/TodayContainer.kt | 169 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/TodayContract.kt | 104 | Source file | 5 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/WorkspaceSettingsContainer.kt | 421 | Source file | 4 |
| composeApp/src/commonMain/kotlin/tech/dokus/app/viewmodel/WorkspaceSettingsContract.kt | 237 | Source file | 5 |
| composeApp/src/desktopMain/kotlin/tech/dokus/app/local/KoinProvided.desktop.kt | 23 | Source file | 5 |
| composeApp/src/desktopMain/kotlin/tech/dokus/app/main.kt | 40 | Source file | 5 |
| composeApp/src/iosMain/kotlin/tech/dokus/app/MainViewController.kt | 6 | Source file | 5 |
| composeApp/src/iosMain/kotlin/tech/dokus/app/local/KoinProvided.ios.kt | 23 | Source file | 5 |
| composeApp/src/wasmJsMain/kotlin/tech/dokus/app/local/KoinProvided.wasmJs.kt | 23 | Source file | 5 |
| composeApp/src/wasmJsMain/kotlin/tech/dokus/app/main.kt | 12 | Source file | 4 |
| features/ai/backend/build.gradle.kts | 55 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/agents/CategorySuggestionAgent.kt | 101 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/agents/ChatAgent.kt | 387 | Source file | 4 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/agents/DocumentClassificationAgent.kt | 120 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/agents/ExtractionAgent.kt | 151 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/client/OllamaVisionClient.kt | 159 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/config/AIModels.kt | 54 | Config | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/config/AIProviderFactory.kt | 63 | Config | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/config/ModelRegistry.kt | 38 | Config | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/config/ThrottledPromptExecutor.kt | 36 | Config | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/coordinator/AutonomousProcessingCoordinator.kt | 927 | Source file | 2 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/coordinator/AutonomousResult.kt | 291 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/ensemble/ConsensusEngine.kt | 449 | Source file | 3 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/ensemble/ConsensusModels.kt | 214 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/ensemble/PerceptionEnsemble.kt | 229 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/judgment/JudgmentAgent.kt | 413 | Source file | 3 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/judgment/JudgmentCriteria.kt | 225 | Source file | 4 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/judgment/JudgmentModels.kt | 216 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/CategorySuggestion.kt | 23 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/DocumentAIResult.kt | 131 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/DocumentAIResultDomainMapping.kt | 198 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/DocumentAIResultParsing.kt | 18 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/DocumentAIResultProvenance.kt | 124 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/DocumentAIResultThresholds.kt | 51 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/DocumentClassification.kt | 41 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/DocumentProcessingResult.kt | 31 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/ExtractedBillData.kt | 103 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/ExtractedExpenseData.kt | 80 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/ExtractedInvoiceData.kt | 181 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/models/ExtractedReceiptData.kt | 92 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/prompts/AgentPrompts.kt | 710 | Source file | 2 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/retry/FeedbackDrivenRetryAgent.kt | 262 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/retry/FeedbackPromptBuilder.kt | 289 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/retry/RetryModels.kt | 86 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/services/ChunkingService.kt | 453 | Source file | 3 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/services/DocumentImageService.kt | 195 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/services/EmbeddingService.kt | 153 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/services/RAGService.kt | 367 | Source file | 4 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/tools/FinancialToolRegistry.kt | 59 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/tools/LookupCompanyTool.kt | 96 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/tools/ValidateIbanTool.kt | 75 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/tools/ValidateOgmTool.kt | 62 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/tools/VerifyTotalsTool.kt | 78 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/utils/AmountParser.kt | 106 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/utils/MarkdownUtils.kt | 19 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/validation/AuditModels.kt | 218 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/validation/BelgianVatRateValidator.kt | 243 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/validation/ChecksumValidator.kt | 223 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/validation/ExtractionAuditService.kt | 273 | Source file | 5 |
| features/ai/backend/src/main/kotlin/tech/dokus/features/ai/validation/MathValidator.kt | 202 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/coordinator/AutonomousResultTest.kt | 348 | Source file | 3 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/coordinator/LlmJudgmentGatingTest.kt | 82 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/ensemble/ConsensusEngineTest.kt | 285 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/judgment/JudgmentAgentTest.kt | 254 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/judgment/JudgmentCriteriaTest.kt | 316 | Source file | 4 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/models/DocumentAIResultThresholdsTest.kt | 88 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/retry/FeedbackPromptBuilderTest.kt | 304 | Source file | 4 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/retry/RetryModelsTest.kt | 147 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/tools/ValidateIbanToolTest.kt | 118 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/tools/ValidateOgmToolTest.kt | 106 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/tools/VerifyTotalToolTest.kt | 113 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/validation/BelgianVatRateValidatorTest.kt | 188 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/validation/ChecksumValidatorTest.kt | 145 | Source file | 5 |
| features/ai/backend/src/test/kotlin/tech/dokus/features/ai/validation/MathValidatorTest.kt | 152 | Source file | 4 |
| features/auth/data/build.gradle.kts | 106 | Source file | 5 |
| features/auth/data/src/androidMain/kotlin/tech/dokus/features/auth/DiModule.android.kt | 17 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/DiModule.kt | 182 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/database/Database.kt | 14 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/AccountRemoteDataSource.kt | 54 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/AccountRemoteDataSourceImpl.kt | 77 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/IdentityRemoteDataSource.kt | 57 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/IdentityRemoteDataSourceImpl.kt | 88 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/LookupRemoteDataSource.kt | 20 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/LookupRemoteDataSourceImpl.kt | 32 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/TeamRemoteDataSource.kt | 63 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/TeamRemoteDataSourceImpl.kt | 87 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/TenantRemoteDataSource.kt | 96 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/datasource/TenantRemoteDataSourceImpl.kt | 156 | Remote data source | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/gateway/TeamInvitationsGatewayImpl.kt | 18 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/gateway/TeamMembersGatewayImpl.kt | 19 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/gateway/TeamOwnershipGatewayImpl.kt | 12 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/gateway/WorkspaceSettingsGatewayImpl.kt | 30 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/initializer/AuthDataInitializer.kt | 14 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/manager/AuthManager.kt | 44 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/manager/TokenManager.kt | 194 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/repository/AuthRepository.kt | 288 | Repository | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/storage/TokenStorage.kt | 106 | Source file | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/AuthUseCaseImpls.kt | 72 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/ConnectToServerUseCaseImpl.kt | 132 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/GetCurrentTenantIdUseCase.kt | 17 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/GetCurrentTenantUseCase.kt | 53 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/LoginUseCaseImpl.kt | 62 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/LogoutUseCaseImpl.kt | 56 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/RegisterAndLoginUseCaseImpl.kt | 90 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/SearchCompanyUseCaseImpl.kt | 44 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/SelectTenantUseCase.kt | 36 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/TeamSettingsUseCaseImpls.kt | 67 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/TenantUseCaseImpls.kt | 20 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/ValidateServerUseCaseImpl.kt | 93 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/usecases/WorkspaceSettingsUseCaseImpls.kt | 56 | Use case | 5 |
| features/auth/data/src/commonMain/kotlin/tech/dokus/features/auth/utils/JwtDecoder.kt | 173 | Source file | 5 |
| features/auth/data/src/commonTest/kotlin/tech/dokus/features/auth/gateway/TeamGatewayImplsTest.kt | 171 | Source file | 5 |
| features/auth/data/src/iosMain/kotlin/tech/dokus/features/auth/DiModule.ios.kt | 11 | Source file | 5 |
| features/auth/data/src/jvmMain/kotlin/tech/dokus/features/auth/DiModule.jvm.kt | 11 | Source file | 5 |
| features/auth/data/src/wasmJsMain/kotlin/tech/dokus/features/auth/DiModule.wasmJs.kt | 11 | Source file | 5 |
| features/auth/domain/build.gradle.kts | 77 | Source file | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/gateway/AuthGateway.kt | 55 | Source file | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/gateway/TeamInvitationsGateway.kt | 16 | Source file | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/gateway/TeamMembersGateway.kt | 16 | Source file | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/gateway/TeamOwnershipGateway.kt | 10 | Source file | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/gateway/WorkspaceSettingsGateway.kt | 25 | Source file | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/AuthSessionUseCase.kt | 12 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/AuthUseCases.kt | 31 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/GetCurrentTenantIdUseCase.kt | 23 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/GetCurrentTenantUseCase.kt | 34 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/SelectTenantUseCase.kt | 30 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/ServerConnectionUseCases.kt | 23 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/TeamSettingsUseCases.kt | 57 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/TenantUseCases.kt | 47 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/UserUseCases.kt | 18 | Use case | 5 |
| features/auth/domain/src/commonMain/kotlin/tech/dokus/features/auth/usecases/WorkspaceSettingsUseCases.kt | 45 | Use case | 5 |
| features/auth/presentation/build.gradle.kts | 113 | Source file | 5 |
| features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthAdditionalScreenshotTest.kt | 294 | Source file | 5 |
| features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenScreenshotTest.kt | 268 | Source file | 5 |
| features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/AuthScreenshotTest.kt | 307 | Source file | 4 |
| features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/ScreenshotTestWrapper.kt | 40 | Source file | 5 |
| features/auth/presentation/src/androidUnitTest/kotlin/tech/dokus/features/auth/presentation/screenshot/ScreenshotViewport.kt | 60 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/AuthAppModule.kt | 58 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/AuthInitializer.kt | 38 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/di/AuthPresentationModule.kt | 73 | DI module | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ForgotPasswordContainer.kt | 85 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ForgotPasswordContract.kt | 82 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/LoginContainer.kt | 113 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/LoginContract.kt | 86 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/NewPasswordContainer.kt | 128 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/NewPasswordContract.kt | 90 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ProfileSettingsContainer.kt | 165 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ProfileSettingsContract.kt | 123 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/RegisterContainer.kt | 156 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/RegisterContract.kt | 101 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ServerConnectionContainer.kt | 269 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/ServerConnectionContract.kt | 135 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceCreateContainer.kt | 366 | MVI container/contract | 4 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceCreateContract.kt | 162 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceSelectContainer.kt | 106 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/mvi/WorkspaceSelectContract.kt | 84 | MVI container/contract | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/navigation/AuthNavigationProvider.kt | 49 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/CurrentServerSection.kt | 159 | UI component | 4 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/EntityConfirmationDialog.kt | 252 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/ProfileSettingsSections.kt | 331 | UI component | 4 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/ProtocolSelector.kt | 147 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/RegisterActionButton.kt | 44 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/RegisterCredentialsFields.kt | 62 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/RegisterProfileFields.kt | 58 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/ServerConfirmationDialog.kt | 172 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/WorkspaceCreate.kt | 235 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/WorkspaceSelection.kt | 92 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/steps/CompanyNameStep.kt | 90 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/steps/TypeSelectionStep.kt | 187 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/components/steps/VatAndAddressStep.kt | 206 | UI component | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/model/RegisterModels.kt | 137 | Model/DTO | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/model/WorkspaceWizardModels.kt | 107 | Model/DTO | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/ForgotPasswordRoute.kt | 29 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/LoginRoute.kt | 37 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/NewPasswordRoute.kt | 29 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/ProfileSettingsRoute.kt | 121 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/RegisterRoute.kt | 36 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/ServerConnectionRoute.kt | 58 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/WorkspaceCreateRoute.kt | 46 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/route/WorkspaceSelectRoute.kt | 47 | Source file | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/ForgotPasswordScreen.kt | 56 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/LoginScreen.kt | 248 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/NewPasswordScreen.kt | 70 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/ProfileScreen.kt | 24 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/ProfileSettingsScreen.kt | 156 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/RegisterConfirmationScreen.kt | 106 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/RegisterScreen.kt | 228 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/ServerConnectionScreen.kt | 355 | Compose screen | 4 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/SloganScreen.kt | 225 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/WorkspaceCreateScreen.kt | 295 | Compose screen | 5 |
| features/auth/presentation/src/commonMain/kotlin/tech/dokus/features/auth/presentation/auth/screen/WorkspaceSelectScreen.kt | 152 | Compose screen | 5 |
| features/cashflow/data/build.gradle.kts | 92 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSource.kt | 714 | Remote data source | 2 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/CashflowRemoteDataSourceImpl.kt | 906 | Remote data source | 2 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/ChatRemoteDataSource.kt | 126 | Remote data source | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/datasource/ChatRemoteDataSourceImpl.kt | 121 | Remote data source | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/di/CashflowDataModule.kt | 197 | DI module | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/DocumentReviewGatewayImpl.kt | 62 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/DocumentUploadGatewayImpl.kt | 26 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolConnectionGatewayImpl.kt | 14 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolInboxGatewayImpl.kt | 9 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolInvoiceGatewayImpl.kt | 20 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolRecipientGatewayImpl.kt | 11 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolTransmissionsGatewayImpl.kt | 21 | Source file | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/CashflowDocumentUseCaseImpls.kt | 75 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/CashflowEntriesUseCaseImpls.kt | 99 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/ChatUseCaseImpls.kt | 56 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/DocumentReviewUseCaseImpls.kt | 108 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/DocumentUploadUseCaseImpls.kt | 35 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/LoadDocumentRecordsUseCaseImpl.kt | 35 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/PeppolRegistrationUseCaseImpls.kt | 137 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/PeppolUseCaseImpls.kt | 100 | Use case | 5 |
| features/cashflow/data/src/commonMain/kotlin/tech/dokus/features/cashflow/usecase/SendChatMessageUseCaseImpl.kt | 100 | Use case | 5 |
| features/cashflow/data/src/commonTest/kotlin/tech/dokus/features/cashflow/usecase/PeppolUseCasesTest.kt | 293 | Use case | 5 |
| features/cashflow/data/src/jvmTest/kotlin/tech/dokus/features/cashflow/gateway/PeppolGatewayImplsTest.kt | 152 | Source file | 5 |
| features/cashflow/domain/build.gradle.kts | 65 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/DocumentReviewGateway.kt | 52 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/DocumentUploadGateway.kt | 19 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolConnectionGateway.kt | 14 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolInboxGateway.kt | 10 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolInvoiceGateway.kt | 19 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolRecipientGateway.kt | 10 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/gateway/PeppolTransmissionsGateway.kt | 17 | Source file | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/CashflowDocumentUseCases.kt | 26 | Use case | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/CashflowEntriesUseCases.kt | 80 | Use case | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/ChatUseCases.kt | 56 | Use case | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/DocumentReviewUseCases.kt | 83 | Use case | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/DocumentUploadUseCases.kt | 24 | Use case | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/LoadDocumentRecordsUseCase.kt | 19 | Use case | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/PeppolRegistrationUseCases.kt | 72 | Use case | 5 |
| features/cashflow/domain/src/commonMain/kotlin/tech/dokus/features/cashflow/usecases/PeppolUseCases.kt | 74 | Use case | 5 |
| features/cashflow/presentation/build.gradle.kts | 126 | Source file | 5 |
| features/cashflow/presentation/src/androidMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentDropTarget.android.kt | 14 | UI component | 5 |
| features/cashflow/presentation/src/androidUnitTest/kotlin/tech/dokus/features/cashflow/presentation/screenshot/CashflowAdditionalScreenshotTest.kt | 275 | Source file | 5 |
| features/cashflow/presentation/src/androidUnitTest/kotlin/tech/dokus/features/cashflow/presentation/screenshot/CashflowScreenshotTest.kt | 382 | Source file | 4 |
| features/cashflow/presentation/src/androidUnitTest/kotlin/tech/dokus/features/cashflow/presentation/screenshot/ScreenshotTestWrapper.kt | 40 | Source file | 5 |
| features/cashflow/presentation/src/androidUnitTest/kotlin/tech/dokus/features/cashflow/presentation/screenshot/ScreenshotViewport.kt | 60 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/CashflowAppModule.kt | 67 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/di/CashflowPresentationModule.kt | 107 | DI module | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/AddDocumentContainer.kt | 119 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/AddDocumentContract.kt | 84 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/CreateInvoiceContainer.kt | 570 | MVI container/contract | 3 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/CreateInvoiceContract.kt | 238 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/DeliveryMethodOption.kt | 135 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/mvi/model/CreateInvoiceModels.kt | 172 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/navigation/CashflowHomeNavigationProvider.kt | 23 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/navigation/CashflowNavigationProvider.kt | 49 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/AppDownloadQrDialog.kt | 120 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/CashflowCard.kt | 309 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/CashflowContentLayouts.kt | 200 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/CashflowDocumentSections.kt | 287 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/CashflowExtensions.kt | 118 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/CashflowFilters.kt | 75 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/CashflowHeader.kt | 145 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/CashflowSummarySection.kt | 94 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentDropTarget.kt | 37 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentUploadItem.kt | 315 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentUploadList.kt | 91 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentUploadSidebar.kt | 266 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentUploadZone.kt | 280 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/FilePicker.kt | 39 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/FinancialDocumentTable.kt | 530 | UI component | 3 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/FinancialDocumentTablePreview.kt | 186 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/InvoiceCard.kt | 98 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/InvoiceDetailsForm.kt | 227 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/PendingDocumentsCard.kt | 384 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/PendingDocumentsCardPreview.kt | 368 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/SortDropdown.kt | 134 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/SpaceUploadOverlay.kt | 111 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/VatSummaryCard.kt | 324 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/ExpandableLineItemRow.kt | 395 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InteractiveInvoiceDocument.kt | 114 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceClientSection.kt | 124 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceClientSelector.kt | 127 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceClientSidePanel.kt | 385 | UI component | 3 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceDatesSection.kt | 157 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceDocumentParts.kt | 186 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceFormCard.kt | 127 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceLayouts.kt | 332 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceLineItemCard.kt | 125 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceLineItemsSection.kt | 87 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceLineItemsTable.kt | 146 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceSendOptionsPanel.kt | 384 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceSendOptionsStep.kt | 396 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceSummaryCard.kt | 349 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/InvoiceVatRateSelector.kt | 86 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/invoice/Mocks.kt | 146 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/upload/UploadItemActions.kt | 144 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/upload/UploadItemHeader.kt | 250 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/upload/UploadItemProgress.kt | 88 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/upload/UploadOverlayContent.kt | 572 | UI component | 3 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/upload/UploadOverlayHeader.kt | 86 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/DocumentDeletionHandle.kt | 58 | Model/DTO | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/DocumentUploadDisplayState.kt | 102 | Model/DTO | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/DocumentUploadTask.kt | 98 | Model/DTO | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/manager/DocumentUploadManager.kt | 337 | Model/DTO | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/mapper/CreateInvoiceRequestMapper.kt | 41 | Model/DTO | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/state/DocumentUploadItemState.kt | 225 | Model/DTO | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/model/usecase/ValidateInvoiceUseCase.kt | 61 | Model/DTO | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/route/AddDocumentRoute.kt | 44 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/route/CreateInvoiceRoute.kt | 47 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/screen/AddDocumentScreen.kt | 296 | Compose screen | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/screen/CreateInvoiceScreen.kt | 243 | Compose screen | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/ChatContainer.kt | 493 | Source file | 3 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/ChatContract.kt | 317 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatContent.kt | 107 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatEmptyState.kt | 128 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatInputSection.kt | 58 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatMessages.kt | 104 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatScopeSelector.kt | 25 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatSendingIndicator.kt | 56 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatSessionPicker.kt | 147 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatStatus.kt | 60 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/components/ChatTopBar.kt | 149 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/route/ChatRoute.kt | 91 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/chat/screen/ChatScreen.kt | 71 | Compose screen | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/common/components/chips/DokusStatusChip.kt | 38 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/common/components/empty/DokusEmptyState.kt | 51 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/common/components/filters/DokusFilterChipRow.kt | 22 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/common/components/pagination/LoadMoreTrigger.kt | 25 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/common/components/table/DokusTableShared.kt | 73 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/common/utils/DateFormat.kt | 42 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/documents/components/DocumentRow.kt | 436 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/documents/components/DocumentStatusFilterChips.kt | 53 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/documents/mvi/DocumentsContainer.kt | 281 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/documents/mvi/DocumentsContract.kt | 111 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/documents/route/DocumentsRoute.kt | 105 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/documents/screen/DocumentsScreen.kt | 289 | Compose screen | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/components/CashflowDetailPane.kt | 661 | UI component | 2 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/components/CashflowLedgerOverview.kt | 180 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/components/CashflowLedgerRows.kt | 401 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/components/CashflowLedgerSkeleton.kt | 149 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/components/CashflowSummarySection.kt | 405 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/components/CashflowViewModeFilter.kt | 145 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/mvi/CashflowLedgerContainer.kt | 573 | MVI container/contract | 2 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/mvi/CashflowLedgerContract.kt | 185 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/route/CashflowLedgerRoute.kt | 81 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/ledger/screen/CashflowLedgerScreen.kt | 370 | Compose screen | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/model/DocumentUiStatusMapper.kt | 73 | Model/DTO | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/mvi/PeppolRegistrationContainer.kt | 356 | MVI container/contract | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/mvi/PeppolRegistrationContract.kt | 108 | MVI container/contract | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/route/PeppolRegistrationRoute.kt | 72 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/screen/PeppolRegistrationScreen.kt | 366 | Compose screen | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/peppol/screen/PeppolRegistrationSharedUi.kt | 246 | Compose screen | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/AuthenticatedImageLoader.kt | 54 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/ContactSelectionSection.kt | 147 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentPreviewState.kt | 52 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewAction.kt | 26 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewActions.kt | 318 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewContactBinder.kt | 231 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewContainer.kt | 155 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewExtractedDataMapper.kt | 144 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewFieldEditor.kt | 260 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewFooter.kt | 321 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewIntent.kt | 204 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewLineItems.kt | 68 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewLoader.kt | 190 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewModels.kt | 378 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewPreview.kt | 107 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewProvenance.kt | 11 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewReducer.kt | 173 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/DocumentReviewState.kt | 315 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/PdfPreviewBottomSheet.kt | 128 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/PdfPreviewPane.kt | 318 | Source file | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/PdfThumbnail.kt | 206 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/PreviewConfig.kt | 22 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/ZoomControls.kt | 100 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/AnalysisFailedBanner.kt | 104 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/ContactEditSheet.kt | 707 | UI component | 2 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/RejectDocumentDialog.kt | 139 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/ReviewContent.kt | 401 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/ReviewTopBar.kt | 224 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/details/FactComponents.kt | 385 | UI component | 4 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/details/ReviewAmountsCard.kt | 165 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/details/ReviewDetailsCards.kt | 252 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/forms/BillForm.kt | 184 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/forms/ExpenseForm.kt | 185 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/forms/InvoiceForm.kt | 169 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/forms/ReviewFormCommon.kt | 174 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/mobile/DocumentDetailMobileHeader.kt | 130 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/mobile/DocumentDetailTabBar.kt | 56 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/mobile/MobileFooter.kt | 96 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/components/mobile/MobileTabContent.kt | 267 | UI component | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/models/CounterpartyInfo.kt | 7 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/models/CounterpartyInfoMapper.kt | 31 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/route/DocumentReviewRoute.kt | 235 | Source file | 5 |
| features/cashflow/presentation/src/commonMain/kotlin/tech/dokus/features/cashflow/presentation/review/screen/DocumentReviewScreen.kt | 52 | Compose screen | 5 |
| features/cashflow/presentation/src/commonTest/kotlin/tech/dokus/features/cashflow/presentation/model/DocumentUiStatusMapperTest.kt | 321 | Model/DTO | 4 |
| features/cashflow/presentation/src/desktopMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentDropTarget.desktop.kt | 178 | UI component | 5 |
| features/cashflow/presentation/src/iosMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentDropTarget.ios.kt | 14 | UI component | 5 |
| features/cashflow/presentation/src/wasmJsMain/kotlin/tech/dokus/features/cashflow/presentation/cashflow/components/DocumentDropTarget.wasmJs.kt | 14 | UI component | 5 |
| features/contacts/data/build.gradle.kts | 89 | Source file | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/DiModule.kt | 99 | Source file | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/datasource/ContactCacheDataSourceImpl.kt | 21 | Remote data source | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/datasource/ContactLocalDataSource.kt | 63 | Remote data source | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/datasource/ContactLocalDataSourceImpl.kt | 127 | Remote data source | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/datasource/ContactsDb.kt | 19 | Remote data source | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/initializer/ContactsDataInitializer.kt | 14 | Source file | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/repository/ContactRemoteDataSource.kt | 90 | Repository | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/repository/ContactRemoteDataSourceImpl.kt | 286 | Repository | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactActivityUseCasesImpl.kt | 25 | Use case | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactCacheUseCasesImpl.kt | 21 | Use case | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactCrudUseCasesImpl.kt | 42 | Use case | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactNotesUseCasesImpl.kt | 54 | Use case | 5 |
| features/contacts/data/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ListContactsUseCaseImpl.kt | 91 | Use case | 5 |
| features/contacts/domain/build.gradle.kts | 64 | Source file | 5 |
| features/contacts/domain/src/commonMain/kotlin/tech/dokus/features/contacts/datasource/ContactCacheDataSource.kt | 13 | Remote data source | 5 |
| features/contacts/domain/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactActivityUseCases.kt | 22 | Use case | 5 |
| features/contacts/domain/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactCacheUseCases.kt | 18 | Use case | 5 |
| features/contacts/domain/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactCrudUseCases.kt | 37 | Use case | 5 |
| features/contacts/domain/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ContactNotesUseCases.kt | 49 | Use case | 5 |
| features/contacts/domain/src/commonMain/kotlin/tech/dokus/features/contacts/usecases/ListContactsUseCase.kt | 59 | Use case | 5 |
| features/contacts/presentation/build.gradle.kts | 112 | Source file | 5 |
| features/contacts/presentation/src/androidUnitTest/kotlin/tech/dokus/features/contacts/presentation/screenshot/ContactsAdditionalScreenshotTest.kt | 83 | Source file | 5 |
| features/contacts/presentation/src/androidUnitTest/kotlin/tech/dokus/features/contacts/presentation/screenshot/ContactsScreenshotTest.kt | 446 | Source file | 4 |
| features/contacts/presentation/src/androidUnitTest/kotlin/tech/dokus/features/contacts/presentation/screenshot/ScreenshotTestWrapper.kt | 40 | Source file | 5 |
| features/contacts/presentation/src/androidUnitTest/kotlin/tech/dokus/features/contacts/presentation/screenshot/ScreenshotViewport.kt | 60 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/ContactsAppModule.kt | 57 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/di/ContactsPresentationModule.kt | 84 | DI module | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactDetailsContainer.kt | 656 | MVI container/contract | 2 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactDetailsContract.kt | 244 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactFormContainer.kt | 707 | MVI container/contract | 1 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactFormContract.kt | 364 | MVI container/contract | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactMergeAction.kt | 9 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactMergeConflictCalculator.kt | 110 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactMergeContainer.kt | 231 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactMergeIntent.kt | 16 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactMergeState.kt | 24 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactsContainer.kt | 545 | MVI container/contract | 3 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/ContactsContract.kt | 205 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/CreateContactContainer.kt | 547 | MVI container/contract | 3 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/mvi/CreateContactContract.kt | 252 | MVI container/contract | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/navigation/ContactsHomeNavigationProvider.kt | 20 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/navigation/ContactsNavigationProvider.kt | 41 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ActivitySummarySection.kt | 263 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactAutocomplete.kt | 725 | UI component | 2 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactCard.kt | 177 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactDetailsContent.kt | 91 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactDetailsFormat.kt | 13 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactDetailsTopBar.kt | 134 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactFormContent.kt | 434 | UI component | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactFormFields.kt | 424 | UI component | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactFormPane.kt | 312 | UI component | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactInfoContent.kt | 212 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactInfoElements.kt | 194 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactInfoSection.kt | 63 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactsFilters.kt | 132 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactsHeader.kt | 122 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/ContactsList.kt | 377 | UI component | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/DuplicateWarningBanner.kt | 247 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/EnrichmentSuggestionsDialog.kt | 284 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/NotesBottomSheet.kt | 642 | UI component | 2 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/NotesSection.kt | 276 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/create/ConfirmStepContent.kt | 279 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/create/DuplicateVatBanner.kt | 77 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/create/LookupStepContent.kt | 618 | UI component | 2 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/create/ManualStepContent.kt | 321 | UI component | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/create/SoftDuplicateDialog.kt | 133 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/create/StepIndicator.kt | 190 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeCompareFieldsStep.kt | 167 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeConfirmationStep.kt | 151 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeConflictRow.kt | 77 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeDialog.kt | 135 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeDialogRoute.kt | 42 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeFormatting.kt | 27 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeMiniCard.kt | 104 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeResultStep.kt | 152 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/components/merge/ContactMergeSelectTargetStep.kt | 132 | UI component | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/model/ContactMergeModels.kt | 18 | Model/DTO | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/route/ContactDetailsRoute.kt | 134 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/route/ContactFormRoute.kt | 91 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/route/ContactsRoute.kt | 102 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/route/CreateContactRoute.kt | 84 | Source file | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/screen/ContactDetailsScreen.kt | 126 | Compose screen | 5 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/screen/ContactFormScreen.kt | 346 | Compose screen | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/screen/ContactsScreen.kt | 376 | Compose screen | 4 |
| features/contacts/presentation/src/commonMain/kotlin/tech/dokus/features/contacts/presentation/contacts/screen/CreateContactScreen.kt | 227 | Compose screen | 5 |
| foundation/app-common/build.gradle.kts | 158 | Source file | 5 |
| foundation/app-common/src/androidMain/kotlin/tech/dokus/foundation/app/cache/AttachmentCache.android.kt | 65 | Source file | 5 |
| foundation/app-common/src/androidMain/kotlin/tech/dokus/foundation/app/database/SqlDriverFactory.android.kt | 24 | Source file | 5 |
| foundation/app-common/src/androidMain/kotlin/tech/dokus/foundation/app/network/HttpClient.android.kt | 9 | Source file | 5 |
| foundation/app-common/src/androidMain/kotlin/tech/dokus/foundation/app/network/NetworkExceptionHelper.android.kt | 28 | Source file | 5 |
| foundation/app-common/src/androidMain/kotlin/tech/dokus/foundation/app/picker/FilePicker.android.kt | 90 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/AppDataInitializer.kt | 22 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/AppDataModule.kt | 20 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/AppDomainModule.kt | 17 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/AppModule.kt | 53 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/AppPresentationModule.kt | 35 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/DashboardWidget.kt | 62 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/SharedQualifiers.kt | 9 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/cache/AttachmentCache.kt | 159 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/database/DatabaseWrapper.kt | 91 | Source file | 4 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/database/LocalDatabaseCleaner.kt | 14 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/database/SqlDriverFactory.kt | 11 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/extensions/ValidatableExtensions.kt | 13 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/local/LocalAppModules.kt | 6 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/mvi/ContainerViewModel.kt | 97 | MVI container/contract | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/ConnectionSnackbarEffect.kt | 77 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/HttpClient.kt | 58 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/HttpClientExtensions.kt | 312 | Source file | 4 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/NetworkExceptionHelper.kt | 43 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/OfflineDisabled.kt | 46 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/ServerConnectionMonitor.kt | 75 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/network/ServerConnectionState.kt | 100 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/picker/FilePicker.kt | 100 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/CacheState.kt | 153 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/DokusState.kt | 50 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/DokusStateExtension.kt | 41 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/DokusStateFlowExtension.kt | 20 | Source file | 5 |
| foundation/app-common/src/commonMain/kotlin/tech/dokus/foundation/app/state/DokusStateSimple.kt | 29 | Source file | 5 |
| foundation/app-common/src/desktopMain/kotlin/tech/dokus/foundation/app/cache/AttachmentCache.jvm.kt | 60 | Source file | 5 |
| foundation/app-common/src/desktopMain/kotlin/tech/dokus/foundation/app/database/SqlDriverFactory.jvm.kt | 28 | Source file | 5 |
| foundation/app-common/src/desktopMain/kotlin/tech/dokus/foundation/app/network/HttpClient.desktop.kt | 9 | Source file | 5 |
| foundation/app-common/src/desktopMain/kotlin/tech/dokus/foundation/app/network/NetworkExceptionHelper.desktop.kt | 28 | Source file | 5 |
| foundation/app-common/src/desktopMain/kotlin/tech/dokus/foundation/app/picker/FilePicker.desktop.kt | 114 | Source file | 5 |
| foundation/app-common/src/iosMain/kotlin/tech/dokus/foundation/app/cache/AttachmentCache.ios.kt | 104 | Source file | 5 |
| foundation/app-common/src/iosMain/kotlin/tech/dokus/foundation/app/database/SqlDriverFactory.ios.kt | 31 | Source file | 5 |
| foundation/app-common/src/iosMain/kotlin/tech/dokus/foundation/app/network/HttpClient.ios.kt | 9 | Source file | 5 |
| foundation/app-common/src/iosMain/kotlin/tech/dokus/foundation/app/network/NetworkExceptionHelper.ios.kt | 35 | Source file | 5 |
| foundation/app-common/src/iosMain/kotlin/tech/dokus/foundation/app/picker/FilePicker.ios.kt | 90 | Source file | 5 |
| foundation/app-common/src/wasmJsMain/kotlin/tech/dokus/foundation/app/cache/AttachmentCache.wasmJs.kt | 39 | Source file | 5 |
| foundation/app-common/src/wasmJsMain/kotlin/tech/dokus/foundation/app/database/SqlDriverFactory.wasmJs.kt | 18 | Source file | 5 |
| foundation/app-common/src/wasmJsMain/kotlin/tech/dokus/foundation/app/network/HttpClient.wasmJs.kt | 6 | Source file | 5 |
| foundation/app-common/src/wasmJsMain/kotlin/tech/dokus/foundation/app/network/NetworkExceptionHelper.wasmJs.kt | 49 | Source file | 5 |
| foundation/app-common/src/wasmJsMain/kotlin/tech/dokus/foundation/app/picker/FilePicker.wasmJs.kt | 68 | Source file | 5 |
| foundation/aura/build.gradle.kts | 112 | Source file | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/BaseScreenshotTest.kt | 65 | Source file | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/ScreenshotTestWrapper.kt | 41 | Source file | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/ScreenshotViewport.kt | 60 | Source file | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/BackgroundScreenshotTest.kt | 64 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/BadgeScreenshotTest.kt | 143 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/ButtonScreenshotTest.kt | 131 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/CardScreenshotTest.kt | 188 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/ChatScreenshotTest.kt | 183 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/CommonScreenshotTest.kt | 255 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/DatePickerScreenshotTest.kt | 152 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/DialogScreenshotTest.kt | 125 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/DividerScreenshotTest.kt | 51 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/DropdownScreenshotTest.kt | 178 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/FieldVariantsScreenshotTest.kt | 143 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/FilterToggleScreenshotTest.kt | 137 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/LayoutScreenshotTest.kt | 200 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/NavigationScreenshotTest.kt | 341 | UI component | 4 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/SettingsItemScreenshotTest.kt | 54 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/TextFieldScreenshotTest.kt | 209 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/TextScreenshotTest.kt | 83 | UI component | 5 |
| foundation/aura/src/androidUnitTest/kotlin/tech/dokus/foundation/aura/screenshot/components/TileScreenshotTest.kt | 89 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/Button.kt | 238 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/Card.kt | 189 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/CashflowStatusBadge.kt | 57 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/CashflowTypeBadge.kt | 69 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/CompanyAvatarImage.kt | 189 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/DatePicker.kt | 70 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/Divider.kt | 47 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/DocumentStatusBadge.kt | 57 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/DraftStatusBadge.kt | 34 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/Icon.kt | 28 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/ImageCropperDialog.kt | 260 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/ListSettingsItem.kt | 69 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/StatusBadge.kt | 65 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/Text.kt | 39 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/background/AnimatedBackground.kt | 570 | UI component | 3 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/chat/ChatInputField.kt | 168 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/chat/ChatMessageBubble.kt | 172 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/chat/ChatSourceCitation.kt | 412 | UI component | 4 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/chips/PChoiceChips.kt | 70 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/AnimatedCheck.kt | 134 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/DokusSelectableRow.kt | 202 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/ErrorBox.kt | 197 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/OfflineOverlay.kt | 91 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/PCopyRow.kt | 90 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/SearchActionTopAppBar.kt | 51 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/SearchFieldCompact.kt | 103 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/Shimmer.kt | 132 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/TopAppBar.kt | 88 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/TopAppBarSearchAction.kt | 49 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/common/WaitingIndicator.kt | 82 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/dialog/DokusDialog.kt | 287 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/dropdown/FilterOption.kt | 26 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/dropdown/PFilterDropdown.kt | 212 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/PDateField.kt | 98 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/PDropdownField.kt | 104 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextField.kt | 218 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldEmail.kt | 58 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldFree.kt | 55 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldName.kt | 61 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldPassword.kt | 61 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldPhone.kt | 56 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldStandard.kt | 55 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldTaxNumber.kt | 57 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/fields/TextFieldWorkspaceName.kt | 59 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/filter/DokusFilterToggle.kt | 89 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/icons/LockIcon.kt | 36 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/layout/DokusExpandableAction.kt | 88 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/layout/DokusTabbedPanel.kt | 205 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/layout/DokusTableLayout.kt | 86 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/layout/PCollapsibleSection.kt | 76 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/layout/TwoPaneContainer.kt | 45 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/navigation/DokusNavigationBar.kt | 116 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/navigation/DokusNavigationRail.kt | 97 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/navigation/DokusNavigationRailSectioned.kt | 284 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/navigation/NavigationBar.kt | 197 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/navigation/NavigationRail.kt | 132 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/navigation/SelectableCard.kt | 127 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/navigation/UserPreferencesMenu.kt | 81 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/settings/DataRow.kt | 215 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/settings/SettingsSection.kt | 214 | UI component | 4 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/status/StatusDot.kt | 92 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/text/AppNameText.kt | 61 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/text/CopyRightText.kt | 21 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/text/SectionTitle.kt | 36 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/components/tiles/CompanyTile.kt | 100 | UI component | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/constrains/Constrains.kt | 221 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/CashflowEntryStatusExtensions.kt | 48 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/ClientTypeExtensions.kt | 16 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/DocumentUiStatusExtensions.kt | 86 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/DokusExceptionExtensions.kt | 274 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/DraftStatusExtensions.kt | 105 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/ExpenseCategoryExtensions.kt | 36 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/InvoiceStatusExtensions.kt | 124 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/MediaDocumentTypeExtensions.kt | 85 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/PaymentMethodExtensions.kt | 26 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/extensions/PeppolProviderExtensions.kt | 30 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/local/LocalReduceMotion.kt | 12 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/local/LocalSizes.kt | 65 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/local/LocalTheme.kt | 37 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/model/DocumentUiStatus.kt | 43 | Model/DTO | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/model/HomeItem.kt | 21 | Model/DTO | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/model/NavStructure.kt | 50 | Model/DTO | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/style/ColorScheme.kt | 199 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/style/FontScheme.kt | 189 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/style/ThemeManager.kt | 88 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/style/ThemeMode.kt | 40 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/style/Themed.kt | 79 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/tooling/Mocks.kt | 35 | Source file | 5 |
| foundation/aura/src/commonMain/kotlin/tech/dokus/foundation/aura/tooling/TestWrapper.kt | 68 | Source file | 5 |
| foundation/backend-common/build.gradle.kts | 71 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/CacheKeyBuilder.kt | 130 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/CacheSerializer.kt | 76 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisClient.kt | 140 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisClientFactory.kt | 16 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisClientImpl.kt | 186 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisDelegate.kt | 127 | Source file | 4 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisDsl.kt | 217 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisExtensions.kt | 286 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisModule.kt | 13 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/cache/RedisNamespace.kt | 17 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/AIConfig.kt | 24 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/AppBaseConfig.kt | 52 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/AuthConfig.kt | 66 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/CachingConfig.kt | 62 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/DatabaseConfig.kt | 40 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/FlywayConfig.kt | 23 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/IntelligenceMode.kt | 228 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/JwtConfig.kt | 27 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/KtorConfig.kt | 26 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/LoggingConfig.kt | 17 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/MetricsConfig.kt | 17 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/MinioConfig.kt | 36 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/ProcessorConfig.kt | 21 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/SecurityConfig.kt | 39 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/ServerInfoConfig.kt | 29 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/config/StorageConfig.kt | 27 | Config | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/configure/ErrorHandling.kt | 80 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/configure/JwtAuthentication.kt | 75 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/configure/Monitoring.kt | 51 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/configure/Security.kt | 93 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/configure/Serialization.kt | 13 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/crypto/CredentialCryptoService.kt | 102 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/crypto/PasswordCryptoService.kt | 9 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/crypto/PasswordCryptoService4j.kt | 21 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/database/DatabaseFactory.kt | 126 | Source file | 4 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/database/DateTimeUtils.kt | 16 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/database/DbEnum.kt | 15 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/database/TenantContextHolder.kt | 31 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/lookup/CbeApiClient.kt | 160 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/middleware/RateLimitPlugin.kt | 26 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/routes/HealthRoutes.kt | 208 | HTTP routes | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/routes/ServerInfoRoutes.kt | 91 | HTTP routes | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/security/Authentication.kt | 174 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/security/DokusPrincipal.kt | 61 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/security/JwtGenerator.kt | 97 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/security/JwtValidator.kt | 118 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/security/TokenBlacklistService.kt | 140 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/storage/AvatarStorageService.kt | 183 | Source file | 4 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/storage/DocumentStorageService.kt | 135 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/storage/DocumentUploadValidator.kt | 63 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/storage/MinioStorage.kt | 213 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/storage/ObjectStorage.kt | 63 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/utils/LoggingUtils.kt | 21 | Source file | 5 |
| foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/utils/RequestUtils.kt | 33 | Source file | 5 |
| foundation/database/build.gradle.kts | 48 | Source file | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/DokusSchema.kt | 109 | Source file | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/columns/VectorColumnType.kt | 113 | Source file | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/di/RepositoryModules.kt | 151 | DI module | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/entity/IngestionItemEntity.kt | 18 | Source file | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/mapper/TenantMappers.kt | 75 | Source file | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/mapper/UserMappers.kt | 55 | Source file | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/ai/ChatRepositoryImpl.kt | 422 | Repository | 3 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/ai/DocumentChunksRepository.kt | 348 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/ai/IngestionStatusCheckerImpl.kt | 22 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/auth/AddressRepository.kt | 199 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/auth/InvitationRepository.kt | 253 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/auth/PasswordResetTokenRepository.kt | 166 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/auth/RefreshTokenRepository.kt | 380 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/auth/TenantRepository.kt | 221 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/auth/UserRepository.kt | 418 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/banking/BankingRepository.kt | 326 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/BillRepository.kt | 540 | Repository | 3 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/CashflowEntriesRepository.kt | 334 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/CashflowRepository.kt | 55 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/CreditNoteRepository.kt | 259 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/DocumentDraftRepository.kt | 430 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/DocumentIngestionRunRepository.kt | 323 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/DocumentRepository.kt | 405 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/ExpenseRepository.kt | 355 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/InvoiceNumberRepository.kt | 244 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/InvoiceRepository.kt | 682 | Repository | 2 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/cashflow/RefundClaimRepository.kt | 233 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/contacts/ContactAddressRepository.kt | 460 | Repository | 3 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/contacts/ContactNoteRepository.kt | 225 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/contacts/ContactRepository.kt | 694 | Repository | 2 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/documents/DocumentLineItemRepository.kt | 256 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/documents/DocumentLinkRepository.kt | 227 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/payment/PaymentRepository.kt | 190 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/peppol/PeppolDirectoryCacheRepository.kt | 229 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/peppol/PeppolRegistrationRepository.kt | 183 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/peppol/PeppolSettingsRepository.kt | 157 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/peppol/PeppolTransmissionRepository.kt | 237 | Repository | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/repository/processor/ProcessorIngestionRepository.kt | 354 | Repository | 4 |
| foundation/database/src/main/kotlin/tech/dokus/database/services/InvoiceNumberGenerator.kt | 238 | Source file | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/ai/ChatMessagesTable.kt | 106 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/ai/DocumentChunksTable.kt | 103 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/AddressTable.kt | 35 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/PasswordResetTokensTable.kt | 28 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/RefreshTokensTable.kt | 24 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/TenantInvitationsTable.kt | 44 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/TenantMembersTable.kt | 30 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/TenantSettingsTable.kt | 55 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/TenantTable.kt | 39 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/auth/UsersTable.kt | 35 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/banking/BankConnectionsTable.kt | 45 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/banking/BankTransactionsTable.kt | 54 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/BillsTable.kt | 89 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/CashflowEntriesTable.kt | 86 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/CreditNotesTable.kt | 87 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/ExpensesTable.kt | 74 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/InvoiceItemsTable.kt | 36 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/InvoiceNumberSequencesTable.kt | 42 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/InvoicesTable.kt | 83 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/cashflow/RefundClaimsTable.kt | 78 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/contacts/ContactAddressesTable.kt | 47 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/contacts/ContactNotesTable.kt | 50 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/contacts/ContactsTable.kt | 70 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/documents/DocumentDraftsTable.kt | 145 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/documents/DocumentIngestionRunsTable.kt | 98 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/documents/DocumentLineItemsTable.kt | 66 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/documents/DocumentLinksTable.kt | 60 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/documents/DocumentsTable.kt | 59 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/payment/PaymentsTable.kt | 41 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/peppol/PeppolDirectoryCacheTable.kt | 72 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/peppol/PeppolRegistrationTable.kt | 52 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/peppol/PeppolSettingsTable.kt | 50 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/tables/peppol/PeppolTransmissionsTable.kt | 68 | DB table schema | 5 |
| foundation/database/src/main/kotlin/tech/dokus/database/utils/DateTimeExtensions.kt | 23 | Source file | 5 |
| foundation/database/src/test/kotlin/tech/dokus/database/services/InvoiceNumberGeneratorTest.kt | 399 | Source file | 4 |
| foundation/domain/build.gradle.kts | 89 | Source file | 5 |
| foundation/domain/src/androidMain/kotlin/tech/dokus/domain/DeviceType.android.kt | 4 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/DeviceType.kt | 47 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/Health.kt | 80 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/Money.kt | 457 | Source file | 3 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/Validation.kt | 133 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/asbtractions/AuthManager.kt | 9 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/asbtractions/RetryHandler.kt | 5 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/asbtractions/TokenManager.kt | 12 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/config/AppVersion.kt | 17 | Config | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/config/DynamicDokusEndpointProvider.kt | 79 | Config | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/config/ServerConfig.kt | 145 | Config | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/config/ServerConfigManager.kt | 61 | Config | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/config/ServerInfo.kt | 64 | Config | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/config/ServerValidationResult.kt | 93 | Config | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/database/DbEnum.kt | 5 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/enums/ContactEnums.kt | 28 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/enums/DocumentSource.kt | 32 | Source file | 4 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/enums/FinancialEnums.kt | 1177 | Source file | 2 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/enums/MediaEnums.kt | 35 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/enums/PeppolEnums.kt | 68 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/enums/ProcessingEnums.kt | 187 | Source file | 4 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/exceptions/DokusException.kt | 1013 | Source file | 2 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/exceptions/DokusExceptionExtensions.kt | 39 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/flags/FeatureFlagService.kt | 13 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/flags/FeatureFlags.kt | 6 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Address.kt | 20 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Banking.kt | 69 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Compliance.kt | 152 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Contacts.kt | 42 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Document.kt | 18 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/DocumentProcessing.kt | 21 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Identity.kt | 82 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/IngestionRunId.kt | 21 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Invoicing.kt | 106 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/Payments.kt | 42 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/ids/System.kt | 30 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/Cashflow.kt | 109 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/CompanyAvatar.kt | 15 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/DocumentChunk.kt | 262 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/DocumentDto.kt | 28 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/DocumentPageDto.kt | 47 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/DocumentRecordDto.kt | 159 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/ExtractedData.kt | 364 | Model/DTO | 4 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/Financial.kt | 535 | Model/DTO | 3 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/FinancialDocument.kt | 247 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/Peppol.kt | 283 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/PeppolActivity.kt | 19 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/PeppolRegistration.kt | 78 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/TenantAddress.kt | 13 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/ai/AiProvider.kt | 23 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/ai/ChatMessage.kt | 248 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/ai/ChatModels.kt | 414 | Model/DTO | 4 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/auth/AuthEvent.kt | 15 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/auth/AuthenticationInfo.kt | 24 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/auth/Identity.kt | 78 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/auth/JwtModels.kt | 68 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/auth/Location.kt | 14 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/auth/QrLoginInStatus.kt | 21 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/auth/SessionDto.kt | 67 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/common/DateRange.kt | 10 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/common/DeepLinks.kt | 107 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/common/Feature.kt | 63 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/common/PaginatedResponse.kt | 15 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/common/PaginationState.kt | 14 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/common/PaginationStateExtensions.kt | 79 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/common/Thumbnail.kt | 10 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/contact/Contact.kt | 337 | Model/DTO | 4 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/model/entity/EntityLookup.kt | 59 | Model/DTO | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/repository/AIRepositories.kt | 291 | Repository | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/repository/DraftStatusChecker.kt | 23 | Repository | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/repository/IngestionStatusChecker.kt | 24 | Repository | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/AccountRoutes.kt | 63 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/AttachmentRoutes.kt | 34 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/BillRoutes.kt | 61 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/CashflowRoutes.kt | 89 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/ChatRoutes.kt | 72 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/ContactRoutes.kt | 113 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/DocumentRoutes.kt | 152 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/ExpenseRoutes.kt | 43 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/IdentityRoutes.kt | 67 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/InvoiceRoutes.kt | 78 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/LookupRoutes.kt | 24 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/PaymentRoutes.kt | 63 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/PeppolRoutes.kt | 167 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/ReportRoutes.kt | 72 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/TeamRoutes.kt | 70 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/TenantRoutes.kt | 58 | HTTP routes | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/usecases/SearchCompanyUseCase.kt | 23 | Use case | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/utils/Base64.kt | 28 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/utils/DateTimeUtils.kt | 29 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/utils/Json.kt | 15 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateBicUseCase.kt | 38 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateCityUseCase.kt | 11 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateEmailUseCase.kt | 16 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateIbanUseCase.kt | 85 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateInvoiceNumberUseCase.kt | 21 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateLegalNameUseCase.kt | 11 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateMoneyUseCase.kt | 18 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateNameUseCase.kt | 11 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateNotShortUseCase.kt | 11 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateOgmUseCase.kt | 272 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidatePasswordUseCase.kt | 224 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidatePeppolIdUseCase.kt | 42 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidatePercentageUseCase.kt | 16 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidatePhoneNumberUseCase.kt | 21 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidatePostalCodeUseCase.kt | 34 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateQuantityUseCase.kt | 16 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateVatNumberUseCase.kt | 342 | Source file | 4 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateVatRateUseCase.kt | 17 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidateWorkspaceNameUseCase.kt | 11 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/Validator.kt | 5 | Source file | 5 |
| foundation/domain/src/commonMain/kotlin/tech/dokus/domain/validators/ValidatorThrowable.kt | 8 | Source file | 5 |
| foundation/domain/src/commonTest/kotlin/tech/dokus/domain/MoneyVatTest.kt | 23 | Source file | 5 |
| foundation/domain/src/commonTest/kotlin/tech/dokus/domain/ids/PostalCodeTest.kt | 291 | Source file | 5 |
| foundation/domain/src/commonTest/kotlin/tech/dokus/domain/ids/VatNumberTest.kt | 19 | Source file | 5 |
| foundation/domain/src/commonTest/kotlin/tech/dokus/domain/validators/ValidateOgmUseCaseTest.kt | 193 | Source file | 5 |
| foundation/domain/src/commonTest/kotlin/tech/dokus/domain/validators/ValidatePostalCodeUseCaseTest.kt | 308 | Source file | 4 |
| foundation/domain/src/iosMain/kotlin/tech/dokus/domain/DeviceType.ios.kt | 4 | Source file | 5 |
| foundation/domain/src/jvmMain/kotlin/tech/dokus/domain/DeviceType.jvm.kt | 4 | Source file | 5 |
| foundation/domain/src/jvmMain/kotlin/tech/dokus/domain/MoneyJvm.kt | 78 | Source file | 5 |
| foundation/domain/src/wasmJsMain/kotlin/tech/dokus/domain/DeviceType.wasmJs.kt | 4 | Source file | 5 |
| foundation/navigation/build.gradle.kts | 123 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/NavigationExtensions.kt | 75 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/NavigationProvider.kt | 7 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/animation/TabTransitionsProvider.kt | 28 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/animation/TransitionsProvider.kt | 27 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/animation/TransitionsProviderLargeScreen.kt | 35 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/animation/TransitionsProviderSmallScreen.kt | 53 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/data/NavigationPrefsRepository.kt | 41 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/AppDestination.kt | 18 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/AuthDestination.kt | 82 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/CashFlowDestination.kt | 39 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/ContactsDestination.kt | 37 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/CoreDestination.kt | 18 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/HomeDestination.kt | 47 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/NavigationDestination.kt | 6 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/SettingsDestination.kt | 26 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/local/LocalNavController.kt | 15 | Source file | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/mvi/NavigationContainer.kt | 81 | MVI container/contract | 5 |
| foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/mvi/NavigationContract.kt | 59 | MVI container/contract | 5 |
| foundation/peppol/build.gradle.kts | 42 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/config/PeppolModuleConfig.kt | 84 | Config | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/config/PeppolProviderConfig.kt | 23 | Config | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/mapper/PeppolMapper.kt | 280 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/model/PeppolModels.kt | 213 | Model/DTO | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/policy/DocumentConfirmationPolicy.kt | 52 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/PeppolCredentials.kt | 18 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/PeppolProvider.kt | 98 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/PeppolProviderFactory.kt | 65 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/RecommandCompaniesClient.kt | 98 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/RecommandCredentials.kt | 27 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/RecommandMapper.kt | 469 | Source file | 3 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/RecommandProvider.kt | 381 | Source file | 4 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiAuthModels.kt | 15 | Model/DTO | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiCommonModels.kt | 50 | Model/DTO | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiCompanyManagementModels.kt | 536 | Model/DTO | 3 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiDocumentSchemas.kt | 714 | Model/DTO | 2 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiDocumentsModels.kt | 483 | Model/DTO | 3 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiPlaygroundsModels.kt | 49 | Model/DTO | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiRecipientsModels.kt | 91 | Model/DTO | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiSendingModels.kt | 50 | Model/DTO | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiSupportingDataModels.kt | 355 | Model/DTO | 4 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/provider/client/recommand/model/RecommandApiWebhooksModels.kt | 117 | Model/DTO | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolConnectionService.kt | 143 | Service layer | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolCredentialResolver.kt | 52 | Service layer | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolRegistrationService.kt | 433 | Service layer | 3 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolService.kt | 482 | Service layer | 3 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolTransferPollingService.kt | 93 | Service layer | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/service/PeppolVerificationService.kt | 75 | Service layer | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/util/CompanyAddressParser.kt | 60 | Source file | 5 |
| foundation/peppol/src/main/kotlin/tech/dokus/peppol/validator/PeppolValidator.kt | 266 | Source file | 5 |
| foundation/platform/build.gradle.kts | 65 | Source file | 5 |
| foundation/platform/src/androidMain/kotlin/tech/dokus/foundation/platform/ActivePlatform.android.kt | 3 | Source file | 5 |
| foundation/platform/src/commonMain/kotlin/tech/dokus/foundation/platform/ActivePlatform.kt | 14 | Source file | 5 |
| foundation/platform/src/commonMain/kotlin/tech/dokus/foundation/platform/DiModule.kt | 22 | Source file | 5 |
| foundation/platform/src/commonMain/kotlin/tech/dokus/foundation/platform/Logger.kt | 82 | Source file | 5 |
| foundation/platform/src/commonMain/kotlin/tech/dokus/foundation/platform/Persistence.kt | 56 | Source file | 5 |
| foundation/platform/src/iosMain/kotlin/tech/dokus/foundation/platform/ActivePlatform.ios.kt | 3 | Source file | 5 |
| foundation/platform/src/jvmMain/java/tech/dokus/foundation/platform/ActivePlatform.jvm.kt | 3 | Source file | 5 |
| foundation/platform/src/wasmJsMain/kotlin/tech/dokus/foundation/platform/ActivePlatform.wasmJs.kt | 3 | Source file | 5 |
| foundation/sstorage/build.gradle.kts | 83 | Source file | 5 |
| foundation/sstorage/src/androidMain/kotlin/tech/dokus/foundation/sstorage/SecureStorage.android.kt | 143 | Source file | 5 |
| foundation/sstorage/src/androidUnitTest/kotlin/tech/dokus/foundation/sstorage/AndroidSecureStorageTest.kt | 14 | Source file | 5 |
| foundation/sstorage/src/commonMain/kotlin/tech/dokus/foundation/sstorage/SecureStorage.kt | 82 | Source file | 5 |
| foundation/sstorage/src/iosMain/kotlin/tech/dokus/foundation/sstorage/IOSStorageDelegate.kt | 13 | Source file | 5 |
| foundation/sstorage/src/iosMain/kotlin/tech/dokus/foundation/sstorage/KeychainStorageDelegate.kt | 212 | Source file | 4 |
| foundation/sstorage/src/iosMain/kotlin/tech/dokus/foundation/sstorage/MemoryStorageDelegate.kt | 35 | Source file | 5 |
| foundation/sstorage/src/iosMain/kotlin/tech/dokus/foundation/sstorage/SecureStorage.ios.kt | 74 | Source file | 5 |
| foundation/sstorage/src/iosTest/kotlin/tech/dokus/foundation/sstorage/IOSSecureStorageTest.kt | 64 | Source file | 5 |
| foundation/sstorage/src/jvmMain/kotlin/tech/dokus/foundation/sstorage/JvmSecureStorage.kt | 478 | Source file | 3 |
| foundation/sstorage/src/jvmMain/kotlin/tech/dokus/foundation/sstorage/OSKeychainSecureStorage.kt | 299 | Source file | 5 |
| foundation/sstorage/src/jvmMain/kotlin/tech/dokus/foundation/sstorage/SecureStorage.jvm.kt | 14 | Source file | 5 |
| foundation/sstorage/src/jvmTest/kotlin/tech/dokus/foundation/sstorage/JvmSecureStorageTest.kt | 98 | Source file | 5 |
| foundation/sstorage/src/wasmJsMain/kotlin/tech/dokus/foundation/sstorage/Interop.kt | 28 | Source file | 5 |
| foundation/sstorage/src/wasmJsMain/kotlin/tech/dokus/foundation/sstorage/SecureStorage.wasmJs.kt | 194 | Source file | 5 |
| foundation/sstorage/src/wasmJsTest/kotlin/tech/dokus/foundation/sstorage/WasmSecureStorageTest.kt | 116 | Source file | 5 |
| settings.gradle.kts | 64 | Source file | 5 |