# Changelog

All notable changes to Dokus are documented in this file.

The format follows Keep a Changelog principles with pragmatic release summaries.

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
