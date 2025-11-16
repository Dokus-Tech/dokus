# Cashflow Module Implementation Plan

**Status:** 🚧 In Progress
**Started:** 2025-11-16
**Goal:** Merge invoicing + expense modules into unified cashflow module with document upload

---

## 📊 Architecture Overview

### Backend Consolidation
```
BEFORE:
├── features/expense/backend     (Port: 7093, DB: 5543) ❌ DELETE
├── features/invoicing/backend   (Port: 7092, DB: 5542) ❌ DELETE

AFTER:
└── features/cashflow/backend    (Port: 7098, DB: 5548) ✅ NEW
    ├── invoices/
    ├── expenses/
    └── documents/
```

### RPC API Strategy
- ✅ Single `CashflowApi` interface with all operations
- 📦 Modular service implementations (InvoiceService, ExpenseService, DocumentService)
- 🔒 All operations filtered by `tenant_id` for security

---

## ✅ Phase 0: Foundation (COMPLETED)

- [x] Create `CashflowApi.kt` RPC interface in `foundation/domain/rpc/`
- [x] Create `features/cashflow/backend/build.gradle.kts`
- [x] Register in `settings.gradle.kts`
- [x] Invoice/Expense domain models exist in `Financial.kt`

---

## 🚀 Phase 1: Backend MVP (IN PROGRESS)

### 1.1 Application Setup
- [ ] Create `Application.kt` (main entry point)
- [ ] Create `application.conf` (configuration)
- [ ] Create `DependencyInjection.kt` (Koin modules)
- [ ] Create `DatabaseFactory.kt` (database connection)

### 1.2 Database Schema
- [ ] Create `InvoicesTable.kt` (Exposed table)
- [ ] Create `InvoiceItemsTable.kt`
- [ ] Create `ExpensesTable.kt`
- [ ] Create `AttachmentsTable.kt` (for documents)
- [ ] Create Flyway migration: `V1__create_cashflow_tables.sql`
- [ ] Create Flyway migration: `V2__create_attachments_table.sql`

**Critical Security:**
```sql
-- All tables MUST have tenant_id with index
CREATE INDEX idx_invoices_tenant ON invoices(tenant_id);
CREATE INDEX idx_attachments_tenant_entity ON attachments(tenant_id, entity_type, entity_id);
```

### 1.3 Repository Layer
- [ ] Create `InvoiceRepository.kt`
  - `createInvoice()`
  - `getInvoice()` - MUST filter by tenant_id
  - `listInvoices()` - MUST filter by tenant_id
  - `updateInvoice()`
  - `deleteInvoice()` (soft delete)

- [ ] Create `ExpenseRepository.kt`
  - `createExpense()`
  - `getExpense()` - MUST filter by tenant_id
  - `listExpenses()` - MUST filter by tenant_id
  - `updateExpense()`
  - `deleteExpense()`

- [ ] Create `AttachmentRepository.kt`
  - `uploadAttachment()`
  - `getAttachments()` - MUST filter by tenant_id
  - `deleteAttachment()` - MUST filter by tenant_id

### 1.4 Service Layer
- [ ] Create `InvoiceService.kt`
  - Implements invoice operations
  - Calculates totals (subtotal + VAT)
  - Handles status transitions

- [ ] Create `ExpenseService.kt`
  - Implements expense operations
  - Auto-categorization logic

- [ ] Create `DocumentStorageService.kt`
  - File upload to local storage (dev) / S3 (prod)
  - File validation (size, type)
  - Thumbnail generation
  - Presigned URL generation

### 1.5 RPC Implementation
- [ ] Create `CashflowApiImpl.kt`
  - Implements `CashflowApi` interface
  - Delegates to service layer
  - Wraps responses in `Result<T>`

### 1.6 Deployment
- [ ] Create `Dockerfile.dev`
- [ ] Create `application.conf` (local config)
- [ ] Create `application-cloud.conf` (cloud config)

---

## 🐳 Phase 2: Deployment Scripts

### 2.1 Update dev.sh
- [ ] Add `cashflow` to services array (line ~293)
- [ ] Add to `DB_KEYS` variable (line ~99): `auth invoicing expense payment reporting audit banking cashflow`
- [ ] Add `get_db_config` case:
  ```bash
  cashflow) echo "postgres-cashflow-local:5548:dokus_cashflow" ;;
  ```
- [ ] Update health check URLs (add port 7098)
- [ ] Update service info display
- [ ] Remove invoicing and expense entries (after migration)

