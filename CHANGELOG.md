# Changelog

All notable changes to Dokus are documented in this file.

The format follows Keep a Changelog principles with pragmatic release summaries.

## [Unreleased]

### Changed
- Documentation refresh and cleanup.

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
