# Refactor Analysis: CashflowScreen.kt

## Overview

**File:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/screens/CashflowScreen.kt`
**Current Size:** 747 lines
**Target Size:** < 300 lines
**Pattern Reference:** `BusinessHealthCard.kt` (well-structured component with state handling)

---

## Current Structure Analysis

### Main Composables in CashflowScreen.kt

| Composable | Lines | Visibility | Description |
|------------|-------|------------|-------------|
| `CashflowScreen` | 94-322 (228 lines) | internal | Main entry point with state collection, drag/drop, scaffold |
| `DesktopCashflowContent` | 330-406 (76 lines) | private | Desktop layout with summary cards + table |
| `MobileCashflowContent` | 413-471 (58 lines) | private | Mobile layout with list only |
| `MobileDocumentsSection` | 477-522 (45 lines) | private | Mobile documents list with state handling |
| `MobileDocumentsListSkeleton` | 528-558 (30 lines) | private | Loading skeleton for mobile list |
| `SummaryCardsRow` | 567-614 (47 lines) | private | Row containing VAT, Health, Pending cards |
| `DocumentsTableSection` | 620-668 (48 lines) | private | Desktop documents table with state handling |
| `DocumentsTableSkeleton` | 674-711 (37 lines) | private | Loading skeleton for desktop table |
| `EmptyDocumentsState` | 717-730 (13 lines) | private | Empty state component |
| `LoadingMoreIndicator` | 736-745 (9 lines) | private | Infinite scroll loading indicator |

---

## Logical UI Sections Identified

### 1. **Top App Bar / Header** (Lines 180-250)
- Search field with expansion animation for mobile
- Upload icon button
- Create Invoice button
- **Complexity:** Medium - contains responsive behavior

### 2. **Drag & Drop Handler** (Lines 136-177, 300-320)
- Screen-level drop target modifier
- Flying documents state management
- Pending dropped files state
- Space upload overlay integration
- **Complexity:** High - stateful, animation-dependent

### 3. **Summary Cards Section** (Lines 567-614)
- `SummaryCardsRow` - orchestrates three cards:
  - VatSummaryCard (existing component)
  - BusinessHealthCard (existing component)
  - PendingDocumentsCard (existing component)
- **Complexity:** Low - composition of existing components

### 4. **Desktop Documents Table Section** (Lines 620-711)
- `DocumentsTableSection` with loading/success/error states
- `DocumentsTableSkeleton` for loading state
- Uses `FinancialDocumentTable` (existing component)
- **Complexity:** Medium - state handling pattern

### 5. **Mobile Documents List Section** (Lines 477-558)
- `MobileDocumentsSection` with loading/success/error states
- `MobileDocumentsListSkeleton` for loading state
- Uses `FinancialDocumentList` (existing component)
- **Complexity:** Medium - state handling pattern

### 6. **Layout Containers** (Lines 330-471)
- `DesktopCashflowContent` - LazyColumn with infinite scroll
- `MobileCashflowContent` - LazyColumn with infinite scroll
- **Complexity:** Medium - contains pagination logic

### 7. **Shared Utility Components** (Lines 717-745)
- `EmptyDocumentsState`
- `LoadingMoreIndicator`
- **Complexity:** Low - simple UI components

---

## Components That Can Be Extracted

### High Priority Extractions (Will significantly reduce CashflowScreen.kt)

#### 1. **CashflowTopBar.kt** (NEW)
- Extract lines 180-250
- Contains: Search field, upload button, create invoice button
- Props: `searchQuery`, `isSearchExpanded`, `isLargeScreen`, callbacks
- **Estimated lines:** 80-100
- **Reduces main file by:** ~70 lines

#### 2. **CashflowSummarySection.kt** (NEW)
- Extract `SummaryCardsRow` (lines 567-614)
- Contains: Row with VAT, Business Health, Pending Documents cards
- Props: State objects for each card, callbacks
- **Estimated lines:** 60-80
- **Reduces main file by:** ~50 lines

#### 3. **DocumentsTableSection.kt** (NEW)
- Extract `DocumentsTableSection` + `DocumentsTableSkeleton` (lines 620-711)
- Contains: Desktop table with loading/success/error states
- Props: `DokusState<PaginationState<FinancialDocumentDto>>`, callbacks
- **Estimated lines:** 100-120
- **Reduces main file by:** ~90 lines

#### 4. **MobileDocumentsSection.kt** (NEW)
- Extract `MobileDocumentsSection` + `MobileDocumentsListSkeleton` (lines 477-558)
- Contains: Mobile list with loading/success/error states
- Props: `DokusState<PaginationState<FinancialDocumentDto>>`, callbacks
- **Estimated lines:** 90-100
- **Reduces main file by:** ~80 lines

### Medium Priority Extractions

#### 5. **DesktopCashflowContent.kt** (NEW)
- Extract lines 330-406
- Contains: LazyColumn with infinite scroll, section composition
- Props: All section states, content padding, callbacks
- **Estimated lines:** 80-100
- **Reduces main file by:** ~75 lines

#### 6. **MobileCashflowContent.kt** (NEW)
- Extract lines 413-471
- Contains: LazyColumn with infinite scroll, section composition
- Props: Documents state, sort option, callbacks
- **Estimated lines:** 70-80
- **Reduces main file by:** ~60 lines

### Low Priority Extractions (Small components, can stay or move)

#### 7. **SharedCashflowComponents.kt** (NEW)
- Extract shared utilities: `EmptyDocumentsState`, `LoadingMoreIndicator`
- **Estimated lines:** 30-40
- **Reduces main file by:** ~30 lines

---

## Recommended Extraction Strategy

### Phase 1: Extract State-Handling Sections (Highest Impact)
1. **DocumentsTableSection.kt** - Desktop table with skeleton
2. **MobileDocumentsSection.kt** - Mobile list with skeleton
3. **CashflowSummarySection.kt** - Summary cards row

### Phase 2: Extract Layout Components
4. **DesktopCashflowContent.kt** - Desktop LazyColumn layout
5. **MobileCashflowContent.kt** - Mobile LazyColumn layout

### Phase 3: Extract Header Components
6. **CashflowTopBar.kt** or integrate into existing `PTopAppBarSearchAction` component

### Phase 4: Move Shared Components
7. **SharedCashflowComponents.kt** or add to existing shared components

---

## Estimated Post-Refactor Line Counts

| File | Lines (Estimated) |
|------|-------------------|
| CashflowScreen.kt | ~200-250 (from 747) |
| CashflowSummarySection.kt | ~60-80 |
| DocumentsTableSection.kt | ~100-120 |
| MobileDocumentsSection.kt | ~90-100 |
| DesktopCashflowContent.kt | ~80-100 |
| MobileCashflowContent.kt | ~70-80 |
| SharedCashflowComponents.kt | ~30-40 |

**Total lines preserved:** ~630-770 (slight increase due to file structure overhead)
**Main file reduction:** 747 → ~200-250 lines (**66-73% reduction**)

---

## Pattern Reference: BusinessHealthCard.kt

The reference pattern from `BusinessHealthCard.kt` shows:

1. **Data class for state** - `BusinessHealthData` with companion object for defaults
2. **Enum for status** - `HealthStatus` with display properties
3. **Main composable** - Handles `DokusState<T>` with when-expression for Loading/Success/Error
4. **Private content composable** - `BusinessHealthCardContent` for success state
5. **Private skeleton composable** - `BusinessHealthCardSkeleton` for loading state
6. **Private error handling** - `BusinessHealthCardError` for error state
7. **Small focused helpers** - `DonutChart`, `LegendItem`

### Pattern Application to DocumentsTableSection:

```kotlin
/**
 * Desktop documents table with independent state handling.
 * Shows loading skeleton, success content, or error state.
 */
