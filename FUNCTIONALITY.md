# Implemented Functionality (Deeper Analysis)

This document provides a deeper, but still concise, snapshot of implemented
functionality across backend APIs, frontend screens, and user flows.

## Backend (Ktor API)

### Common
- Health:
  - `GET /health/live` liveness.
  - `GET /health/ready` readiness with memory/disk/thread checks.
  - `GET /health` detailed health + JVM/system info.
- Server info: exposed via backend-common routes.

### Auth & Identity
- Identity (unauthenticated):
  - `POST /api/v1/identity/login`
  - `POST /api/v1/identity/register`
  - `POST /api/v1/identity/refresh`
  - `POST /api/v1/identity/password-resets` (request reset email)
  - `PATCH /api/v1/identity/password-resets/{token}` (reset password)
  - `PATCH /api/v1/identity/email-verifications/{token}` (verify email)
- Account (authenticated):
  - `GET /api/v1/account/me`
  - `PATCH /api/v1/account/profile`
  - `POST /api/v1/account/deactivate`
  - `PUT /api/v1/account/active-tenant` (switch tenant + new tokens)
  - `POST /api/v1/account/logout`
  - `POST /api/v1/account/email-verifications` (resend)

### Tenants / Workspaces
- Tenants:
  - `GET /api/v1/tenants` list memberships (+ avatar URLs if set)
  - `POST /api/v1/tenants` create tenant and add user as owner
  - `GET /api/v1/tenants/{id}` fetch tenant (with avatar)
  - `GET /api/v1/tenants/settings` / `PUT /api/v1/tenants/settings`
  - `GET /api/v1/tenants/address` / `PUT /api/v1/tenants/address`
  - `GET /api/v1/tenants/invoice-number-preview`
- Avatar (tenant logo):
  - `POST /api/v1/tenants/avatar` (multipart upload)
  - `GET /api/v1/tenants/avatar`
  - `DELETE /api/v1/tenants/avatar`

### Team
- Members:
  - `GET /api/v1/team/members`
  - `PUT /api/v1/team/members/{userId}/role`
  - `DELETE /api/v1/team/members/{userId}`
- Ownership:
  - `PUT /api/v1/team/owner` (transfer ownership)
- Invitations:
  - `GET /api/v1/team/invitations`
  - `POST /api/v1/team/invitations`
  - `DELETE /api/v1/team/invitations/{id}`

### Lookup
- Company search (CBE):
  - `GET /api/v1/lookup/company?name=...&number=...`

### Contacts
- CRUD:
  - `GET /api/v1/contacts` (filters: active, peppolEnabled, search, limit, offset)
  - `POST /api/v1/contacts`
  - `GET /api/v1/contacts/{id}`
  - `PUT /api/v1/contacts/{id}`
  - `DELETE /api/v1/contacts/{id}`
- Segments:
  - `GET /api/v1/contacts/customers`
  - `GET /api/v1/contacts/vendors`
  - `GET /api/v1/contacts/summary`
- Peppol:
  - `PATCH /api/v1/contacts/{id}/peppol`
- Activity:
  - `GET /api/v1/contacts/{id}/activity`
- Merge:
  - `POST /api/v1/contacts/{id}/merge-into/{targetId}`
- Notes:
  - `GET /api/v1/contacts/{id}/notes`
  - `POST /api/v1/contacts/{id}/notes`
  - `PUT /api/v1/contacts/{id}/notes/{noteId}`
  - `DELETE /api/v1/contacts/{id}/notes/{noteId}`

### Cashflow
- Invoices:
  - `GET /api/v1/invoices` (filter by status/date, pagination)
  - `POST /api/v1/invoices`
  - `GET /api/v1/invoices/overdue`
  - `GET /api/v1/invoices/{id}`
  - `PUT /api/v1/invoices/{id}`
  - `DELETE /api/v1/invoices/{id}`
  - `PATCH /api/v1/invoices/{id}/status`
  - `POST /api/v1/invoices/{id}/payments` (stub)
  - `POST /api/v1/invoices/{id}/emails` (stub)
- Expenses:
  - `GET /api/v1/expenses` (filter by category/date, pagination)
  - `POST /api/v1/expenses`
  - `GET /api/v1/expenses/{id}`
  - `PUT /api/v1/expenses/{id}`
  - `DELETE /api/v1/expenses/{id}`
- Bills (supplier invoices):
  - `GET /api/v1/bills` (filter by status/category/date, pagination)
  - `POST /api/v1/bills`
  - `GET /api/v1/bills/overdue`
  - `GET /api/v1/bills/{id}`
  - `PUT /api/v1/bills/{id}`
  - `DELETE /api/v1/bills/{id}`
  - `PATCH /api/v1/bills/{id}/status`
  - `POST /api/v1/bills/{id}/payments` (mark paid)
- Overview:
  - `GET /api/v1/cashflow/overview`

### Documents & Attachments
- Upload:
  - `POST /api/v1/documents/upload` (multipart, prefix validation, SHA-256 dedupe)
