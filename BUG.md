# Implemented Functionality Overview (Bug Context)

This file summarizes functionality that already exists in the codebase today.
It is intentionally high-level and scoped to what is implemented, not roadmap.

## Backend (Ktor API + Services)

- Platform health and server info endpoints (liveness/readiness + detailed health).
- Auth/Identity:
  - Login, register, refresh token.
  - Password reset request + reset by token.
  - Email verification by token.
- Account:
  - Get current user.
  - Update profile, deactivate account.
  - Set active tenant (workspace) and obtain scoped tokens.
  - Logout, resend email verification.
- Tenants/workspaces:
  - List/create tenants for the user.
  - Get/update tenant settings.
  - Address get/upsert.
  - Invoice number preview.
  - Tenant avatar upload/get/delete.
- Team management:
  - List members, update role, remove member.
  - Transfer ownership.
  - Create/list/cancel invitations.
- Lookup:
  - CBE company lookup by name/VAT number.
- Contacts:
  - CRUD, list customers/vendors, summary stats.
  - Peppol settings per contact.
  - Activity summary per contact.
  - Merge contacts.
  - Notes CRUD.
- Cashflow:
  - Invoices CRUD, overdue list, status update.
  - Expenses CRUD.
  - Bills CRUD, overdue list, status update, mark paid.
  - Cashflow overview aggregation.
- Documents and attachments:
  - Upload documents to object storage with validation and hash de-duplication.
  - Document record API with pagination and filters, draft state, ingestion runs.
  - Confirm/reject/reprocess document extraction.
  - PDF page preview rendering.
  - Invoice/expense attachments upload/list/delete + download URLs.
- Peppol:
  - Provider list, settings save/delete/test/connect.
  - Recipient validation.
  - Invoice validation and transmission.
  - Inbox polling to create bills.
  - Transmission history listing and lookup.
- AI/Chat:
  - Cross-document and per-document chat (RAG).
  - Session listing/history and chat config.
- Background/utility services:
  - Document processing worker.
  - Embedding/chunking and PDF preview services.

Notes:
- Payment routes exist but are stubbed (TODO).
- Banking routes are registered but currently empty.

## Frontend (Compose Multiplatform)

- App shell:
  - Splash/bootstrap navigation (update required, auth, workspace selection, main).
  - Home navigation with dashboard, contacts, cashflow, AI chat, settings.
  - Placeholder/under-development destinations for simulations, inventory, banking, profile.
- Auth flow:
  - Server connection screen (self-hosted host/port/protocol).
  - Login, register, forgot/new password, email confirmation.
  - Workspace select/create.
  - Profile settings.
- Dashboard:
  - Workspace selector with avatar.
  - Pending documents card (mobile).
  - Search UI (desktop + mobile expand/collapse).
- Cashflow:
  - Documents list with search, sort, pagination.
  - Desktop summary cards (VAT, business health, pending docs).
  - Upload sidebar + drag-and-drop on desktop.
  - Add Document screen on mobile (file picker, upload list, QR app prompt).
  - Document review with confirm/reject and contact correction.
  - Document chat view.
  - Invoice creation WYSIWYG editor (desktop split layout, mobile step flow).
  - Peppol settings and connection screens.
- Contacts:
  - List with filters, search, master-detail on desktop.
  - Create contact VAT-first flow (lookup, confirm, manual).
  - Contact details with activity summary, notes, peppol toggle, merge, enrichment suggestions.
  - Edit contact form.
- Settings:
  - Workspace settings (company info, logo, address, invoice numbering, payment terms).
  - Team settings (members, roles, invitations, ownership transfer).
  - Appearance settings.

## User Flows (High-Level)

- Launch -> Splash bootstrap -> Login/Register or Server Connection -> Workspace Select/Create -> Home.
- Switch workspace from dashboard/settings; update profile and workspace settings.
- Upload document -> processing -> review -> confirm/reject -> appears in cashflow list; optional doc chat.
- Create invoice -> select client -> edit line items -> choose delivery method -> save/send.
- Manage contacts -> create via VAT lookup or manual -> view details -> add notes/toggle Peppol -> edit/merge.
- Team management -> invite members -> adjust roles -> transfer ownership.
