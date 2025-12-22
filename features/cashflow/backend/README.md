# Cashflow Service

Financial document management service for invoices, expenses, bills, and cashflow tracking.

## Overview

The Cashflow service is the core financial document management system for the Dokus platform, handling:

- **Invoices**: Outgoing invoices to clients (Cash-In)
- **Expenses**: Direct purchases and receipts (Cash-Out)
- **Bills**: Incoming supplier invoices (Cash-Out)
- **Attachments**: Document storage for receipts and invoices
- **Peppol Integration**: European e-invoicing network for sending/receiving electronic invoices
- **Cashflow Analytics**: Aggregated financial overview and reporting

The service supports multi-tenant architecture with strict data isolation.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         REST API Layer                                   │
│  InvoiceRoutes │ ExpenseRoutes │ BillRoutes │ AttachmentRoutes │ Peppol │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────────┐   ┌───────────────────┐   ┌───────────────────┐
│   InvoiceService  │   │   ExpenseService  │   │    BillService    │
│   (Cash-In)       │   │   (Cash-Out)      │   │   (Cash-Out)      │
└─────────┬─────────┘   └─────────┬─────────┘   └─────────┬─────────┘
          │                       │                       │
          └───────────────────────┼───────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Foundation Repositories                               │
│  InvoiceRepository │ ExpenseRepository │ BillRepository │ ContactRepo   │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        PostgreSQL Database                               │
│  InvoicesTable │ ExpensesTable │ BillsTable │ DocumentsTable │ Peppol  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Services

| Service | Purpose | Key Operations |
|---------|---------|----------------|
| `InvoiceService` | Manages outgoing invoices to clients | Create, update, list, status changes, overdue tracking |
| `ExpenseService` | Handles direct expenses and receipts | Create, update, list, auto-categorization |
| `BillService` | Manages incoming supplier bills | Create, update, payment tracking, Peppol import |
| `CashflowOverviewService` | Aggregates financial data | Period summaries, cash-in/out calculations |
| `DocumentStorageService` | Handles file attachments | Upload, download, validation, S3/local storage |

## API Reference

Full API documentation is available in [REST_API_ROUTES.md](./REST_API_ROUTES.md).

### Invoice Endpoints (`/api/v1/invoices`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/invoices` | Create invoice |
| GET | `/api/v1/invoices` | List invoices (with filters) |
| GET | `/api/v1/invoices/{id}` | Get invoice by ID |
| GET | `/api/v1/invoices/overdue` | List overdue invoices |
| PUT | `/api/v1/invoices/{id}` | Update invoice |
| PUT | `/api/v1/invoices/{id}/status` | Update invoice status |
| DELETE | `/api/v1/invoices/{id}` | Delete invoice |

### Expense Endpoints (`/api/v1/expenses`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/expenses` | Create expense |
| GET | `/api/v1/expenses` | List expenses (with filters) |
| GET | `/api/v1/expenses/{id}` | Get expense by ID |
| PUT | `/api/v1/expenses/{id}` | Update expense |
| DELETE | `/api/v1/expenses/{id}` | Delete expense |

### Bill Endpoints (`/api/v1/bills`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/bills` | Create bill |
| GET | `/api/v1/bills` | List bills (with filters) |
| GET | `/api/v1/bills/{id}` | Get bill by ID |
| GET | `/api/v1/bills/overdue` | List overdue bills |
| PUT | `/api/v1/bills/{id}` | Update bill |
| PUT | `/api/v1/bills/{id}/status` | Update bill status |
| POST | `/api/v1/bills/{id}/mark-paid` | Mark bill as paid |
| DELETE | `/api/v1/bills/{id}` | Delete bill |

### Attachment Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/invoices/{id}/attachments` | Upload invoice attachment |
| GET | `/api/v1/invoices/{id}/attachments` | List invoice attachments |
| POST | `/api/v1/expenses/{id}/attachments` | Upload expense receipt |
| GET | `/api/v1/expenses/{id}/attachments` | List expense attachments |
| GET | `/api/v1/attachments/{id}/download-url` | Get download URL |
| DELETE | `/api/v1/attachments/{id}` | Delete attachment |

