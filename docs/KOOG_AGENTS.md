# Koog Agents in Dokus

Project-specific reference for Koog usage in the Dokus backend.

## Scope

This file documents how Dokus wires Koog in production code.

External framework docs are kept separately in `docs/koog/*` and are intentionally untouched.

## Dependencies

Version catalog entry:
- `gradle/libs.versions.toml`
- `versions.koog = "0.6.1"`

Relevant libraries:
- `ai.koog:koog-agents`
- `ai.koog:prompt-executor-openai-client`
- `ai.koog:agents-features-event-handler`

## Main Integration Points

Dependency injection:
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/AIDI.kt`

Provider/model wiring:
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/config/AIProviderFactory.kt`
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/config/AIModels.kt`
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/config/AIConfig.kt`

Agents:
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/agents/DocumentProcessingAgent.kt`
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/agents/ChatAgent.kt`

Graph strategy and subgraphs:
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/graph/AcceptDocumentStrategy.kt`
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/graph/sub/*`

Tools and registry:
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/tools/TenantDocumentsRegistry.kt`
- `features/ai/backend/src/main/kotlin/tech/dokus/features/ai/tools/*`

## Runtime Model Selection

Dokus supports multiple execution modes (for example, local Ollama and OpenAI-compatible endpoints).

Model selection and capabilities are centralized in `AIModels.kt`; avoid hardcoding model IDs in route/service layers.

## Testing

Current Koog-focused tests live under:
- `features/ai/backend/src/test/kotlin/tech/dokus/features/ai/graph/*`

Use these tests as the baseline when changing graph wiring, tool contracts, or prompt assumptions.

## Change Rules

When modifying Koog integration:
- Keep deterministic business rules outside LLM calls.
- Keep tenant/document context injection explicit.
- Preserve traceability of tool/agent decisions.
- Update this file when adding or removing integration entry points.
