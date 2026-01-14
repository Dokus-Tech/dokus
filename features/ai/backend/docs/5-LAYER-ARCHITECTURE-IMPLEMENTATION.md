# 5-Layer Autonomous Architecture - Implementation Notes

> Implementation notes and insights from building the autonomous document processing pipeline.

## Overview

The 5-Layer architecture transforms Dokus AI from "trying to get it right" to "proving it is right":

| Layer | Component | Purpose | Implementation |
|-------|-----------|---------|----------------|
| 1 | Perception Ensemble | Multi-model extraction | `PerceptionEnsemble.kt` |
| 2 | Consensus Engine | Field-level conflict resolution | `ConsensusEngine.kt` |
| 3 | Legally-Aware Auditor | Belgian compliance validation | `ExtractionAuditService.kt` |
| 4 | Self-Correction Loop | Feedback-driven retries | `FeedbackDrivenRetryAgent.kt` |
| 5 | Judgment Agent | AUTO_APPROVE / NEEDS_REVIEW / REJECT | `JudgmentAgent.kt` |

---

## Layer 1: Perception Ensemble

### Key Insight: Parallel Execution
Both models (qwen3-vl:8b and qwen3-vl:72b) run in parallel using Kotlin coroutines. On M4 Max with 128GB RAM, both can be kept loaded simultaneously.

```kotlin
val fastDeferred = async { fastAgent.extract(images) }
val expertDeferred = async { expertAgent.extract(images) }
// Total time ≈ max(fast, expert) instead of sum
```

### Design Decision: Generic Type Parameter
`PerceptionEnsemble<T>` is generic to support any extraction type (Invoice, Bill, Receipt, etc.) without duplication.

---

## Layer 2: Consensus Engine

### Key Insight: Numeric Equivalence
Amounts like "100.00" and "100" are semantically equivalent. The consensus engine treats these as agreement, not conflict.

```kotlin
// "100.00" == "100" → No conflict
// Uses BigDecimal comparison to handle formatting differences
```

### Design Decision: Expert Preference
When models disagree, expert model value is preferred by default. This is configurable via `ModelWeight`:
- `PREFER_EXPERT` (default for critical fields)
- `PREFER_FAST` (rarely used)
- `REQUIRE_MATCH` (forces human review on disagreement)

### Critical Fields
These fields have `CRITICAL` severity when models disagree:
- `totalAmount`, `subtotal`, `totalVatAmount`
- `iban`, `paymentReference`
- `vendorVatNumber`, `clientVatNumber`

---

## Layer 3: Legally-Aware Auditor

### Key Insight: Belgian Compliance
The auditor understands Belgian-specific rules:

1. **OGM Validation**: Mod-97 checksum with OCR correction (0↔O, 8↔B, etc.)
2. **VAT Rates**: 0%, 6%, 12%, 21% - flags unusual rates
3. **March 2026 Reform**: 12% rate expansion for Horeca (date-aware)
4. **Belgian IBAN**: Exactly 16 characters

### Design Decision: Hints for Self-Correction
Each `AuditCheck` includes a `hint` field specifically for Layer 4:
```kotlin
AuditCheck.criticalFailure(
    type = CheckType.MATH,
    field = "totalAmount",
    message = "100 + 21 ≠ 120",
    hint = "Expected 121.00. Check for digit substitution (1↔7, 0↔6).",
    expected = "121.00",
    actual = "120.00"
)
```

---

## Layer 4: Self-Correction Loop

### Key Insight: Specific Feedback Beats Generic Retry
Generic "try again" prompts don't work. Specific hints dramatically improve correction rates:

**Bad**: "The extraction failed. Please try again."

**Good**:
```
❌ OGM CHECKSUM FAILED
Expected check digit: 39, found: 40
Common OCR mistakes: 0↔O, 1↔I, 8↔B, 5↔S, 6↔G
Re-read the PAYMENT SECTION carefully.
```

### Design Decision: Max 2 Retries
Default configuration limits retries to 2 attempts. Beyond this, the document likely has genuine issues that retrying won't fix.

### Feedback Prompt Structure
Each failure type has a specialized feedback builder:
- `buildMathFeedback()`: Points to TOTALS section
- `buildOgmFeedback()`: Points to PAYMENT section with OCR hints
- `buildIbanFeedback()`: Points to BANK DETAILS with length check
- `buildVatRateFeedback()`: Lists valid Belgian rates
- `buildCompanyFeedback()`: Suggests re-reading VAT number

---

## Layer 5: Judgment Agent