### Peppol Endpoints (`/api/v1/peppol`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/peppol/providers` | List available providers |
| GET | `/api/v1/peppol/settings` | Get Peppol settings |
| PUT | `/api/v1/peppol/settings` | Save Peppol settings |
| DELETE | `/api/v1/peppol/settings` | Delete Peppol settings |
| POST | `/api/v1/peppol/settings/connect` | Connect to Peppol provider |
| POST | `/api/v1/peppol/settings/connection-tests` | Test connection |
| POST | `/api/v1/peppol/recipient-validations` | Verify recipient on network |
| POST | `/api/v1/peppol/transmissions` | Send invoice via Peppol |
| GET | `/api/v1/peppol/transmissions` | List transmission history |
| POST | `/api/v1/peppol/invoice-validations` | Validate invoice for Peppol |
| POST | `/api/v1/peppol/inbox/syncs` | Poll inbox for documents |

### Cashflow Overview (`/api/v1/cashflow`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/cashflow/overview` | Get cashflow summary |

## Database Schema

### Tables Owned by Cashflow Service

| Table | Purpose |
|-------|---------|
| `DocumentsTable` | Base document storage |
| `DocumentProcessingTable` | Document processing status |
| `InvoicesTable` | Invoice records |
| `InvoiceItemsTable` | Invoice line items |
| `ExpensesTable` | Expense records |
| `BillsTable` | Supplier bill records |
| `PeppolSettingsTable` | Peppol provider configuration |
| `PeppolTransmissionsTable` | Peppol transmission history |

### Dependencies

| Dependency | Purpose |
|------------|---------|
| `TenantTable` (auth) | Multi-tenant isolation |
| `ContactsTable` (contacts) | Customer/supplier references |

## Key Files

```
features/cashflow/backend/
├── src/main/kotlin/ai/dokus/cashflow/backend/
│   ├── Application.kt                    # Ktor application entry point
│   ├── config/
│   │   └── DependencyInjection.kt        # Koin DI modules
│   ├── database/
│   │   └── CashflowTables.kt             # Table initialization
│   ├── plugins/
│   │   ├── Database.kt                   # Database configuration
│   │   └── Routing.kt                    # Route registration
│   ├── routes/
│   │   ├── Authentication.kt             # Auth utilities (re-exports)
│   │   ├── InvoiceRoutes.kt              # Invoice API endpoints
│   │   ├── ExpenseRoutes.kt              # Expense API endpoints
│   │   ├── BillRoutes.kt                 # Bill API endpoints
│   │   ├── AttachmentRoutes.kt           # File upload/download
│   │   ├── PeppolRoutes.kt               # E-invoicing endpoints
│   │   ├── CashflowOverviewRoutes.kt     # Analytics endpoints
│   │   └── DocumentUploadRoutes.kt       # Document processing
│   └── service/
│       ├── InvoiceService.kt             # Invoice business logic
│       ├── ExpenseService.kt             # Expense business logic
│       ├── BillService.kt                # Bill business logic
│       ├── CashflowOverviewService.kt    # Analytics calculations
│       └── DocumentStorageService.kt     # File storage handling
├── REST_API_ROUTES.md                    # Detailed API documentation
└── build.gradle.kts
```

## Authentication

All endpoints require JWT authentication via `Authorization: Bearer <token>` header.

### Authentication Flow

1. JWT token extracted from Authorization header
2. Token validated by `JwtValidator`
3. `DokusPrincipal` injected with user and tenant context
4. All database queries filtered by `tenantId` for isolation

### Multi-Tenant Security

- Every query includes `tenantId` filter
- No cross-tenant data access possible
- Tenant context required for all operations

## Peppol Integration

The service integrates with the Peppol network for European e-invoicing:

### Features

- **Send Invoices**: Convert and send invoices via Peppol
- **Receive Bills**: Poll inbox and automatically create bills
- **Recipient Verification**: Check if recipients are on the Peppol network
- **Provider Support**: Configurable provider (e.g., Recommand)

### Peppol Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Invoice       │────▶│  PeppolService  │────▶│  Peppol Network │
│   (Internal)    │     │  + PeppolMapper │     │  (Recommand)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                         │
                                                         ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Bill          │◀────│  BillService    │◀────│  Inbox Poll     │
│   (Auto-created)│     │  (createBill)   │     │  (Documents)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Configuration

```hocon
peppol {
  default-provider = "recommand"

  recommand {
    base-url = "https://api.recommand.eu"
    api-key = ${?RECOMMAND_API_KEY}
  }

  inbox {
    polling-enabled = true
    polling-interval-minutes = 15
  }
}
```

## Document Storage

### Supported File Types

| Category | MIME Types |
|----------|------------|
| Images | `image/jpeg`, `image/png`, `image/gif`, `image/webp` |
| Documents | `application/pdf`, Word (.doc, .docx), Excel (.xls, .xlsx) |
| Text | `text/plain`, `text/csv` |

### Storage Backends