@Composable
fun DocumentsTableSection(
    state: DokusState<PaginationState<FinancialDocumentDto>>,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when (state) {
            is DokusState.Loading, is DokusState.Idle -> {
                DocumentsTableSkeleton()
            }
            is DokusState.Success -> {
                DocumentsTableContent(
                    paginationState = state.data,
                    onDocumentClick = onDocumentClick,
                    onMoreClick = onMoreClick
                )
            }
            is DokusState.Error -> {
                DocumentsTableError(state = state)
            }
        }
    }
}

@Composable
private fun DocumentsTableContent(...) { /* ... */ }

@Composable
private fun DocumentsTableSkeleton(...) { /* ... */ }

@Composable
private fun DocumentsTableError(...) { /* ... */ }
```

---

## Existing Components Already Extracted

The following components are already extracted and used by CashflowScreen:

- `VatSummaryCard` - Handles its own DokusState
- `BusinessHealthCard` - Handles its own DokusState
- `PendingDocumentsCard` - Handles its own DokusState
- `FinancialDocumentTable` - Documents table content
- `FinancialDocumentList` - Documents list content (mobile)
- `SortDropdown` - Sort option selector
- `DocumentUploadSidebar` - Upload sidebar panel
- `AppDownloadQrDialog` - QR code dialog
- `SpaceUploadOverlay` - Drag and drop overlay

---

## Dependencies to Consider

### Imports Required by New Components

```kotlin
// State handling
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.domain.model.FinancialDocumentDto