### 2.2 Update docker-compose.yml
- [ ] Add PostgreSQL container:
  ```yaml
  postgres-cashflow-local:
    image: postgres:16-alpine
    container_name: postgres-cashflow-local
    environment:
      POSTGRES_DB: dokus_cashflow
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: devpassword
    ports:
      - "5548:5432"
  ```

- [ ] Add cashflow service container:
  ```yaml
  dokus-cashflow-local:
    build:
      context: ../..
      dockerfile: features/cashflow/backend/Dockerfile.dev
    container_name: dokus-cashflow-local
    ports:
      - "7098:8000"
      - "5015:5005"  # Debug port
    environment:
      DB_HOST: postgres-cashflow-local
      DB_PORT: 5432
      DB_NAME: dokus_cashflow
      DB_USER: dev
      DB_PASSWORD: devpassword
      REDIS_HOST: redis-local
      REDIS_PORT: 6379
      REDIS_PASSWORD: devredispass
      JWT_SECRET: ${JWT_SECRET}
      ENVIRONMENT: local
    depends_on:
      - postgres-cashflow-local
      - redis-local
      - rabbitmq-local
  ```

### 2.3 Update dokus.sh (production)
- [ ] Same changes as dev.sh
- [ ] Update service list
- [ ] Update database configuration

---

## 🎨 Phase 3: Frontend Module

### 3.1 Module Setup
- [ ] Create `application/cashflow/` directory structure:
  ```
  application/cashflow/
  ├── screens/
  │   ├── CashflowScreen.kt
  │   └── InvoiceDetailsScreen.kt
  ├── components/
  │   ├── InvoiceCard.kt
  │   └── DocumentUploadZone.kt
  ├── viewmodel/
  │   └── CashflowViewModel.kt
  └── di/
      └── CashflowModule.kt
  ```

- [ ] Create `build.gradle.kts` for cashflow presentation module
- [ ] Register in `settings.gradle.kts`

### 3.2 Navigation
- [ ] Update `HomeDestination.kt`:
  ```kotlin
  @Serializable
  @SerialName("cashflow")
  data object Cashflow : HomeDestination
  ```

- [ ] Update `AppDestination.kt`:
  ```kotlin
  @Serializable
  @SerialName("cashflow/invoice_details")
  data class InvoiceDetails(val invoiceId: String) : AppDestination
  ```

- [ ] Create `CashflowNavigationProvider.kt`
- [ ] Create `CashflowAppModule.kt` implementing `AppModule`
- [ ] Register in `AppModules.kt`

### 3.3 String Resources
- [ ] Add to `composeResources/values/strings.xml`:
  ```xml
  <string name="home_cashflow">Cashflow</string>
  <string name="cashflow_invoices">Invoices</string>
  <string name="cashflow_expenses">Expenses</string>
  <string name="invoice_new">New Invoice</string>
  <string name="invoice_upload">Upload Document</string>
  ```

---

## 📱 Phase 4: MVP UI Implementation

### 4.1 Main Screen (List View)
- [ ] Create `CashflowScreen.kt`
  - Tabs: Invoices / Expenses
  - LazyColumn with invoice cards
  - FAB for "New Invoice"
  - Pull-to-refresh
  - Search/filter bar

- [ ] Create `InvoiceCard.kt` component:
  ```kotlin
  @Composable
  fun InvoiceCard(
      invoice: Invoice,
      onClick: () -> Unit
  ) {
      // Display: number, client, amount, status, date
  }
  ```

- [ ] Create `CashflowViewModel.kt`:
  ```kotlin
  sealed class CashflowState {
      object Loading : CashflowState()
      data class Success(val invoices: List<Invoice>) : CashflowState()
      data class Error(val message: String) : CashflowState()
  }
  ```

### 4.2 Details Panel (Secondary Navigation)
- [ ] Create `InvoiceDetailsScreen.kt`
  - Read-only invoice view
  - Show all details, items, documents
  - Edit/Delete actions
  - Use `SetupSecondaryPanel(SecondaryPanelType.Complimentary)`

- [ ] Responsive behavior:
  - Large screens: Show in secondary panel
  - Small screens: Navigate to full screen

### 4.3 Document Upload (Basic)
- [ ] Create `DocumentUploadZone.kt`:
  - File picker button
  - Display selected file name
  - Upload progress indicator
  - Error handling

**Defer to Phase 5:**
- Drag & drop
- Multiple files
- Preview thumbnails
- Camera integration (mobile)

---

## 🧹 Phase 5: Cleanup Old Modules

**⚠️ Only after cashflow is fully working!**

