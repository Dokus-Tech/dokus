# Document Processing: Stuck Documents & UI Improvements

## Investigation Summary

### Current Flow
1. **Upload**: `POST /documents/upload` -> stores file in MinIO, creates `DocumentDto` + `IngestionRun` (status=Queued)
2. **Processing**: `DocumentProcessingWorker` polls for Queued runs, processes with AI graph, marks Succeeded/Failed
3. **Frontend**: `DocumentReviewLoader` fetches `DocumentRecordDto`. If `extractedData == null` -> shows `AwaitingExtraction` state

### Edge Cases Where Documents Get Stuck

#### Backend Issues
1. **Processing crash mid-flight**: If worker marks run as `Processing` but crashes before `markAsSucceeded`/`markAsFailed`, the run stays in `Processing` forever. There is NO stale run recovery.
2. **No automatic retry on failure**: When `markAsFailed` is called, the run stays `Failed` permanently. There is NO auto-retry. Only manual `/reprocess` endpoint creates a new run.
3. **Worker not running**: If `DocumentProcessingWorker.start()` was never called or crashed, Queued runs sit forever.

#### Frontend Issues
4. **No polling in AwaitingExtraction**: The `AwaitingExtraction` state shows a spinner but NEVER auto-refreshes. User stares at "Awaiting extraction..." forever.
5. **No polling in Content state when isProcessing**: Even in `Content` state with `isProcessing=true`, there is no auto-refresh.
6. **No error state in AwaitingExtraction**: If AI fails, frontend stays in `AwaitingExtraction` because `extractedData` remains null. The `AnalysisFailedBanner` only shows in `Content` state. Dead end.
7. **Generic loading animation**: `AwaitingExtraction` shows a bare `CircularProgressIndicator`. The document is already uploaded and previewable, but not shown. Scan animation exists but unused here.

### Dokus Philosophy Alignment
- "Every state must be: Explainable, Traceable, Reversible" - violated by stuck state
- "Deterministic Interaction" - users ask "why is this stuck?"
- "One alert. One place. One explanation. One required action." - no alert, no action

---

## Plan

### 1. Backend: Add stale run recovery
- [x] In `ProcessorIngestionRepository`, add `recoverStaleRuns()` that finds `Processing` runs where `startedAt < now - 5min` and resets them to `Failed` with error message "Processing timed out"
- [x] Call `recoverStaleRuns()` at the start of each `processBatch()` cycle in `DocumentProcessingWorker`

### 2. Frontend: Add auto-polling when processing
- [x] In `DocumentReviewRoute.kt`, add a `LaunchedEffect` that fires `Refresh` every 3 seconds when state is `AwaitingExtraction` or `Content` with `isProcessing == true`

### 3. Frontend: Handle failed extraction in AwaitingExtraction
- [x] In `DocumentReviewLoader.transitionToContent()`, when `extractedData == null` but `latestIngestion.status == Failed`, transition to `Content` with `draftData = null` (so `AnalysisFailedBanner` shows with retry + continue manually options)

### 4. Frontend: Replace generic spinner with document preview + scan animation
- [x] Rewrite `AwaitingExtractionContent` to show the PDF preview with scan animation overlay instead of a centered spinner
- [x] Desktop: Split layout — PDF preview with scan line (left), processing status (right)
- [x] Mobile: Full PDF preview with scan animation overlay, status text at bottom

---

## Review

### Changes Summary

**7 files modified** across backend and frontend:

| File | Change |
|------|--------|
| `ProcessorIngestionRepository.kt` | Added `recoverStaleRuns()` — finds runs stuck in `Processing` for >5min and marks them `Failed` |
| `DocumentProcessingWorker.kt` | Calls `recoverStaleRuns()` at start of each `processBatch()` cycle, wrapped in `runSuspendCatching` |
| `DocumentReviewLoader.kt` | When `extractedData == null` and ingestion `Failed`, transitions to `Content(draftData=null)` so `AnalysisFailedBanner` shows. Also fires `LoadPreviewPages` for `AwaitingExtraction`. |
| `DocumentReviewRoute.kt` | Added auto-polling `LaunchedEffect` — fires `Refresh` every 3s when `AwaitingExtraction` or `Content.isProcessing` |
| `DocumentReviewState.kt` | Added `previewState: DocumentPreviewState` to `AwaitingExtraction` data class |
| `DocumentReviewPreview.kt` | Added `loadPreviewPagesForAwaiting()` to handle `LoadPreviewPages` intent for `AwaitingExtraction` state |
| `ReviewContent.kt` | Rewrote `AwaitingExtractionContent`: Desktop shows PDF preview + scan animation (left) with status panel (right); Mobile shows full preview with gradient overlay and status at bottom |

### Edge Cases Now Covered
1. Stale Processing runs -> recovered by backend, frontend polls and sees Failed status
2. Failed extraction -> frontend transitions to Content with AnalysisFailedBanner (retry + continue manually)
3. Slow extraction -> frontend auto-polls every 3s, transitions automatically when done
4. Generic spinner -> replaced with PDF preview + scan animation

### Build Verification
- `./gradlew :features:cashflow:presentation:compileKotlinWasmJs` — BUILD SUCCESSFUL
- `./gradlew :backendApp:compileKotlin` — BUILD SUCCESSFUL