// Design system
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.ShimmerLine

// Compose
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
```

### Component Relationships

```
CashflowScreen
├── PTopAppBarSearchAction (existing)
│   └── [Search + Actions content - inline]
├── Scaffold
│   └── DesktopCashflowContent (NEW) or MobileCashflowContent (NEW)
│       ├── CashflowSummarySection (NEW) [Desktop only]
│       ├── SortDropdown (existing)
│       └── DocumentsTableSection (NEW) or MobileDocumentsSection (NEW)
├── DocumentUploadSidebar (existing)
├── AppDownloadQrDialog (existing)
└── SpaceUploadOverlay (existing)
```

---

## Next Steps

1. **Create DocumentsTableSection.kt** - Extract desktop table section
2. **Create MobileDocumentsSection.kt** - Extract mobile list section
3. **Create CashflowSummarySection.kt** - Extract summary cards row
4. **Update CashflowScreen.kt** - Replace inline code with component calls
5. **Verify compilation and behavior**
6. **Run tests to ensure zero regression**

---

# Refactor Analysis: ViewModels → Use Cases

## Overview

This section analyzes business logic in ViewModels that should be extracted into dedicated use case classes following the pattern established in `WatchPendingDocumentsUseCase.kt`.

**Pattern Reference:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/WatchPendingDocumentsUseCase.kt`

---

## CreateInvoiceViewModel.kt Analysis

**File:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/viewmodel/CreateInvoiceViewModel.kt`
**Current Size:** 530 lines
**Target Size:** < 300 lines

### Current Business Logic (Candidates for Use Case Extraction)

| Operation | Lines | Description | Priority |
|-----------|-------|-------------|----------|
| Load Clients | 233-244 | Loads contacts/clients for invoice creation | High |
| Filter Clients | 249-259 | Filters clients based on search query | Medium |
| Validate Invoice Form | 432-452 | Validates invoice form data before submission | High |
| Create Invoice / Save Draft | 461-511 | Submits invoice to API and handles response | High |
| Check Peppol Availability | 411-414 | Checks if client supports Peppol delivery | Low |

### Proposed Use Cases to Extract

#### 1. **LoadContactsUseCase** (HIGH PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/LoadContactsUseCase.kt`
- **Current Code:** Lines 233-244
- **Description:** Loads contacts/clients from data source for invoice creation
- **Returns:** `Flow<DokusState<List<ContactDto>>>`
- **Dependencies:** `ContactsRemoteDataSource` (when available)
- **Estimated lines:** 40-50
- **Reduces ViewModel by:** ~15 lines

```kotlin
class LoadContactsUseCase(
    private val dataSource: ContactsRemoteDataSource
) {
    operator fun invoke(): Flow<DokusState<List<ContactDto>>> = flow {
        emit(DokusState.loading())
        dataSource.listContacts().fold(
            onSuccess = { emit(DokusState.success(it)) },
            onFailure = { emit(DokusState.error(it.asDokusException)) }
        )
    }
}
```

#### 2. **ValidateInvoiceUseCase** (HIGH PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/ValidateInvoiceUseCase.kt`
- **Current Code:** Lines 432-452
- **Description:** Validates invoice form state and returns validation errors
- **Returns:** `ValidationResult` (sealed class with Success/Failure)
- **Dependencies:** None (pure validation logic)
- **Estimated lines:** 60-80

```kotlin
class ValidateInvoiceUseCase {
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Failure(val errors: Map<String, String>) : ValidationResult()
    }

    operator fun invoke(formState: CreateInvoiceFormState): ValidationResult {
        val errors = mutableMapOf<String, String>()

        if (formState.selectedClient == null) {
            errors["client"] = "Please select a client"
        }
        if (!formState.items.any { it.isValid }) {
            errors["items"] = "Please add at least one valid line item"
        }
        // ... rest of validation

        return if (errors.isEmpty()) ValidationResult.Success
               else ValidationResult.Failure(errors)
    }
}
```

#### 3. **SubmitInvoiceUseCase** (HIGH PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/SubmitInvoiceUseCase.kt`
- **Current Code:** Lines 461-511
- **Description:** Submits invoice to backend and handles response
- **Returns:** `Flow<DokusState<FinancialDocumentDto.InvoiceDto>>`
- **Dependencies:** `CashflowRemoteDataSource`, `ValidateInvoiceUseCase`
- **Estimated lines:** 70-90

