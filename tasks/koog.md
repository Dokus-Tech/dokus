# Koog Graph-Based Agent Architecture

**Goal:** Migrate from `singleRunStrategy` to Koog graph-based architecture.
**Principle:** Code Controls, AI Assists.

Full plan: `/Users/voider/.claude/plans/expressive-meandering-ember.md`

---

## Phase 1: Graph Infrastructure & Cleanup

**Goal:** Set up foundation, remove legacy code.

- [ ] Create `graphs/` package with `GraphTypes.kt`, `NodeExtensions.kt`
- [ ] Define node types: deterministic, AI-assisted, router, gate
- [ ] Delete `ProcessingTrace.kt` (ToolTraceSink)
- [ ] Remove `traceSink` from all tools
- [ ] Add `@property:LLMDescription` to all extraction data classes
- [ ] Create `ModelConfig` (qwn3-vl-30b vision, gpt-oss-20b reasoning)

---

## Phase 2: Document Processing Graph

**Goal:** Replace `singleRunStrategy` with explicit graph.

- [ ] Create `DocumentProcessingGraph.kt` with strategy DSL
- [ ] Implement deterministic nodes: `ingest`, `validate`, `confidenceGate`, `persist`
- [ ] Implement AI nodes: `classify` using `nodeLLMRequestStructured`
- [ ] Wire edges: `start → ingest → classify → extraction → validate → gate → persist → finish`

---

## Phase 3: Extraction Subgraph (7 Types)

**Goal:** Route documents to type-specific extraction paths.

- [ ] Create `ExtractionSubgraph.kt`
- [ ] Create `nodeUnwrap<T>()` helper for `Result<StructuredResponse<T>> → T`
- [ ] Implement 7 paths:
  - INVOICE: `extract → unwrap → enrich (customer lookup)`
  - INBOUND_INVOICE: `extract → unwrap → enrich (supplier lookup)`
  - RECEIPT: `extract → unwrap → categorize (AI)`
  - EXPENSE: `extract → unwrap → categorize (rules)`
  - CREDIT_NOTE: `extract → unwrap → enrich (type detection)`
  - PRO_FORMA: `extract → unwrap → mark neutral`
  - UNKNOWN: `mark for review`
- [ ] Implement `normalize` node (convergence)

---

## Phase 4: Contact Resolution Subgraph

**Goal:** Deterministic contact linking.

- [ ] Create `ContactResolutionSubgraph.kt`
- [ ] Implement `lookup` (VAT query) and `decide` (auto-link vs suggest) nodes

---

## Phase 5: Tracing Integration

**Goal:** Replace ToolTraceSink with Koog native tracing.

- [ ] Add `install(Tracing)` with log + file writers
- [ ] Add `install(AgentEventHandler)` for metrics
- [ ] Verify LLM/tool/node events logged

---

## Phase 6: Cashflow Derivation Graph

**Goal:** 100% deterministic cashflow creation.

- [ ] Create `CashflowDerivationGraph.kt`
- [ ] Implement nodes: `checkRules`, `deriveEntry`, `handlePartials`, `commit`
- [ ] Rules: `legalType ≠ ProForma`, `status = Confirmed`, `direction ≠ Neutral`

---

## Phase 7: ToolSet Migration

**Goal:** Consolidate tools with `@Tool` annotations.

- [ ] Create `ValidationTools.kt` (IBAN, OGM, totals)
- [ ] Create `ContactTools.kt` (lookup, create)
- [ ] Create `DocumentTools.kt` (images, PEPPOL)
- [ ] Create `ExtractionTools.kt` (6 extraction tools)
- [ ] Update `OrchestratorToolRegistry` to use `tools(ToolSet)` pattern

---

## Phase 8: Testing

**Goal:** Verify with Koog testing utilities.

- [ ] Add `ai.koog:agents-test` dependency
- [ ] Create `getMockExecutor()` with mocked responses
- [ ] Use `testGraph<>()`, `assertNodes{}`, `assertEdges{}` for structure tests
- [ ] Test all 7 document type paths
- [ ] Generate Mermaid diagram: `strategy.asMermaidDiagram()`

---

## Phase 9: Persistence & OpenTelemetry

**Goal:** Production-grade observability.

- [ ] Add `FilePersistenceStorageProvider` for checkpoints
- [ ] Add `install(OpenTelemetry)` with span exporter
- [ ] Verify spans for agent/strategy/node/LLM/tool

---

## Phase 10: Chat Agent (Future)

**Goal:** Conversational agent for queries.

- [ ] Create `ChatAgentGraph.kt` using `chatAgentStrategy()`
- [ ] Implement read-only tools: `CashflowQueryTools`, `CompanyContextTools`
- [ ] Add WebSocket/SSE endpoint

---

## Key Koog APIs

| API | Purpose |
|-----|---------|
| `node<I,O>{}` | Deterministic node |
| `nodeLLMRequestStructured<T>` | AI node returning `Result<StructuredResponse<T>>` |
| `nodeUnwrap<T>{}` | Helper to unwrap Result to T |
| `strategy<I,O>{}` | Define graph |
| `subgraph<I,O>{}` | Encapsulated sub-graph |
| `edge(a forwardTo b onCondition{})` | Connect nodes |
| `install(Tracing)` | Event logging |
| `install(AgentEventHandler)` | Fine-grained events |
| `install(OpenTelemetry)` | Production spans |
| `getMockExecutor()` | Testing |
| `testGraph<>{}` | Structure testing |

---

## Models

| Task | Model |
|------|-------|
| Classification | qwn3-vl-30b |
| Extraction | qwn3-vl-30b |
| Orchestration | gpt-oss-20b |
| Chat | gpt-oss-20b |