- Records:
  - `GET /api/v1/documents` (paginated + filters: draft status, type, ingestion status, search)
  - `GET /api/v1/documents/{id}`
  - `DELETE /api/v1/documents/{id}`
  - `GET /api/v1/documents/{id}/draft`
  - `PATCH /api/v1/documents/{id}/draft`
  - `GET /api/v1/documents/{id}/ingestions`
  - `POST /api/v1/documents/{id}/reprocess`
  - `POST /api/v1/documents/{id}/confirm`
  - `POST /api/v1/documents/{id}/reject`
- PDF previews:
  - `GET /api/v1/documents/{id}/pages` (dpi, maxPages)
  - `GET /api/v1/documents/{id}/pages/{page}.png`
- Attachments:
  - `POST /api/v1/invoices/{id}/attachments`
  - `GET /api/v1/invoices/{id}/attachments`
  - `POST /api/v1/expenses/{id}/attachments`
  - `GET /api/v1/expenses/{id}/attachments`
  - `GET /api/v1/attachments/{id}/url`
  - `DELETE /api/v1/attachments/{id}`

### Peppol
- Providers and settings:
  - `GET /api/v1/peppol/providers`
  - `GET /api/v1/peppol/settings`
  - `PUT /api/v1/peppol/settings`
  - `DELETE /api/v1/peppol/settings`
  - `POST /api/v1/peppol/settings/connection-tests`
  - `POST /api/v1/peppol/settings/connect`
- Validations and transmissions:
  - `POST /api/v1/peppol/recipient-validations`
  - `POST /api/v1/peppol/invoice-validations`
  - `POST /api/v1/peppol/transmissions`
  - `GET /api/v1/peppol/transmissions`
  - `GET /api/v1/peppol/transmissions/{id}`
- Inbox:
  - `POST /api/v1/peppol/inbox/syncs` (poll inbox and create bills)

### AI / Chat
- Cross-document chat:
  - `POST /api/v1/chat`
- Single-document chat:
  - `POST /api/v1/documents/{id}/chat`
- Sessions:
  - `GET /api/v1/chat/sessions`
  - `GET /api/v1/chat/sessions/{sessionId}`
  - `GET /api/v1/chat/config`

Notes:
- Payment routes exist but are TODO in implementation.
- Banking routes are registered but currently empty.

## Frontend (Compose Multiplatform)

### App Shell & Navigation
- Root navigation: splash -> auth or home based on bootstrap checks.
- Home navigation: dashboard, contacts, cashflow, AI chat, settings.
- Stubs/under-development: simulations, inventory, banking, profile.

### Auth & Onboarding
- Server connection setup (self-hosted host/port/protocol).
- Login, register, forgot password, new password.
- Workspace select and workspace creation.
- Profile settings.

### Dashboard
- Workspace selector with avatar and switch action.
- Pending documents list (mobile only).
- Search UI in top bar.

### Cashflow
- Document table/list with search + sort + pagination.
- Desktop summary cards (VAT summary, business health, pending docs).
- Upload sidebar (desktop) + drag-and-drop with animated overlay.
- Mobile add document flow with file picker and upload list.
- Document review screen: confirm/reject, contact correction, chat link.
- Document chat screen for doc Q&A or cross-doc Q&A.
- Invoice creation:
  - Desktop: interactive invoice + send options panel.
  - Mobile: edit step -> send options step.
  - Client selection with autocomplete; auto-fill due date + VAT defaults.
- Peppol settings and connection flows.

### Contacts
- List with filters/search; master-detail on desktop.
- Create contact with VAT-first flow:
  - Lookup step -> confirm step -> manual entry step.
- Contact details:
  - Activity summary, notes, merge, peppol toggle.
  - Enrichment suggestions dialog.
- Edit contact form.

### Settings
- Workspace settings:
  - Company info, logo, address, invoice numbering, payment terms.
- Team settings:
  - Members list, role changes, invitations, ownership transfer.
- Appearance settings.

## User Flows (More Detail)

### Authentication & Workspace
1. Launch app -> Splash bootstrap checks.
2. If server not configured -> Server Connection screen.
3. Login or Register.
4. Email verification if required.
5. Workspace select or create.
6. Enter Home (dashboard by default).

### Document Intake -> Review -> Confirm
1. Upload document (desktop drag-drop/sidebar or mobile add document).
2. Backend ingestion runs create draft + extraction data.
3. Pending docs appear in dashboard (mobile) and cashflow (desktop).
4. Review document -> confirm or reject; optionally fix contact.
5. Confirmed doc appears in cashflow list and is available for chat.

### Invoice Creation -> Send
1. Create invoice from cashflow.
2. Select client from contacts (auto-fill due date/VAT defaults).
3. Edit line items + dates.
4. Choose delivery method; save or send.

### Contacts Management
1. Browse/search contacts list.
2. Create contact via VAT lookup or manual entry.
3. View details, add notes, merge duplicates, toggle Peppol.

### Team & Workspace Settings
1. Upload/change company logo.
2. Edit address and invoice numbering.
3. Invite members, change roles, transfer ownership.