```kotlin
class SubmitInvoiceUseCase(
    private val dataSource: CashflowRemoteDataSource,
    private val validateInvoice: ValidateInvoiceUseCase
) {
    operator fun invoke(
        formState: CreateInvoiceFormState
    ): Flow<DokusState<FinancialDocumentDto.InvoiceDto>> = flow {
        // Validate first
        val validation = validateInvoice(formState)
        if (validation is ValidateInvoiceUseCase.ValidationResult.Failure) {
            emit(DokusState.error(DokusException.Validation("Invalid form")))
            return@flow
        }

        emit(DokusState.loading())

        val request = buildCreateInvoiceRequest(formState)
        dataSource.createInvoice(request).fold(
            onSuccess = { emit(DokusState.success(it)) },
            onFailure = { emit(DokusState.error(it.asDokusException) { /* retry */ }) }
        )
    }

    private fun buildCreateInvoiceRequest(form: CreateInvoiceFormState): CreateInvoiceRequest {
        // Extract request building logic from ViewModel
    }
}
```

#### 4. **FilterContactsUseCase** (MEDIUM PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/FilterContactsUseCase.kt`
- **Current Code:** Lines 249-259
- **Description:** Filters contacts list based on search query
- **Returns:** `List<ContactDto>`
- **Dependencies:** None (pure function)
- **Estimated lines:** 25-35

```kotlin
class FilterContactsUseCase {
    operator fun invoke(contacts: List<ContactDto>, query: String): List<ContactDto> {
        val trimmedQuery = query.trim().lowercase()
        if (trimmedQuery.isBlank()) return contacts

        return contacts.filter { client ->
            client.name.value.lowercase().contains(trimmedQuery) ||
            client.email?.value?.lowercase()?.contains(trimmedQuery) == true ||
            client.vatNumber?.value?.lowercase()?.contains(trimmedQuery) == true
        }
    }
}
```

### What Should Stay in CreateInvoiceViewModel

| Responsibility | Lines | Reason |
|---------------|-------|--------|
| UI State Management | 201-227 | Pure UI state (expanded items, panels, date pickers) |
| Form State Updates | 266-398 | Simple state updates triggered by UI events |
| Line Item Management | 333-378 | UI-driven CRUD operations on form state |
| Delivery Method Selection | 404-426 | UI state for delivery selection |
| Mobile Navigation | 420-426 | Step navigation state |

### Post-Refactor ViewModel Structure

```kotlin
class CreateInvoiceViewModel(
    private val loadContacts: LoadContactsUseCase,
    private val filterContacts: FilterContactsUseCase,
    private val validateInvoice: ValidateInvoiceUseCase,
    private val submitInvoice: SubmitInvoiceUseCase
) : BaseViewModel<...>(), KoinComponent {

    // States (unchanged)
    private val _clientsState = MutableStateFlow<DokusState<List<ContactDto>>>(DokusState.idle())
    private val _uiState = MutableStateFlow(CreateInvoiceUiState())
    private val _formState = MutableStateFlow(createInitialFormState())

    // Delegated operations
    fun loadClients() = scope.launch { loadContacts().collect { _clientsState.value = it } }
    fun getFilteredClients() = filterContacts(clientsState.value.dataOrNull ?: emptyList(), uiState.value.clientSearchQuery)
    fun saveAsDraft() = scope.launch { submitInvoice(formState.value).collect { ... } }

    // UI operations stay in ViewModel (panels, pickers, line items, etc.)
}
```

---

## CashflowViewModel.kt Analysis

**File:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/viewmodel/CashflowViewModel.kt`
**Current Size:** 415 lines
**Target Size:** < 300 lines

### Already Extracted Use Cases ✅

| Use Case | Location | Status |
|----------|----------|--------|
| `WatchPendingDocumentsUseCase` | `usecase/WatchPendingDocumentsUseCase.kt` | ✅ Already extracted |
| `SearchCashflowDocumentsUseCase` | `usecase/SearchCashflowDocumentsUseCase.kt` | ✅ Already extracted |

### Current Business Logic (Candidates for Use Case Extraction)

| Operation | Lines | Description | Priority |
|-----------|-------|-------------|----------|
| Load Paginated Documents | 322-356 | Loads paginated cashflow documents from API | High |
| Sort Documents | 160-181 | Sorts documents by various criteria | Medium |
| Load VAT Summary | 261-273 | Loads VAT summary data | Medium |
| Load Business Health | 279-291 | Loads business health metrics | Medium |
| Refresh All Data | 227-254 | Orchestrates parallel refresh of all sections | Low |

### Proposed Use Cases to Extract

#### 1. **LoadCashflowDocumentsUseCase** (HIGH PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/LoadCashflowDocumentsUseCase.kt`
- **Current Code:** Lines 322-356
- **Description:** Loads paginated cashflow documents from remote data source
- **Returns:** `Flow<DokusState<PaginationResult<FinancialDocumentDto>>>`
- **Dependencies:** `CashflowRemoteDataSource`
- **Estimated lines:** 60-80