| Backend | Use Case | Configuration |
|---------|----------|---------------|
| Local Filesystem | Development | `./storage/documents` |
| MinIO/S3 | Production | Configured via `storage.minio.*` |

### File Validation

- Maximum file size: 10MB (configurable)
- MIME type validation
- Path traversal protection
- Unique filename generation

## Expense Categories

The service supports Belgian tax-relevant expense categories:

| Category | Description |
|----------|-------------|
| `OfficeSupplies` | Office equipment, stationery |
| `Hardware` | Computers, monitors, peripherals |
| `Software` | Licenses, SaaS subscriptions |
| `Travel` | Business travel, accommodation |
| `Telecommunications` | Phone, internet services |
| `Meals` | Business meals (69% deductible in Belgium) |
| `ProfessionalServices` | Legal, accounting fees |
| `Utilities` | Electricity, gas, water |
| `Insurance` | Business insurance |
| `Rent` | Office space, coworking |
| `Vehicle` | Car expenses, fuel |
| `Marketing` | Advertising, website hosting |
| `Other` | Miscellaneous expenses |

## Configuration

### Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `DATABASE_URL` | PostgreSQL connection string | Yes |
| `JWT_SECRET` | JWT signing secret | Yes |
| `ENCRYPTION_KEY` | Peppol credential encryption | Yes |
| `RECOMMAND_API_KEY` | Peppol provider API key | For Peppol |

### HOCON Configuration

```hocon
ktor {
  deployment {
    port = 8002
    host = "0.0.0.0"
  }
}

database {
  url = "jdbc:postgresql://localhost:5432/dokus_cashflow"
  driver = "org.postgresql.Driver"
}

storage {
  public-url = "http://localhost:9000"
  minio {
    endpoint = "http://localhost:9000"
    access-key = ${?MINIO_ACCESS_KEY}
    secret-key = ${?MINIO_SECRET_KEY}
    bucket = "dokus-documents"
  }
}
```

## Development

### Running Locally

```bash
# Start dependencies (PostgreSQL, MinIO)
docker compose up -d postgres minio

# Run the service
./gradlew :features:cashflow:backend:run
```

### Testing API

```bash
# Get invoices
curl -X GET \
  -H "Authorization: Bearer <token>" \
  "http://localhost:8002/api/v1/invoices?limit=10"

# Create expense
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"merchant":"Apple Store","amount":"999.00","category":"Hardware"}' \
  "http://localhost:8002/api/v1/expenses"

# Upload attachment
curl -X POST \
  -H "Authorization: Bearer <token>" \
  -F "file=@receipt.pdf" \
  "http://localhost:8002/api/v1/expenses/{id}/attachments"
```

## Dependencies

- **Ktor** - HTTP server framework with Netty engine
- **Exposed** - Kotlin SQL framework
- **Koin** - Dependency injection
- **PostgreSQL** - Primary database
- **MinIO** - Object storage (S3-compatible)
- **Kotlin Serialization** - JSON processing
- **SLF4J + Logback** - Logging

## Error Handling

This service uses the standard DokusException error handling system. For comprehensive documentation including all exception types, error codes, client handling patterns, and troubleshooting, see the **[Error Handling Guide](../../../docs/ERROR_HANDLING.md)**.

### Quick Reference

Standard HTTP status codes and `DokusException` types used by this service:

| Status | Exception | Meaning |
|--------|-----------|---------|
| 200 | - | Success (GET/PUT) |
| 201 | - | Created (POST) |
| 204 | - | No Content (DELETE) |
| 400 | `BadRequest` | Invalid parameters |
| 401 | `NotAuthenticated` | Missing/invalid JWT |
| 403 | `NotAuthorized` | Permission denied |
| 404 | `NotFound` | Resource not found |
| 500 | `InternalError` | Server error |

### Additional Resources

- **[Complete Exception Reference](../../../docs/ERROR_HANDLING.md#exception-types-reference)** - All 40+ DokusException types with error codes
- **[Backend Configuration](../../../docs/ERROR_HANDLING.md#backend-error-handling-configuration)** - StatusPages plugin setup
- **[Client-Side Handling](../../../docs/ERROR_HANDLING.md#client-side-error-handling)** - Frontend error handling patterns
- **[Troubleshooting](../../../docs/ERROR_HANDLING.md#troubleshooting)** - Common error scenarios and debugging

## Related Services

- **Auth Service** - User authentication and tenant management
- **Contacts Service** - Customer/supplier contact management
- **AI Service** - Document classification and data extraction
- **Processor Service** - Background document processing
