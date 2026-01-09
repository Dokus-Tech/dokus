# Documents Screen Refactor - Firstbase-Inspired Design

## Overview

Refactor the Documents screen to be a clean operational inbox (no charts/insights) with Firstbase-inspired styling: calm density, table-style rows, subtle chips, filter pills.

**Core Rule**: Documents = operational inbox for inbound documents (uploads, Peppol, email). No insights, charts, trends, metrics, or dashboard cards.

---

## Current State Analysis

### Existing Files
- `documents/screen/DocumentsScreen.kt` - Main screen composable
- `documents/components/DocumentRow.kt` - Card-based row (needs refactor to table-style)
- `documents/components/DocumentStatusFilterChips.kt` - Filter chips (needs more statuses)
- `documents/components/DocumentStatusChip.kt` - Status badge (already good)
- `documents/mvi/DocumentsContract.kt` - State/Intent/Action definitions
- `documents/mvi/DocumentsContainer.kt` - FlowMVI store
- `documents/route/DocumentsRoute.kt` - Navigation route

### Existing Patterns to Reuse
- `PSearchFieldCompact` - Search input component
- `DocumentUploadSidebar` - Upload side panel (exists but not integrated)
- `FinancialDocumentTable` / `FinancialDocumentTableRow` - Table styling patterns
- `DokusCardSurface` - Card surface styling
- FlowMVI pattern already implemented

---

## Checklist

### Phase 1: Add Upload Button & Sidebar Integration

- [ ] **1.1** Add upload button to DocumentsScreen top bar (right side next to search)
- [ ] **1.2** Add upload sidebar state to DocumentsRoute (`isUploadSidebarVisible`)
- [ ] **1.3** Integrate `DocumentUploadSidebar` component into DocumentsRoute
- [ ] **1.4** Add `DocumentUploadManager` injection to DocumentsRoute
- [ ] **1.5** Wire upload button click to show sidebar

### Phase 2: Enhance Filter Chips

- [ ] **2.1** Add "Processing" filter to DocumentStatusFilterChips (for IngestionStatus.Processing/Queued)
- [ ] **2.2** Add "Failed" filter to DocumentStatusFilterChips (for IngestionStatus.Failed)
- [ ] **2.3** Add "Rejected" filter to DocumentStatusFilterChips (for DraftStatus.Rejected)
- [ ] **2.4** Update DocumentsContract state to support filter by DocumentDisplayStatus instead of just DraftStatus
- [ ] **2.5** Update DocumentsContainer to handle new filter logic
- [ ] **2.6** Ensure filter chips horizontally scroll on smaller screens

### Phase 3: Refactor DocumentRow to Table-Style

- [ ] **3.1** Remove Card wrapper from DocumentRow - use flat row with subtle hover state
- [ ] **3.2** Update row layout for better column alignment:
  - Left: Counterparty/Filename (title) + Document type (subtitle)
  - Middle: Issue date (if available)
  - Right: Amount + Status chip
- [ ] **3.3** Add horizontal dividers between rows
- [ ] **3.4** Add hover/pressed state styling (subtle background change)
- [ ] **3.5** Improve amount formatting (use existing formatter pattern from FinancialDocumentTable)
- [ ] **3.6** Add chevron icon on right for navigation affordance

### Phase 4: Improve Empty States

- [ ] **4.1** Add proper empty state for no documents at all: "No documents yet" + Upload CTA
- [ ] **4.2** Add empty state for filter with no results: "No documents match this filter"
- [ ] **4.3** Add empty state for search with no results: "No results for '[query]'"
- [ ] **4.4** Add string resources for new empty states

### Phase 5: Improve DocumentsScreen Layout

- [ ] **5.1** Move search to proper top bar layout (full width with padding)
- [ ] **5.2** Add upload button next to search (icon button or small button)
- [ ] **5.3** Improve filter chips row with proper horizontal scrolling
- [ ] **5.4** Add table header row (optional - only if it improves readability)
- [ ] **5.5** Improve loading state (centered spinner is fine)
- [ ] **5.6** Add pull-to-refresh support for mobile

### Phase 6: Code Quality & Testing

- [ ] **6.1** Run detekt to check for issues
- [ ] **6.2** Verify Android compilation
- [ ] **6.3** Verify desktop compilation
- [ ] **6.4** Manual testing: verify all filter combinations work
- [ ] **6.5** Manual testing: verify upload sidebar opens/closes
- [ ] **6.6** Manual testing: verify row click navigates to Document Review

---

## Technical Details

### New String Resources Needed

```xml
<!-- Add to cashflow.xml -->
<string name="documents_empty_title">No documents yet</string>
<string name="documents_empty_upload_cta">Upload your first document</string>
<string name="documents_filter_no_match">No documents match this filter</string>
<string name="documents_search_no_results">No results for "%1$s"</string>
<string name="documents_filter_processing">Processing</string>
<string name="documents_filter_failed">Failed</string>
<string name="documents_filter_rejected">Rejected</string>
```

### Filter Status Mapping