```kotlin
class LoadCashflowDocumentsUseCase(
    private val dataSource: CashflowRemoteDataSource
) {
    data class PaginationParams(
        val page: Int = 0,
        val pageSize: Int = 20
    )

    suspend operator fun invoke(
        params: PaginationParams
    ): Result<PaginatedResponse<FinancialDocumentDto>> {
        val offset = params.page * params.pageSize
        return dataSource.listCashflowDocuments(
            limit = params.pageSize,
            offset = offset
        )
    }
}
```

#### 2. **SortDocumentsUseCase** (MEDIUM PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/SortDocumentsUseCase.kt`
- **Current Code:** Lines 160-181
- **Description:** Sorts document list based on selected sort option
- **Returns:** `List<FinancialDocumentDto>`
- **Dependencies:** None (pure function)
- **Estimated lines:** 40-50

```kotlin
class SortDocumentsUseCase {
    operator fun invoke(
        documents: List<FinancialDocumentDto>,
        sortOption: DocumentSortOption
    ): List<FinancialDocumentDto> {
        return when (sortOption) {
            DocumentSortOption.Default -> documents
            DocumentSortOption.DateNewest -> documents.sortedByDescending { it.date }
            DocumentSortOption.DateOldest -> documents.sortedBy { it.date }
            DocumentSortOption.AmountHighest -> documents.sortedByDescending {
                it.amount.value.toDoubleOrNull() ?: 0.0
            }
            DocumentSortOption.AmountLowest -> documents.sortedBy {
                it.amount.value.toDoubleOrNull() ?: 0.0
            }
            DocumentSortOption.Type -> documents.sortedBy { document ->
                when (document) {
                    is FinancialDocumentDto.InvoiceDto -> 0
                    is FinancialDocumentDto.ExpenseDto -> 1
                    is FinancialDocumentDto.BillDto -> 2
                }
            }
        }
    }
}
```

#### 3. **LoadVatSummaryUseCase** (MEDIUM PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/LoadVatSummaryUseCase.kt`
- **Current Code:** Lines 261-273
- **Description:** Loads VAT summary data from remote data source
- **Returns:** `Flow<DokusState<VatSummaryData>>`
- **Dependencies:** `CashflowRemoteDataSource` (when endpoint available)
- **Estimated lines:** 35-45

```kotlin
class LoadVatSummaryUseCase(
    private val dataSource: CashflowRemoteDataSource
) {
    operator fun invoke(): Flow<DokusState<VatSummaryData>> = flow {
        emit(DokusState.loading())
        // TODO: Replace with actual API call when endpoint available
        emit(DokusState.success(VatSummaryData.empty))
    }
}
```