### Key Insight: Deterministic First
The plan suggested using an LLM (qwen3:30b) for judgment. However, we implemented a **hybrid approach**:

1. **Deterministic path** (JudgmentCriteria): Fast, predictable rules for 90%+ of cases
2. **LLM path** (optional): For edge cases requiring nuanced reasoning

This gives us:
- Speed: <1ms for deterministic decisions
- Predictability: Same inputs always produce same outputs
- Flexibility: LLM available when needed

### Design Decision: Configurable Thresholds
Three configurations for different use cases:

| Config | Auto-Approve Min | Review Min | Use Case |
|--------|-----------------|------------|----------|
| DEFAULT | 80% | 50% | Balanced |
| STRICT | 90% | 60% | High-value documents |
| LENIENT | 70% | 40% | High-volume processing |

### Decision Tree
```
1. Essential fields missing? → REJECT
2. Document type unknown? → REJECT
3. Critical failures after retry? → REJECT
4. Confidence < review threshold? → REJECT
5. Critical conflicts (if consensus required)? → NEEDS_REVIEW
6. Confidence < approve threshold? → NEEDS_REVIEW
7. Too many warnings? → NEEDS_REVIEW
8. Otherwise → AUTO_APPROVE
```

---

## Tool-Calling (Phase 3)

### Key Insight: Koog SimpleTool Interface
Koog provides `SimpleTool<Args>` for tools that return text:

```kotlin
object VerifyTotalsTool : SimpleTool<VerifyTotalsTool.Args>(
    argsSerializer = Args.serializer(),
    name = "verify_totals",
    description = "Validates subtotal + VAT = total..."
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The subtotal amount...")
        val subtotal: String,
        ...
    )

    override suspend fun execute(args: Args): String = ...
}
```

### Design Decision: Post-Processing Fallback
The plan noted "Ollama models apply tools inconsistently". Our tools work both as:
1. **Agent tools**: Called during extraction (if model supports)
2. **Post-processing validators**: Used by ExtractionAuditService regardless

This ensures validation happens even if tool-calling is unreliable.

---

## Test Coverage

| Layer | Tests | Key Coverage |
|-------|-------|--------------|
| L1-2 (Ensemble/Consensus) | 17 | Merge scenarios, conflict detection |
| L3 (Validation Tools) | 29 | Math, OGM, IBAN edge cases |
| L4 (Retry) | 27 | Feedback generation, retry logic |
| L5 (Judgment) | 35 | All outcomes, configs, edge cases |
| **Total** | **108+** | |

---

## Performance Characteristics (M4 Max, 128GB)

| Operation | Time | Notes |
|-----------|------|-------|
| Fast model (8b) | ~3s | Quick baseline |
| Expert model (72b) | ~8s | High accuracy |
| Parallel ensemble | ~9s | Not 11s = speedup |
| Judgment (deterministic) | <1ms | No LLM needed |
| Judgment (LLM) | ~500ms | qwen3:30b |

### Memory Budget
```
qwen3-vl:8b    :   8GB
qwen3-vl:72b   :  55GB
qwen3:30b      :  24GB
─────────────────────
Subtotal       :  87GB
System + Apps  :  20GB
Buffer         :  21GB
```

---

## Future Considerations

### Potential Improvements
1. **Confidence calibration**: Track actual vs predicted confidence over time
2. **Feedback learning**: Store which hints led to successful corrections
3. **Model routing**: Skip expert model for clearly simple documents
4. **Batch processing**: Process multiple documents in parallel

### Not Implemented (Intentionally)
1. **Tool-calling reliability test**: Deferred until production usage patterns emerge
2. **LLM judgment by default**: Deterministic is sufficient for most cases
3. **Automatic model selection**: Fixed ensemble works well on target hardware

---

## Files Created

### Phase 1: Validation (Layer 3)
- `validation/AuditModels.kt` - CheckType, Severity, AuditCheck, AuditReport
- `validation/MathValidator.kt` - Mathematical verification
- `validation/BelgianVatRateValidator.kt` - March 2026 aware
- `validation/ChecksumValidator.kt` - OGM/IBAN wrapper
- `validation/ExtractionAuditService.kt` - Orchestrator

### Phase 2: Ensemble (Layers 1-2)
- `ensemble/ConsensusModels.kt` - FieldConflict, ConflictReport, ConsensusResult
- `ensemble/PerceptionEnsemble.kt` - Parallel model execution
- `ensemble/ConsensusEngine.kt` - Field-level conflict resolution

