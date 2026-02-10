# Backend DI Refactoring: Move AI Services to AI Module

**Status:** Complete
**Date:** 2026-02-10

---

## Plan

### 1. Create `StorageDocumentFetcher` named class
- [x] Create `backendApp/.../services/documents/StorageDocumentFetcher.kt`
- [x] Implements `DocumentFetcher` interface from `features/ai`
- [x] Constructor takes `DocumentRepository` + `DocumentStorageService`

### 2. Move `ExampleRepository` binding to `repositoryModuleAI`
- [x] Add `single { DocumentExamplesRepository() } bind ExampleRepository::class` to `RepositoryModules.kt`
- [x] Remove from `processorModule()`

### 3. Expand `aiModule()` in `features/ai/AIDI.kt`
- [x] Move from processorModule: Redis AI cache, `DocumentImageCache`, `ChunkingService`, `EmbeddingService`, `PromptExecutor`, `DocumentProcessingAgent`
- [x] Add new registrations: `RAGService`, `ChatAgent`, `ModelSet`
- [x] Use `singleOf` where possible

### 4. Rename `processorModule` to `documentProcessingModule`
- [x] Keep only: `StorageDocumentFetcher`, `ContactResolutionService`, `AutoConfirmPolicy`, `DocumentProcessingWorker`
- [x] Convert to `singleOf` syntax
- [x] Update reference in `configureDependencyInjection`
- [x] Clean up unused imports

### 5. Simplify `ChatRoutes.kt`
- [x] Remove inline creation of `EmbeddingService`, `RAGService`, `ChatAgent`
- [x] Inject `ChatAgent` and `ModelSet` via Koin
- [x] Remove unused `httpClient`, `executor`, `aiConfig`, `chunksRepository` injections
- [x] Update `processChat` to accept `ModelSet` instead of `AIConfig`
- [x] Clean up unused imports

---

## Review

### Build verification
- `./gradlew :backendApp:compileKotlin :features:ai:backend:compileKotlin :foundation:database:compileKotlin` — BUILD SUCCESSFUL

### Files changed

| File | Change |
|------|--------|
| `backendApp/.../services/documents/StorageDocumentFetcher.kt` | NEW — named `DocumentFetcher` implementation replacing inline anonymous object |
| `features/ai/backend/.../AIDI.kt` | EXPANDED — now registers all AI infrastructure, services, agents, and tool registries |
| `backendApp/.../config/DependencyInjection.kt` | SHRUNK — removed all AI registrations, renamed `processorModule` → `documentProcessingModule`, cleaned imports |
| `foundation/database/.../di/RepositoryModules.kt` | ADDED — `DocumentExamplesRepository` binding in `repositoryModuleAI` |
| `backendApp/.../routes/cashflow/ChatRoutes.kt` | SIMPLIFIED — inject `ChatAgent` and `ModelSet` from Koin, removed duplicate service creation |

### Key decisions
- Used `single<DocumentFetcher> { StorageDocumentFetcher(get(), get()) }` instead of `singleOf` + `bind` because the Koin `bind` infix on `singleOf` requires a different import (`org.koin.core.module.dsl.bind`) than what was in the file
- Registered `ModelSet` as a singleton in `aiModule()` (`single { AIModels.forMode(get<AIConfig>().mode) }`) since both `ChatAgent` and `ChatRoutes` need it
- `ChatAgent` cannot use `singleOf` because it needs `models.chat` (a field of `ModelSet`), not the full `ModelSet`
- Eliminated the duplicate `EmbeddingService` that `ChatRoutes` was creating inline