#### 4. **LoadBusinessHealthUseCase** (MEDIUM PRIORITY)
- **Location:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/usecase/LoadBusinessHealthUseCase.kt`
- **Current Code:** Lines 279-291
- **Description:** Loads business health metrics from remote data source
- **Returns:** `Flow<DokusState<BusinessHealthData>>`
- **Dependencies:** `CashflowRemoteDataSource` (when endpoint available)
- **Estimated lines:** 35-45

```kotlin
class LoadBusinessHealthUseCase(
    private val dataSource: CashflowRemoteDataSource
) {
    operator fun invoke(): Flow<DokusState<BusinessHealthData>> = flow {
        emit(DokusState.loading())
        // TODO: Replace with actual API call when endpoint available
        emit(DokusState.success(BusinessHealthData.empty))
    }
}
```

### What Should Stay in CashflowViewModel

| Responsibility | Lines | Reason |
|---------------|-------|--------|
| Sidebar State | 131-149 | Pure UI state (open/close sidebar) |
| QR Dialog State | 141-147 | Pure UI state (show/hide dialog) |
| Sort Option State | 52-53, 155-158 | UI preference state |
| Search Query State | 47-49, 306-320 | Search UI coordination |
| Upload Manager Delegation | 63-67, 149 | Exposes upload manager state |
| Pending Documents Pagination UI | 195-221 | UI-driven pagination state |

### Post-Refactor ViewModel Structure

```kotlin
internal class CashflowViewModel(
    private val loadCashflowDocuments: LoadCashflowDocumentsUseCase,
    private val sortDocuments: SortDocumentsUseCase,
    private val loadVatSummary: LoadVatSummaryUseCase,
    private val loadBusinessHealth: LoadBusinessHealthUseCase,
    private val searchDocuments: SearchCashflowDocumentsUseCase, // already extracted
    private val watchPendingDocuments: WatchPendingDocumentsUseCase, // already extracted
    private val uploadManager: DocumentUploadManager
) : BaseViewModel<...>(), KoinComponent {

    // States (simplified)
    private val _vatSummaryState = MutableStateFlow<DokusState<VatSummaryData>>(DokusState.loading())
    private val _businessHealthState = MutableStateFlow<DokusState<BusinessHealthData>>(DokusState.loading())

    // Delegated operations
    fun refresh() {
        scope.launch {
            coroutineScope {
                launch { loadVatSummary().collect { _vatSummaryState.value = it } }
                launch { loadBusinessHealth().collect { _businessHealthState.value = it } }
                launch { loadDocumentsPage(0, reset = true) }
            }
        }
    }

    private suspend fun loadDocumentsPage(page: Int, reset: Boolean) {
        loadCashflowDocuments(PaginationParams(page)).fold(
            onSuccess = { /* update state */ },
            onFailure = { /* handle error */ }
        )
    }

    // UI operations stay in ViewModel
}
```

---

## Use Case Extraction Summary

### CreateInvoiceViewModel Use Cases to Create

| Use Case | Priority | Est. Lines | Dependency |
|----------|----------|------------|------------|
| `ValidateInvoiceUseCase` | HIGH | 60-80 | None |
| `SubmitInvoiceUseCase` | HIGH | 70-90 | CashflowRemoteDataSource, ValidateInvoiceUseCase |
| `LoadContactsUseCase` | HIGH | 40-50 | ContactsRemoteDataSource |
| `FilterContactsUseCase` | MEDIUM | 25-35 | None |

### CashflowViewModel Use Cases to Create

| Use Case | Priority | Est. Lines | Dependency |
|----------|----------|------------|------------|
| `LoadCashflowDocumentsUseCase` | HIGH | 60-80 | CashflowRemoteDataSource |
| `SortDocumentsUseCase` | MEDIUM | 40-50 | None |
| `LoadVatSummaryUseCase` | MEDIUM | 35-45 | CashflowRemoteDataSource |
| `LoadBusinessHealthUseCase` | MEDIUM | 35-45 | CashflowRemoteDataSource |

### Already Extracted ✅

- `WatchPendingDocumentsUseCase`
- `SearchCashflowDocumentsUseCase`

---

## Estimated Post-Refactor Line Counts

| File | Current | Target | Reduction |
|------|---------|--------|-----------|
| CreateInvoiceViewModel.kt | 530 | ~250-280 | ~47-53% |
| CashflowViewModel.kt | 415 | ~250-280 | ~32-40% |

### New Use Case Files

| File | Est. Lines |
|------|------------|
| ValidateInvoiceUseCase.kt | 60-80 |
| SubmitInvoiceUseCase.kt | 70-90 |
| LoadContactsUseCase.kt | 40-50 |
| FilterContactsUseCase.kt | 25-35 |
| LoadCashflowDocumentsUseCase.kt | 60-80 |
| SortDocumentsUseCase.kt | 40-50 |
| LoadVatSummaryUseCase.kt | 35-45 |
| LoadBusinessHealthUseCase.kt | 35-45 |

---

## DI Registration (Koin)

All new use cases need to be registered in `CashflowPresentationModule.kt`:

```kotlin
val cashflowPresentationModule = module {
    // Existing
    factory { WatchPendingDocumentsUseCase(dataSource = get()) }
    factory { SearchCashflowDocumentsUseCase() }

    // New - CreateInvoice
    factory { ValidateInvoiceUseCase() }
    factory { SubmitInvoiceUseCase(dataSource = get(), validateInvoice = get()) }
    factory { LoadContactsUseCase(dataSource = get()) }
    factory { FilterContactsUseCase() }

    // New - Cashflow
    factory { LoadCashflowDocumentsUseCase(dataSource = get()) }
    factory { SortDocumentsUseCase() }
    factory { LoadVatSummaryUseCase(dataSource = get()) }
    factory { LoadBusinessHealthUseCase(dataSource = get()) }

    // ViewModels
    viewModel { CreateInvoiceViewModel() }
    viewModel { CashflowViewModel() }
}
```

---

## Next Steps for ViewModel Refactoring

1. **Create ValidateInvoiceUseCase.kt** - Extract validation logic
2. **Create SubmitInvoiceUseCase.kt** - Extract invoice submission
3. **Create LoadCashflowDocumentsUseCase.kt** - Extract document loading
4. **Create SortDocumentsUseCase.kt** - Extract sorting logic
5. **Update DI module** - Register new use cases
6. **Refactor CreateInvoiceViewModel** - Inject and delegate to use cases
7. **Refactor CashflowViewModel** - Inject and delegate to use cases
8. **Run tests** - Verify zero regression

---

# Refactor Analysis: Upload Components

## Overview

This section analyzes SpaceUploadOverlay.kt and DocumentUploadItem.kt for decomposition opportunities.

---

## SpaceUploadOverlay.kt Analysis

**File:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/components/SpaceUploadOverlay.kt`
**Current Size:** 519 lines
**Pattern Reference:** `VatSummaryCard.kt` (well-structured component with state handling)