### Phase 3: Tools
- `tools/VerifyTotalsTool.kt` - Math validation tool
- `tools/ValidateOgmTool.kt` - OGM checksum tool
- `tools/ValidateIbanTool.kt` - IBAN checksum tool
- `tools/LookupCompanyTool.kt` - CBE registry lookup
- `tools/FinancialToolRegistry.kt` - Tool registry factory

### Phase 4: Retry (Layer 4)
- `retry/RetryModels.kt` - RetryResult, RetryConfig
- `retry/FeedbackPromptBuilder.kt` - Specific hint generation
- `retry/FeedbackDrivenRetryAgent.kt` - Self-correction loop

### Phase 5: Judgment (Layer 5)
- `judgment/JudgmentModels.kt` - JudgmentOutcome, JudgmentDecision, JudgmentContext
- `judgment/JudgmentCriteria.kt` - Deterministic decision rules
- `judgment/JudgmentAgent.kt` - Final gatekeeper

### Phase 6: Coordinator (Pipeline Orchestration)
- `coordinator/ProcessingConfig.kt` - Pipeline configuration with presets
- `coordinator/AutonomousResult.kt` - Result hierarchy and statistics
- `coordinator/AutonomousProcessingCoordinator.kt` - 5-layer orchestrator

---

## Phase 6: Pipeline Coordinator

### Key Insight: Configuration Presets

Different use cases require different pipeline configurations. Instead of exposing all parameters, we provide curated presets:

| Preset | Use Case | Characteristics |
|--------|----------|-----------------|
| `DEFAULT` | Balanced processing | Full ensemble, self-correction enabled |
| `FAST` | High-volume, lower stakes | Single model, no retries, lenient judgment |
| `THOROUGH` | High-value documents | Full ensemble, aggressive retries, strict judgment |
| `OFFLINE` | No network access | External validation disabled |
| `DEVELOPMENT` | Testing and debugging | Single model, provenance enabled |

### Design Decision: Type-Specific Processing

The coordinator routes each document type to appropriate extraction, consensus, and audit paths:

```kotlin
when (classification.documentType) {
    INVOICE, CREDIT_NOTE, PRO_FORMA -> processInvoice(...)
    BILL -> processBill(...)
    RECEIPT -> processReceipt(...)
    EXPENSE -> processExpense(...)
    UNKNOWN -> processUnknown(...)
}
```

This ensures domain-specific validation rules are applied correctly:
- Invoices get OGM payment reference validation
- Bills get supplier VAT number validation
- Receipts get simpler validation (no payment references)
- Expenses derive subtotal from total - VAT

### Design Decision: Graceful Degradation

The coordinator implements multiple fallback strategies:

1. **Ensemble Fallback**: If one model fails, use the other
2. **Agent Fallback**: If expert agent unavailable, use fast agent
3. **Retry Fallback**: If retry agent unavailable, skip self-correction
4. **Early Rejection**: Fast-fail on classification failures

### Design Decision: Result Hierarchy

`AutonomousResult` is a sealed class with two variants:

```kotlin
sealed class AutonomousResult {
    data class Success<T>(...) : AutonomousResult() {
        val isAutoApproved: Boolean
        val needsReview: Boolean
        val wasCorrected: Boolean
        val confidence: Double
    }

    data class Rejected(...) : AutonomousResult() {
        val reason: String
        val stage: RejectionStage
        val details: Map<String, String>
    }
}
```

Key insight: `Success` can still have `REJECT` judgment. It means the pipeline completed but the judgment agent decided to reject. `Rejected` means the pipeline failed early (classification, extraction failure).

### Statistics and Monitoring

The `AutonomousProcessingStats` class tracks pipeline health:

```kotlin
val stats = AutonomousProcessingStats.from(results)
if (!stats.meetsSilenceGoal) {
    // AUTO_APPROVE rate < 95% - investigate
    logger.warn("Silence goal not met: ${stats.autoApproveRate}%")
}
```

### Not Serializable (Intentional)

`AutonomousResult` is not `@Serializable` because:
1. Contains generic types (`Success<T>`)
2. Contains `RetryResult<T>` which is also generic
3. Is primarily a runtime model, not a wire format

If serialization is needed, convert to a DTO first.

---

## Summary

The 5-Layer architecture achieves the "Silence" philosophy goal:
- **AUTO_APPROVE** target: 95%+ of clean documents
- Users only see documents when the system cannot prove correctness
- Each layer adds confidence, not latency (parallel execution)
- Deterministic paths ensure predictability
- Specific feedback enables effective self-correction