| UI Filter | Backend Field | Value |
|-----------|---------------|-------|
| All | (none) | null |
| Processing | IngestionStatus | Processing, Queued |
| Needs review | DraftStatus | NeedsReview, NeedsInput |
| Ready | DraftStatus | Ready |
| Confirmed | DraftStatus | Confirmed |
| Failed | IngestionStatus | Failed |
| Rejected | DraftStatus | Rejected |

### DocumentRow Column Layout

```
[Counterparty/Filename]          [Issue Date]     [Amount]
[Document Type]                                   [Status Chip] [>]
```

Widths:
- Left column: weight(1f) - fills available space
- Date column: 100.dp (optional, hide on mobile)
- Amount: 80.dp
- Status + chevron: wrap_content

### Colors (from existing DocumentStatusChip)

Already implemented in `DocumentStatusChip.kt`:
- Processing: Light orange (#FFF3E0) / Dark orange (#E65100)
- NeedsReview: Light blue (#E3F2FD) / Dark blue (#1565C0)
- Ready: Light green (#E8F5E9) / Dark green (#2E7D32)
- Confirmed: Light teal (#E0F2F1) / Dark teal (#00695C)
- Failed: Light red (#FFEBEE) / Dark red (#C62828)
- Rejected: Light pink (#FCE4EC) / Dark pink (#AD1457)

---

## Files to Modify

| File | Changes |
|------|---------|
| `DocumentsScreen.kt` | Add upload button, improve layout, add table header |
| `DocumentsRoute.kt` | Integrate upload sidebar, add manager |
| `DocumentRow.kt` | Remove Card, use flat row with dividers |
| `DocumentStatusFilterChips.kt` | Add Processing, Failed, Rejected filters |
| `DocumentsContract.kt` | Update filter type to support display status |
| `DocumentsContainer.kt` | Handle new filter logic |
| `cashflow.xml` | Add new string resources |

---

## Acceptance Criteria

- [ ] Documents screen shows list from `/api/v1/documents` with correct statuses
- [ ] No charts/insights/overview cards on Documents screen
- [ ] Search + filter pills work correctly
- [ ] Row click opens Document Review screen
- [ ] Upload sidebar opens from upload button
- [ ] Empty states display appropriately
- [ ] Build passes (Android + Desktop)

---

## Reference Screenshots

1. **Current Dokus Documents screen** - Simple rows with search, filter pills (All, Needs review, Ready, Confirmed), and document cards
2. **Firstbase Invoices screen** - Clean table layout, filter pills, subtle chips, calm density

**Key differences to implement:**
- Remove Card elevation from rows
- Add horizontal dividers
- Improve column alignment
- Add upload button to top bar
- Add more filter statuses

---

## Review - Completed Implementation

### Changes Made

**1. String Resources (`cashflow.xml`):**
- Added new strings for Documents screen: `documents_empty_title`, `documents_empty_upload_cta`, `documents_filter_no_match`, `documents_search_no_results`
- Added filter label strings: `documents_filter_all`, `documents_filter_processing`, `documents_filter_needs_review`, `documents_filter_ready`, `documents_filter_confirmed`, `documents_filter_failed`, `documents_filter_rejected`
- Added `documents_upload` and `documents_view_details`

**2. Filter Enhancement (`DocumentStatusFilterChips.kt`):**
- Changed from `DraftStatus` to `DocumentDisplayStatus` for filtering
- Added all 7 filter options: All, Processing, Needs review, Ready, Confirmed, Failed, Rejected
- Added horizontal scrolling for smaller screens

**3. Use Case Update (`LoadDocumentRecordsUseCase.kt` & impl):**
- Added `ingestionStatus: IngestionStatus?` parameter
- Updated implementation to pass through to data source

**4. Contract Update (`DocumentsContract.kt`):**
- Changed `statusFilter` from `DraftStatus?` to `DocumentDisplayStatus?`
- Updated intent type accordingly

**5. Container Update (`DocumentsContainer.kt`):**
- Added `toApiFilters()` helper to map `DocumentDisplayStatus` to API params
- Updated all filter/search/refresh methods to use the new mapping

**6. Row Refactor (`DocumentRow.kt`):**
- Removed Card wrapper
- Changed to flat Row with surface background
- Added horizontal dividers between rows
- Added chevron icon for navigation affordance
- Improved typography and spacing

**7. Screen Layout (`DocumentsScreen.kt`):**
- Added upload button in top bar next to search
- Added `onUploadClick` callback parameter
- Added empty states component with proper messaging
- Wrapped list in `DokusCardSurface` for clean appearance
- Added proper dividers between rows

**8. Route Integration (`DocumentsRoute.kt`):**
- Integrated `AddDocumentContainer` for upload management
- Added upload sidebar with visibility state
- Added QR dialog for mobile app download
- Auto-refresh on upload completion

### Acceptance Criteria Status

- [x] Documents screen shows list from `/api/v1/documents` with correct statuses
- [x] No charts/insights/overview cards on Documents screen
- [x] Search + filter pills work (all 7 filters supported)
- [x] Row click opens Document Review screen
- [x] Upload sidebar opens from upload button
- [x] Empty states display appropriately
- [x] Android build passes
- [x] Backend build passes