### Current Structure Analysis

| Composable | Lines | Visibility | Description |
|------------|-------|------------|-------------|
| `SpaceUploadOverlay` | 81-141 (~60 lines) | public | Main entry with animation, visibility, child composition |
| `BlackHoleVortex` | 146-352 (~207 lines) | private | Complex Canvas animation with accretion disk, particles |
| `DropZonePrompt` | 357-395 (~38 lines) | private | Simple text overlay with pulsing animation |
| `GravitationalDocumentsLayer` | 400-518 (~118 lines) | private | Canvas for flying documents with physics |

### Data Classes

| Class | Lines | Visibility | Description |
|-------|-------|------------|-------------|
| `FlyingDocument` | 55-62 | public | Animation state for documents entering the vortex |
| `AccretionParticle` | 67-74 | private | Particle data for black hole animation |

### Decomposition Assessment

**✅ ALREADY WELL-STRUCTURED - NO EXTRACTION NEEDED**

The file already follows best practices:

1. **Clear separation of concerns:**
   - Main composable handles visibility and composition
   - Each private composable has a single responsibility
   - Data classes are appropriately scoped

2. **Private helpers pattern:**
   - `BlackHoleVortex` - visual effect (Canvas-based)
   - `DropZonePrompt` - user guidance (Text-based)
   - `GravitationalDocumentsLayer` - document animation (Canvas-based)

3. **Complexity is inherent:**
   - The 207-line `BlackHoleVortex` is complex due to physics-based animations
   - Canvas drawing requires multiple draw calls for visual effects
   - Splitting would reduce readability without improving maintainability

### Why No Extraction is Recommended

| Concern | Assessment |
|---------|------------|
| File length (519 lines) | Within acceptable range for animation-heavy components |
| Cohesion | HIGH - all composables work together for single feature |
| Coupling | LOW - private composables have clear contracts |
| Reusability | LOW - these are specific to upload overlay feature |
| Testability | OK - main composable can be snapshot tested |

### Potential Future Extractions (Low Priority)

If the file grows significantly in the future, consider:

1. **BlackHoleAnimation.kt** - Extract `BlackHoleVortex` + `AccretionParticle`
   - Would make sense if animation is reused elsewhere
   - Currently tightly coupled to upload overlay

2. **GravitationalPhysics.kt** - Extract physics calculations
   - Only if physics logic becomes more complex
   - Currently inline in Canvas draw scope

---

## DocumentUploadItem.kt Analysis

**File:** `features/cashflow/presentation/src/commonMain/kotlin/ai/dokus/app/cashflow/components/DocumentUploadItem.kt`
**Current Size:** 493 lines
**Pattern Reference:** `VatSummaryCard.kt` (state handling pattern)

### Current Structure Analysis

| Composable | Lines | Visibility | Description |
|------------|-------|------------|-------------|
| `DocumentUploadItem` | 63-84 (~22 lines) | public | Entry point, creates state, delegates to content |
| `DocumentUploadItemContent` | 89-160 (~72 lines) | private | AnimatedContent with state dispatch |
| `PendingContent` | 164-196 (~33 lines) | private | Waiting in queue state |
| `UploadingContent` | 198-248 (~51 lines) | private | In progress with progress bar |
| `FailedContent` | 250-305 (~56 lines) | private | Error state with retry/cancel |
| `UploadedContent` | 307-350 (~44 lines) | private | Success state with delete action |
| `DeletingContent` | 352-425 (~74 lines) | private | Deletion countdown with undo |
| `UploadItemRow` | 429-474 (~46 lines) | private | Shared row layout component |
| `FileIconWithOverlay` | 476-492 (~17 lines) | private | Shared icon with overlay slot |