### 5.1 Backend Cleanup
- [ ] Verify cashflow service handles all invoice operations
- [ ] Verify cashflow service handles all expense operations
- [ ] Remove `features/invoicing/backend/` directory
- [ ] Remove `features/expense/backend/` directory
- [ ] Remove from `settings.gradle.kts`

### 5.2 RPC Cleanup
- [ ] Remove `InvoiceApi.kt` (deprecated by `CashflowApi`)
- [ ] Remove `ExpenseApi.kt` (deprecated by `CashflowApi`)
- [ ] Update all RPC client usages to use `CashflowApi`

### 5.3 Deployment Cleanup
- [ ] Remove invoicing service from `dev.sh`
- [ ] Remove expense service from `dev.sh`
- [ ] Remove from `docker-compose.yml`
- [ ] Remove from `dokus.sh`
- [ ] Remove database containers (or mark for deletion after backup)

### 5.4 Documentation
- [ ] Update README.md
- [ ] Update deployment guide
- [ ] Document migration path (if needed for existing data)

---

## 🚀 Phase 6: Advanced Features (Post-MVP)

### 6.1 Enhanced Document Upload
- [ ] Drag & drop support (desktop)
- [ ] Multiple file upload (up to 5 files)
- [ ] Image preview thumbnails
- [ ] PDF preview
- [ ] Camera integration (mobile)
- [ ] OCR for data extraction

### 6.2 Invoice Form Enhancements
- [ ] Line items table editor
- [ ] Auto-calculate totals
- [ ] Client autocomplete
- [ ] Template system
- [ ] Duplicate invoice feature
- [ ] Recurring invoices

### 6.3 Smart Features
- [ ] Auto-save drafts
- [ ] Real-time collaboration
- [ ] Email integration
- [ ] Payment link generation
- [ ] Overdue notifications

---

## 📋 Current Sprint (Today)

### Immediate Tasks:
1. ✅ Create this execution plan (DONE)
2. ⏳ Create `Application.kt`
3. ⏳ Create database tables
4. ⏳ Create basic RPC implementation
5. ⏳ Create Dockerfile
6. ⏳ Update deployment scripts
7. ⏳ Test backend startup

**Target:** Backend service running and responding to health checks

---

## 🎯 Success Criteria

### Phase 1 Complete:
- [ ] `./dev.sh start` successfully starts cashflow service
- [ ] Health check: `curl http://localhost:7098/health` returns 200
- [ ] Database tables created via Flyway
- [ ] RPC endpoint accessible
- [ ] Can create a test invoice via RPC

### MVP Complete:
- [ ] Can view list of invoices in UI
- [ ] Can click invoice → shows details in secondary panel
- [ ] Can upload one document to an invoice
- [ ] Can create basic invoice (no line items)
- [ ] All operations scoped by tenant_id

### Full Feature Complete:
- [ ] Old invoicing/expense modules removed
- [ ] All tests passing
- [ ] Production deployment successful
- [ ] Documentation updated

---

## 🔐 Security Checklist

**CRITICAL - Verify before ANY deployment:**

- [ ] All database queries filter by `tenant_id`
- [ ] Document access checks `tenant_id`
- [ ] File uploads validated (type, size)
- [ ] No SQL injection vulnerabilities
- [ ] JWT authentication on all RPC endpoints
- [ ] File storage isolated by tenant
- [ ] Audit logging for all mutations
- [ ] Rate limiting configured

---

## 📊 Progress Tracking

| Phase | Status | Started | Completed | Notes |
|-------|--------|---------|-----------|-------|
| Phase 0: Foundation | ✅ Done | 2025-11-16 | 2025-11-16 | RPC API & build setup |
| Phase 1: Backend MVP | 🚧 In Progress | 2025-11-16 | - | Application.kt next |
| Phase 2: Deployment | ⏸️ Pending | - | - | After Phase 1 |
| Phase 3: Frontend Module | ⏸️ Pending | - | - | After Phase 2 |
| Phase 4: MVP UI | ⏸️ Pending | - | - | After Phase 3 |
| Phase 5: Cleanup | ⏸️ Pending | - | - | After Phase 4 tested |
| Phase 6: Advanced | 📅 Future | - | - | Post-MVP |

---

## 🐛 Known Issues / Blockers

_None yet - will update as we encounter issues_

---

## 📝 Notes

- Using modular service approach (InvoiceService, ExpenseService, DocumentService)
- Document storage: local filesystem for dev, S3 for production
- All money amounts use `BigDecimal` - NEVER Float!
- Following existing Dokus patterns (Koin DI, Exposed ORM, KotlinX RPC)
- Secondary panel for details view (like Pulse project)

---

**Last Updated:** 2025-11-16
**Next Review:** After Phase 1 completion
