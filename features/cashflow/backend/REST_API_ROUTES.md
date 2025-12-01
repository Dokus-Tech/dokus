# Cashflow REST API Routes

This document describes the REST API routes implementation for the Cashflow backend service.

## Overview

The Cashflow service has been enhanced with REST API routes following the patterns from the Pulse project. These routes provide HTTP endpoints that replace the RPC interface while maintaining the same business logic.

## Architecture

### Route Files

All route files are located in `/features/cashflow/backend/src/main/kotlin/ai/dokus/cashflow/backend/routes/`:

1. **Authentication.kt** - Authentication helpers
   - `withTenant()` - Executes block with authenticated tenant context
   - `getAuthInfo()` - Retrieves authentication info from routing context

2. **ParametersExtensions.kt** - Type-safe parameter extraction
   - Extension properties for extracting typed parameters from Ktor `Parameters`
   - Handles ID types, enums, dates, pagination

3. **InvoiceRoutes.kt** - Invoice management endpoints
4. **ExpenseRoutes.kt** - Expense management endpoints
5. **AttachmentRoutes.kt** - File upload/download endpoints
6. **CashflowOverviewRoutes.kt** - Cashflow statistics endpoints
7. **CashflowRoutes.kt** - Route registration

## API Endpoints

### Invoice Routes (`/api/v1/invoices`)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/v1/invoices` | Create invoice | Implemented |
| GET | `/api/v1/invoices/{id}` | Get invoice by ID | Implemented |
| GET | `/api/v1/invoices` | List invoices (with filters) | Implemented |
| GET | `/api/v1/invoices/overdue` | List overdue invoices | Implemented |
| PUT | `/api/v1/invoices/{id}/status` | Update invoice status | Implemented |
| PUT | `/api/v1/invoices/{id}` | Update invoice | Implemented |
| DELETE | `/api/v1/invoices/{id}` | Delete invoice | Implemented |
| POST | `/api/v1/invoices/{id}/payment` | Record payment | TODO |
| POST | `/api/v1/invoices/{id}/send-email` | Send invoice via email | TODO |
| POST | `/api/v1/invoices/{id}/mark-sent` | Mark invoice as sent | TODO |
| POST | `/api/v1/invoices/calculate-totals` | Calculate invoice totals | TODO |

**Query Parameters for List:**
- `status` - Filter by InvoiceStatus
- `fromDate` - Filter by date range (ISO 8601)
- `toDate` - Filter by date range (ISO 8601)
- `limit` - Pagination limit (default: 50)
- `offset` - Pagination offset (default: 0)

### Expense Routes (`/api/v1/expenses`)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/v1/expenses` | Create expense | Implemented |
| GET | `/api/v1/expenses/{id}` | Get expense by ID | Implemented |
| GET | `/api/v1/expenses` | List expenses (with filters) | Implemented |
| PUT | `/api/v1/expenses/{id}` | Update expense | Implemented |
| DELETE | `/api/v1/expenses/{id}` | Delete expense | Implemented |
| POST | `/api/v1/expenses/categorize` | Categorize expense | TODO |

**Query Parameters for List:**
- `category` - Filter by ExpenseCategory
- `fromDate` - Filter by date range (ISO 8601)
- `toDate` - Filter by date range (ISO 8601)
- `limit` - Pagination limit (default: 50)
- `offset` - Pagination offset (default: 0)

### Attachment Routes

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/v1/invoices/{invoiceId}/attachments` | Upload invoice document | Implemented |
| GET | `/api/v1/invoices/{invoiceId}/attachments` | List invoice attachments | Implemented |
| POST | `/api/v1/expenses/{expenseId}/attachments` | Upload expense receipt | Implemented |
| GET | `/api/v1/expenses/{expenseId}/attachments` | List expense attachments | Implemented |
| GET | `/api/v1/attachments/{id}/download-url` | Get attachment download URL | Implemented |
| DELETE | `/api/v1/attachments/{id}` | Delete attachment | Implemented |

**File Upload Format:**
- Content-Type: `multipart/form-data`
- Field: `file` (binary file data)

### Cashflow Overview Routes (`/api/v1/cashflow`)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/v1/cashflow/overview` | Get cashflow overview | TODO |

**Query Parameters:**
- `fromDate` - Start date (ISO 8601, required)
- `toDate` - End date (ISO 8601, required)

## Authentication

All routes require JWT authentication and tenant context. The authentication flow:

1. JWT token is extracted from `Authorization: Bearer <token>` header
2. Token is validated and `AuthenticationInfo` is injected into coroutine context
3. `withTenant()` helper ensures tenant context exists
4. Repository methods are called with `tenantId` for multi-tenant security

## Error Handling

Routes use `DokusException` for consistent error responses:

- `DokusException.NotAuthenticated` (401) - Missing or invalid JWT
- `DokusException.NotAuthorized` (403) - Missing tenant context or permission denied
- `DokusException.BadRequest` (400) - Invalid request parameters
- `DokusException.InternalError` (500) - Server-side errors

## HTTP Status Codes

- `200 OK` - Successful GET/PUT requests
- `201 Created` - Successful POST requests (creation)
- `204 No Content` - Successful DELETE requests or void operations
- `400 Bad Request` - Validation errors
- `401 Unauthorized` - Authentication errors
- `403 Forbidden` - Authorization errors
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server errors

## Integration

To enable these routes in the application, call the registration function in the main Application configuration:

```kotlin
fun Application.module() {
    // ... other configuration ...

    configureCashflowRoutes()
}
```

## Migration from RPC

The routes are designed to work alongside the existing RPC implementation. Key differences:

1. **Transport**: HTTP/REST instead of KotlinX RPC
2. **Authentication**: Ktor JWT middleware instead of AuthInfoProvider
3. **Parameter Handling**: URL params and request body instead of method parameters
4. **File Uploads**: Multipart form data instead of ByteArray parameters

The existing `CashflowRemoteServiceImpl` can remain in place during the migration. Once clients are fully migrated to REST, the RPC implementation can be removed.

## TODO Items

The following features are marked as TODO and need implementation:

1. **Invoice Operations:**
   - Payment recording
   - Email sending
   - Mark as sent
   - Invoice totals calculation

2. **Expense Operations:**
   - Auto-categorization (ML/AI integration)

3. **Cashflow Overview:**
   - Statistics calculation (implement using repository aggregation queries)

## Testing

To test the routes, use tools like:
- curl
- Postman
- HTTPie
- Automated tests with Ktor test client

Example curl command:
```bash
curl -X GET \
  -H "Authorization: Bearer <jwt-token>" \
  "http://localhost:8000/api/v1/invoices?limit=10&offset=0"
```

## Security Considerations

1. **Multi-tenant isolation**: All queries filter by `tenantId`
2. **Authentication required**: No anonymous access
3. **File upload validation**: File size, type, and content validation
4. **Rate limiting**: Consider adding rate limiting middleware
5. **CORS**: Configure allowed origins for web clients

## Performance

1. **Pagination**: Default limit of 50 items to prevent large responses
2. **Lazy loading**: Attachments are not loaded with invoices/expenses by default
3. **Connection pooling**: Use HikariCP for database connections
4. **Async I/O**: File operations use Dispatchers.IO
