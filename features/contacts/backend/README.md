# Contacts Service

Contact management service for customers and vendors with notes history and Peppol e-invoicing support.

## Overview

The Contacts service provides a unified contact management system for the Dokus platform:

- **Customer Management**: Manage contacts who receive invoices
- **Vendor Management**: Manage contacts who send bills/expenses
- **Contact Notes**: Maintain notes history with author tracking for CRM functionality
- **Peppol Integration**: Support for Belgian B2B e-invoicing mandate (2026)

Contacts serve as the link between the business and external parties, used across invoicing, expenses, and banking reconciliation.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Contacts Service                            │
│                 (Ktor + Netty on port 8093)                      │
└───────────────────────────┬──────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ ContactRoutes │   │ContactService │   │ContactNote    │
│ (REST API)    │   │(Business Logic│   │Service        │
└───────────────┘   └───────────────┘   └───────────────┘
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Foundation Layer                              │
│  ContactRepository │ ContactNoteRepository │ JwtValidator        │
└─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                     PostgreSQL Database                          │
│           contacts │ contact_notes (tenant-isolated)            │
└─────────────────────────────────────────────────────────────────┘
```

### Contact Types

| Type | Description | Use Case |
|------|-------------|----------|
| `Customer` | Receives invoices | Invoice recipients |
| `Vendor` | Sends bills/expenses | Suppliers, service providers |
| `Both` | Acts as customer AND vendor | Trading partners |

## Key Files

```
features/contacts/backend/
├── src/main/kotlin/ai/dokus/contacts/backend/
│   ├── Application.kt              # Service entry point
│   ├── config/
│   │   └── DependencyInjection.kt  # Koin DI configuration
│   ├── database/
│   │   └── ContactsTables.kt       # Table initialization
│   ├── plugins/
│   │   ├── Database.kt             # Database lifecycle
│   │   └── Routing.kt              # Route registration
│   ├── routes/
│   │   └── ContactRoutes.kt        # REST API endpoints
│   └── service/
│       ├── ContactService.kt       # Contact business logic
│       └── ContactNoteService.kt   # Notes business logic
└── build.gradle.kts
```

## Services

### ContactService

Manages contact CRUD operations and business logic.

| Method | Description |
|--------|-------------|
| `createContact()` | Create a new customer/vendor contact |
| `getContact()` | Retrieve contact by ID |
| `listContacts()` | List contacts with filters (type, active, peppol, search) |
| `listCustomers()` | List Customer and Both type contacts |
| `listVendors()` | List Vendor and Both type contacts |
| `updateContact()` | Update contact details |
| `updateContactPeppol()` | Update Peppol e-invoicing settings |
| `deleteContact()` | Permanently delete a contact |
| `deactivateContact()` | Soft delete (set inactive) |
| `reactivateContact()` | Restore inactive contact |
| `getContactStats()` | Dashboard statistics (total, active, customers, vendors) |
| `listPeppolEnabledContacts()` | List contacts with Peppol enabled |

### ContactNoteService

Manages notes history for contacts with author tracking.

| Method | Description |
|--------|-------------|
| `createNote()` | Add a note with author info |
| `getNote()` | Retrieve note by ID |
| `listNotes()` | List notes for a contact (paginated) |
| `countNotes()` | Count notes for a contact |
| `updateNote()` | Update note content |
| `deleteNote()` | Delete a note |

## REST API

All endpoints require JWT authentication and tenant context.

**Base URL**: `/api/v1/contacts`

### Contact Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/contacts` | List contacts with filters |
| `POST` | `/api/v1/contacts` | Create a new contact |
| `GET` | `/api/v1/contacts/customers` | List customers only |
| `GET` | `/api/v1/contacts/vendors` | List vendors only |
| `GET` | `/api/v1/contacts/summary` | Get contact statistics |
| `GET` | `/api/v1/contacts/{id}` | Get contact by ID |
| `PUT` | `/api/v1/contacts/{id}` | Update contact |
| `DELETE` | `/api/v1/contacts/{id}` | Delete contact |
| `PATCH` | `/api/v1/contacts/{id}/peppol` | Update Peppol settings |

### Notes Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/contacts/{id}/notes` | List notes for contact |
| `POST` | `/api/v1/contacts/{id}/notes` | Add note to contact |
| `PUT` | `/api/v1/contacts/{id}/notes/{noteId}` | Update note |
| `DELETE` | `/api/v1/contacts/{id}/notes/{noteId}` | Delete note |

### Query Parameters

**List Contacts** (`GET /api/v1/contacts`):
| Parameter | Type | Description |
|-----------|------|-------------|
| `type` | `ContactType` | Filter by Customer, Vendor, or Both |
| `active` | `Boolean` | Filter by active status |
| `peppolEnabled` | `Boolean` | Filter by Peppol status |
| `search` | `String` | Search in name, email, company |
| `limit` | `Int` | Page size (1-200, default 50) |
| `offset` | `Int` | Pagination offset (default 0) |

## Database Schema

### ContactsTable

Stores all contacts (customers AND vendors) for a tenant.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `tenant_id` | UUID | Tenant reference (CRITICAL for isolation) |
| `name` | VARCHAR(255) | Contact/company name |
| `email` | VARCHAR(255) | Email address |
| `phone` | VARCHAR(50) | Phone number |
| `contact_person` | VARCHAR(255) | Primary contact person name |
| `vat_number` | VARCHAR(50) | VAT registration number |
| `company_number` | VARCHAR(50) | Company registration number |
| `contact_type` | ENUM | Customer, Vendor, or Both |
| `business_type` | ENUM | Business or Individual |
| `peppol_id` | VARCHAR(255) | Peppol participant ID |
| `peppol_enabled` | BOOLEAN | Peppol e-invoicing enabled |
| `address_line_1` | VARCHAR(255) | Street address line 1 |
| `address_line_2` | VARCHAR(255) | Street address line 2 |
| `city` | VARCHAR(100) | City |
| `postal_code` | VARCHAR(20) | Postal/ZIP code |
| `country` | VARCHAR(2) | ISO 3166-1 alpha-2 country code |
| `default_payment_terms` | INTEGER | Default payment days (default 30) |
| `default_vat_rate` | DECIMAL(5,2) | Default VAT rate |
| `tags` | TEXT | JSON tags for categorization |
| `is_active` | BOOLEAN | Active status (soft delete) |
| `created_at` | DATETIME | Creation timestamp |
| `updated_at` | DATETIME | Last update timestamp |

**Indexes**:
- `tenant_id` (for multi-tenant queries)
- `email` (for lookups)
- `contact_type` (for type filtering)
- `(tenant_id, vat_number)` UNIQUE (prevent duplicate VAT per tenant)
- `(tenant_id, is_active)` (common filter combo)
- `(tenant_id, contact_type, is_active)` (optimized listing)

### ContactNotesTable

Stores notes history for contacts with author tracking.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `tenant_id` | UUID | Tenant reference |
| `contact_id` | UUID | Parent contact reference |
| `content` | TEXT | Note content |
| `author_id` | UUID | User who created the note |
| `author_name` | VARCHAR(255) | Denormalized author display name |
| `created_at` | DATETIME | Creation timestamp |
| `updated_at` | DATETIME | Last update timestamp |

**Indexes**:
- `contact_id` (for listing notes by contact)
- `(contact_id, created_at)` (ordered listing)
- `(tenant_id, created_at)` (tenant-wide queries)

## Authentication

All routes require JWT authentication via the `authenticateJwt` wrapper.

```kotlin
authenticateJwt {
    get<Contacts> { route ->
        val tenantId = dokusPrincipal.requireTenantId()
        // ... tenant-scoped operations
    }
}
```

### JWT Principal

The `dokusPrincipal` provides:
- `userId` - Authenticated user ID
- `email` - User email address
- `tenantId` - Current tenant context
- `requireTenantId()` - Get tenant ID or throw exception

### Multi-Tenant Isolation

**CRITICAL**: All database queries MUST filter by `tenant_id` to ensure data isolation between organizations.

## Configuration

The service uses standard Ktor configuration via HOCON:

```hocon
ktor {
  deployment {
    port = 8093
    host = "0.0.0.0"
    environment = "development"
  }
}

database {
  url = "jdbc:postgresql://localhost:5432/dokus"
  driver = "org.postgresql.Driver"
  user = "postgres"
  password = ${?DB_PASSWORD}
  maximumPoolSize = 10
}

jwt {
  secret = ${?JWT_SECRET}
  issuer = "dokus-auth"
  audience = "dokus-api"
  realm = "dokus"
}
```

## Dependencies

- **Ktor** - Web framework with Netty server
- **Exposed** - Kotlin SQL framework
- **Koin** - Dependency injection
- **PostgreSQL** - Database
- **Foundation** - Shared models, repositories, and utilities

## Integration Points

### Auth Service
- Depends on `TenantTable` for multi-tenant support
- Depends on `UsersTable` for note author tracking
- Uses shared JWT validation from foundation

### Cashflow Service
- Contacts are referenced by invoices (customer) and expenses/bills (vendor)
- Contact information used for invoice generation

### Banking Service
- Contact matching for transaction reconciliation
- Vendor identification from bank transaction descriptions

## Development

### Running Locally

```bash
# From project root
./gradlew :features:contacts:backend:run
```

The service starts on port 8093 by default.

### Testing API

```bash
# List contacts (requires valid JWT token)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8093/api/v1/contacts"

# Create a customer
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corp",
    "email": "billing@acme.com",
    "contactType": "Customer",
    "vatNumber": "BE0123456789"
  }' \
  "http://localhost:8093/api/v1/contacts"

# Get contact statistics
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8093/api/v1/contacts/summary"
```

## Peppol E-Invoicing

Belgium mandates B2B e-invoicing via Peppol by 2026. The Contacts service supports:

- Storing Peppol participant IDs for contacts
- Filtering Peppol-enabled contacts
- Integration with Cashflow service for Peppol invoice delivery

### Enabling Peppol for a Contact

```bash
curl -X PATCH \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "peppolId": "0208:BE0123456789",
    "peppolEnabled": true
  }' \
  "http://localhost:8093/api/v1/contacts/{id}/peppol"
```
