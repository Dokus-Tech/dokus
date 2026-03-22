# Changelog

All notable changes to Dokus are documented in this file.

The format follows Keep a Changelog principles with pragmatic release summaries.

## [1.3.3] - 2026-03-22

### Added
- Dedicated duplicate document review flow with PDF thumbnails and side-by-side comparison (#250).
- Desktop review surface for document validation with keyboard navigation and contextual review queues.
- Cross-platform PDF download with state management, retry functionality, and platform-specific file saving.
- Cashflow tracking start date window for controlling when tracking begins.
- Bank statement draft transaction persistence and deduplication (#250).
- Contact selection in clean document state and context-aware back navigation labels in Document Detail.
- ErrorOverlay component replacing OfflineOverlay with versatile error state handling.

### Changed
- Document Review renamed to Document Detail; Cashflow Ledger renamed to CashFlow Overview.
- Backend entities separated from domain models with unified DTO mapping layer and companion object factory methods.
- MVI architecture modularized with child FlowMVI containers and extracted intent handlers.
- Error handling standardized across the app — global snackbars removed in favor of inline error banners and overlays.
- Dialogs refactored to use Navigation Compose `dialog` routes.
- Typography standardized across platforms; design tokens and MaterialTheme color schemes replace hardcoded values.
- All hardcoded UI strings externalized to string resources.
- "Review later" functionality removed from document review surfaces.

### Fixed
- Error state UI improved for team and workspace settings screens.

### Internal
- Backend repositories refactored: auto-payment and audit logic extracted, matching logic reorganized, ResultRow mappers moved to dedicated files.
- Contact and Cashflow MVI containers use Koin factories for child containers.
- Android unit test snapshots updated.

## [1.3.2] - 2026-03-19

### Changed
- Payment candidate suggestions now filter by cashflow direction, preventing wrong-direction transactions from appearing as matches.

### Fixed
- CSV bank statements now correctly tagged as `CSV_STATEMENT` source instead of `PDF_STATEMENT`.
- `BankTransactionEntity` serialization error in payment candidates endpoint resolved by mapping to DTOs in service layer.

## [1.3.1] - 2026-03-19

### Added
- CSV bank statement import with AI-driven column mapping, encoding detection, and deterministic parsing via Apache Commons CSV (#249).
- Unconfirm document flow to revert confirmed documents back to draft for editing (#247).

### Changed
- Bank transaction auto-match classifier no longer requires HIGH statement trust, allowing PDF-imported transactions with strong signals (OGM, invoice ref) to auto-confirm (#249).
- Banking service returns DTOs instead of raw entities, with mappers following `Dto.Companion.from(entity)` pattern (#249).
- AI extraction date format guidance clarified to ISO strings with improved normalization (#248).
- VAT amount defaults to zero when all line items have 0% rate or VAT data is missing (#247).

### Fixed
- Drag-and-drop file upload now accepts CSV and TXT files (#249).
- Inline text field cursor position preserved when entering edit mode (#247).
- Premature commit on focus loss in editable text fields (#247).

## [1.3.0] - 2026-03-19

### Added
- AI assistant with structured content blocks, document rendering, markdown chat, file upload/download, RAG indexing, and cross-document session flow (#243).
- Structured feedback UI with category chips and streamlined feedback flow (#242).
- Auto-create contact from document counterparty snapshot when no match exists, then confirm (#242).
- EU OSS VAT format recognition (`EU` + 9-12 digits) in validation and AI extraction prompts (#245).
- Current date injection into AI extraction prompts for date format disambiguation (#245).

### Changed
- Search ALL tab now deduplicates documents that already appear as linked transactions (#246).
- VAT amount inferred as zero when all line items have 0% VAT rate or breakdown shows zero amounts, fixing auto-confirm for reverse-charge invoices (#245).
- Default image DPI set to medium for balanced density (#242).
- Database entity mappers converted to `Entity.Companion.from(row)` extension pattern; repositories return entities instead of DTOs (#243).
- `runCatching` replaced with `runSuspendCatching` across backend suspend functions (#243).

### Fixed
- Reprocess query now includes documents with null status, previously omitted from processing health counts (#241).
- Local Docker Compose `POSTGRES_DB` env var aligned with development database name (#244).

## [1.2.2] - 2026-03-16

### Changed
- Needs-review and bulk reprocess candidate selection now include additional non-confirmed documents and use a shared eligibility predicate for more consistent behavior (#238).

### Fixed
- Backend CORS defaults now allow `app.dokus.tech`, so the deployed web app can call the server without extra host overrides (#239).
- Web/WASM image loading now uses a browser-safe Coil configuration and decoder, restoring avatars and other images in the browser (#240).

### Internal
- Splash, payments, documents, and ledger screenshots were refreshed after the latest UI cleanup.

## [1.2.1] - 2026-03-16

### Added
- Internal bank transfer detection plus mark-as-transfer and undo-transfer flows in banking (#235).
- Radial reveal workspace entry transitions with responsive skeleton states for auth and workspace flows (#236).

### Changed
- Auth-to-main navigation on small screens now avoids the unwanted handoff animation and uses a cleaner workspace entry overlay (#236).
- Langfuse tracing for AI agents is now structured and enriched with server metadata.
- Default bulk reprocessing limits were raised to support larger document batches (#237).

### Fixed
- Cloud Traefik routing for `/api` and `/storage` no longer depends on the forwarded `Host` header, avoiding false 404s behind an edge proxy.

## [1.2.0] - 2026-03-16

### Added
- End-to-end banking workflows, including accounts, payments, balances, statement ingestion, manual expense creation, transaction ignore reasons, Bayesian matching, rejection/undo flows, and bank statement auto-confirmation (#217, #233).
- Expanded document workflow coverage with comprehensive and classified-only document types, unsupported document handling, bank statement review improvements, sortable `sortDate` grouping, and processing health with bulk reprocessing (#231, #232).
- Search by numeric amount in unified search (#234).
- Langfuse observability and tracing for AI agents, plus deployment and local environment integration.

### Changed
- AI document processing now uses a cleaner classification/extraction split with improved line-item extraction, counterparty rules, VAT validation, DPI handling, and contact auto-creation fallbacks (#214, #227).
- Contacts and profile flows were streamlined with inline contact editing, richer contact details, direct profile navigation, CompanyHeroSection updates, and better shell/app-bar alignment (#215, #216, #219, #220, #230).
- Shared UI primitives were rolled out more broadly with `PButton`, `DokusFilterBar`, and Lucide icons across the app (#221, #226).

### Fixed
- Banking mobile navigation and filter issues that could break screen access or state transitions (#222, #225).
- Document review state issues around queue switching, date-based list grouping, status handling, and null source channels (#223).
- Incorrect "needs attention" counts and contact details metadata/notes edge cases (#213, #215).
- Backend SSE disconnect handling during document updates.

### Internal
- Gradle toolchain and Foojay resolver configuration updated for builds.
- UI previews, screenshots, fixtures, and tests refreshed to cover the new banking and document flows.

## [1.1.0] - 2026-03-09

### Added
- Skeleton loaders for loading states across all screens (#212).
- UI previews and Roborazzi snapshots for loading and error states.

### Changed
- Loading and error states now preserve screen layout (Scaffold, app bar, navigation chrome) — only the content area changes (#212).
- Unified error display: `DokusErrorContent` replaces `DokusErrorBanner` in all full-content-replacement branches for consistent centered error UX.
- Flattened MVI state structures across all features — replaced sealed `State` interfaces with single data classes using `DokusState<T>` wrappers (#212).
- Standardized initial MVI state initialization across all features.
- `DokusState` enhanced with data persistence across loading/error transitions and smart-cast contracts.
- Simplified contacts screen header and error handling.

### Internal
- Removed redundant UI wrappers and stale explicit casts post-flattening cleanup.
- Screenshot snapshots updated.

## [0.2.0] - 2026-03-08

### Added
- End-to-end authentication flow with multi-tenant access refactor (#175, #185).
- Unified user avatar endpoints, UI rendering, and profile avatar support (#202, #204).
- Tenant-scoped avatar URLs for workspace selection (#194).
- Bank transaction auto-matching and auto-pay automation (#198).
- Document SSE streams and real-time consumers (#201).
- Backend-driven personalized search suggestions (#184).
- LLM priority queue for AI inference orchestration (#205).
- AI document classification with image fetching (#162).
- AI fixture recorder for extraction testing (#161).
- AI-assisted logo fallback with observability budgeting (#193).
- Dedicated purpose similarity index and SERVICE_PERIOD heuristics (#196).
- Lease invoice handling safeguards (#164).
- Company manager and bookkeeper access flows with console CTA (#199).
- Console availability flags and clients API (#187).
- iOS FileProvider integration and backend role metadata (#171).
- Resend email key configuration for notifications (#170).
- Vercel SPA deployment workflow with analytics and speed insights (#163, #165).
- Platform-aware Coil image loader with disk and memory caching via Koin (#209).

### Changed
- Design system v2 overhaul (#179).
- Custom DokusLoader replacing circular progress indicators (#178).
- Document review UI and persistence behavior aligned (#197).
- Document workflow filters and PEPPOL auto-confirm policy aligned (#167).
- Documents upload UX flow refactored (#192).
- Authoritative counterparty extraction with self-healing token contacts (#169).
- Workspace and profile controls moved into home shell (#172).
- Session management stabilized with desktop shell chrome alignment (#203).
- Main tab state transitions stabilized (#188).
- Offline bootstrap resilience improved (#182).
- Recommand webhook flow hardened with debounced polling (#181).
- Document ingestion concurrency and LM Studio timeout tuned (#191).
- Peppol outage handling with recoverable skip path (#190).
- Production app links and email verification assets updated (#183).
- Vercel observability scripts conditionalized (#166).
- Codebase-wide cleanup of inline qualified names replaced with proper imports (#210).

### Fixed
- Desktop invoice sender identity and selected client UI (#186).
- iOS Files tenant headers and session state (#200).
- Share extension upload errors (#168).
- Force logout on unrecoverable auth failures (#195).
- Document processing hang prevention (#173).

### Internal
- Library dependency updates (#207).
- Deprecated components cleanup (#208).
- Database migration files cleanup (#206).
- Navigation changes cleanup (#177).
- Screenshot updates (#211).
- Compose UI best-practices documentation (#180).

## [0.1.17] - 2026-01-23

### Added
- Documents view drag-and-drop file upload support.

### Fixed
- Default intelligence model wiring for vision/chat paths.
- Chat agent executor/provider usage.
- Document extraction fallback behavior.

## [0.1.16] - 2026-01-23

### Added
- Contact recovery from orchestration traces.
- Lookup tracing for AI orchestration.

### Changed
- More lenient JSON parsing in orchestrator pipeline.

## [0.1.15] - 2026-01-22

### Fixed
- Agent system prompt forwarding in runtime configuration.

## [0.1.14] - 2026-01-22

### Added
- Pro forma and credit note extraction paths.

### Changed
- Orchestration model/performance tuning.
- Prompt abstraction and prompt refactoring.

## [0.1.13] - 2026-01-22

### Fixed
- Gross/net amount handling for bill extraction.

## [0.1.12] - 2026-01-21

### Fixed
- LLM JSON parse fallback robustness.
- Trace attribution for fallback extraction tooling.

## [0.1.11] - 2026-01-21

### Added
- Ingestion processing traces and raw extraction persistence/exposure.

### Changed
- Orchestrator lifecycle management and trace normalization.
- AI processing throughput tuning.

## [0.1.10] - 2026-01-21

### Added
- Redis-backed document image cache.
- Image-ID based orchestration flow.

### Fixed
- Image token normalization before decode.

## [0.1.9] - 2026-01-21

### Added
- Tool-based AI orchestrator integration.
- LM Studio OpenAI-compatible executor/config support.
- Contact link decision evidence and policy controls.
- Contact linking tests for document flows.

### Changed
- Refactor from hook-style orchestration to typed handlers.

## [0.1.8] - 2026-01-20

### Added
- Cloud stack Redis password support.
- Higher document-processing concurrency.

### Changed
- Cloud Traefik internal routing alignment.
- Ollama host configuration for containerized networking.

## [0.1.7] - 2026-01-19

### Changed
- Sovereign mode resource targets/concurrency tuning.
- Cloud host defaults and deployment configuration.

## [0.1.6] - 2026-01-19

### Changed
- Container registry migration to `docker.invoid.vision`.

## [0.1.5] - 2026-01-19

### Added
- Cashflow entries parallel overview capability.
- PEPPOL registration flow improvements (including sending-only path and initial polling).
- UI filter toggle improvements and settings UI modernization.

### Changed
- Refactors to split large files in contacts/settings/document processing routes.
- Route enum propagation and typed query cleanup in cashflow/chat.

### Docs/Build
- Project layout docs updated.
- Internal audit/refactor bookkeeping updates.

## [0.1.4] - 2026-01-05

### Added
- Consolidated backend runtime in `backendApp` with expanded cashflow routes and background workers.
- AI platform upgrades (agents, RAG services, config updates).

### Changed
- Namespace and package migration to current `tech.dokus` layout.
- Compose app route/container updates and navigation refactor.
- CI/CD and deployment script improvements.

## [0.1.3] - 2025-12-23

### Added
- Contacts management UI improvements (desktop/mobile patterns).
- Desktop folder drag-and-drop uploads.
- Tag-based release workflow updates.

### Changed
- Design system components and interaction refinements.
- Navigation/provider cleanup.

## [0.1.2] - 2025-12-22

### Added
- Expanded engineering documentation for error handling and diagnostics.
- Additional domain/use-case KDoc coverage.

### Changed
- Repository maintenance and ignore rules.

## [0.1.1] - 2025-12-22

### Added
- Initial tagged release baseline for multiplatform app and backend.
- Tag-driven release pipeline and deployment packaging.

### Changed
- Versioning pipeline aligned to git tags.