### Pattern Conformance

**✅ EXCELLENT PATTERN ADHERENCE - NO EXTRACTION NEEDED**

Follows VatSummaryCard.kt pattern exactly:

1. **Main composable** - Simple entry point with state creation
2. **Content composable** - Handles state dispatch with `when` expression
3. **State-specific composables** - Each state has dedicated private composable
4. **Shared components** - Reusable primitives (`UploadItemRow`, `FileIconWithOverlay`)

### State Machine Visualization

```
┌─────────┐    ┌───────────┐    ┌──────────┐
│ Pending │───▶│ Uploading │───▶│ Uploaded │
└─────────┘    └───────────┘    └──────────┘
     │              │                 │
     │              ▼                 ▼
     │         ┌────────┐       ┌──────────┐
     └────────▶│ Failed │       │ Deleting │
               └────────┘       └──────────┘
                   │                 │
                   └────(retry)──────┘
```

### Why No Extraction is Recommended

| Concern | Assessment |
|---------|------------|
| File length (493 lines) | Acceptable for 5-state component |
| State handling | Excellent - AnimatedContent with proper transitions |
| Shared components | Already extracted within file |
| Single responsibility | Each composable does one thing well |
| Testability | HIGH - each state can be snapshot tested |

### Code Quality Highlights

1. **AnimatedContent transitions** (lines 103-128):
   - Custom transitions per state change type
   - Uses `contentKey` to avoid animating on progress updates

2. **Shared layout pattern** (UploadItemRow):
   - Slot-based design with icon/actions slots
   - Optional subtitle content slot

3. **State encapsulation**:
   - Uses `DocumentUploadItemState` for actions (retry, cancel, delete)
   - Clean separation between UI and state management

---

## Upload Components Summary

### Files Analyzed

| File | Lines | Status | Recommendation |
|------|-------|--------|----------------|
| SpaceUploadOverlay.kt | 519 | ✅ Well-structured | No extraction needed |
| DocumentUploadItem.kt | 493 | ✅ Excellent patterns | No extraction needed |

### Key Observations

1. **Both files follow the established patterns:**
   - Main public composable as entry point
   - Private composables for sub-components
   - Clear state handling with `when` expressions
   - Shared utility composables at file bottom

2. **Complexity is justified:**
   - SpaceUploadOverlay: Animation complexity requires Canvas code
   - DocumentUploadItem: 5 states with transitions require explicit handling

3. **No immediate refactoring needed:**
   - Both files are within acceptable line counts
   - Code is well-organized and maintainable
   - No duplication that warrants extraction

### Comparison with VatSummaryCard.kt Pattern

| Aspect | VatSummaryCard | SpaceUploadOverlay | DocumentUploadItem |
|--------|----------------|--------------------|--------------------|
| Entry composable | ✅ | ✅ | ✅ |
| State dispatch | ✅ when(state) | ✅ when(isDragging) | ✅ AnimatedContent |
| Private helpers | ✅ 5 functions | ✅ 3 functions | ✅ 8 functions |
| Data classes | ✅ VatSummaryData | ✅ FlyingDocument | ❌ Uses sealed class |
| Skeleton state | ✅ | ❌ N/A | ✅ implicit in Pending |
| Error state | ✅ | ❌ N/A | ✅ FailedContent |

---

## Recommendations for Future Development

### SpaceUploadOverlay.kt

1. If adding new animation effects, consider extracting to `animations/` subpackage
2. If particles logic grows, extract to separate `AccretionDiskAnimation.kt`
3. Keep Canvas drawing together for readability

### DocumentUploadItem.kt

1. If adding new states, follow existing pattern (add private XxxContent composable)
2. Consider extracting `DocumentUploadDisplayState` transitions to separate file if they grow
3. `UploadItemRow` could be promoted to shared components if needed elsewhere

---

## No Action Items for This Phase

Both upload components are **already refactored** according to project patterns. They serve as **reference implementations** for how to structure complex Compose components:

- **SpaceUploadOverlay.kt** - Example of animation-heavy component
- **DocumentUploadItem.kt** - Example of multi-state component with transitions

These files can be used as patterns for future refactoring work on other components
