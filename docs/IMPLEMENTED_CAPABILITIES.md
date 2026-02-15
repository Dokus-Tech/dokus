# Implemented Capabilities Snapshot

This document summarizes currently implemented capabilities based on:
- route contracts in `foundation/domain/src/commonMain/kotlin/tech/dokus/domain/routes/*.kt`
- navigation destinations in `foundation/navigation/src/commonMain/kotlin/tech/dokus/navigation/destinations/*.kt`
- module declarations in `settings.gradle.kts`

## Backend API Surface

### Common

- Health endpoints: `/health`, `/health/live`, `/health/ready`
- Server info endpoint: `/api/v1/server/info`

### Identity and Account

- Identity: login, register, refresh, password reset, email verification
- Account: current user, profile update, deactivate, active tenant, logout

### Tenant and Team

- Tenant: list/create/get, settings, address, avatar, invoice number preview
- Team: members, role updates, invitations, ownership transfer

### Lookup

- Company lookup endpoint for external registry search

### Contacts

- CRUD and filtered listing
- Customers/vendors projections
- Activity summary
- Merge flow
- Contact notes CRUD
- PEPPOL status checks per contact

### Documents

- Upload and paginated listing with filters
- Content download
- Draft retrieval and patching
- Ingestion history and reprocess
- Confirm/reject flows
- Single-document chat
- PDF pages listing and rendered page image retrieval

### Cashflow

- Overview endpoint with view-mode/date/status filters
- Ledger entries list + entry detail operations
- Entry payments and cancel operation endpoints

### Invoices and Expenses

- Invoice listing/CRUD/overdue/status/payments/emails/attachments
- Expense listing/CRUD/attachments

### PEPPOL

- Providers/settings/connectivity tests
- Recipient/invoice validation
- Transmission history/detail
- Inbox sync
- Registration state-machine endpoints (`verify`, `enable`, `enable-sending-only`, `wait-for-transfer`, `opt-out`, `poll`)

### Notifications

- Listing, unread count, mark-read/mark-all-read, preferences

### Payments

- Payment routes exist and are currently stubbed in backend implementation.

## Frontend Navigation Surface

### Core

- Splash
- Home
- Update required

### Auth

- Login/register/password reset
- Workspace selection/creation
- Profile/settings
- Session management
- QR auth flow
- Server connection flow

### Home Tabs / Areas

- Today
- Tomorrow
- Documents
- Cashflow
- Contacts
- Team
- AI chat
- Settings
- More

### Settings

- Workspace settings
- Team settings
- Appearance settings
- Notification preferences
- PEPPOL registration

### Cashflow/Document Destinations

- Add document
- Create invoice
- Document review
- Document chat
- Cashflow ledger (with optional highlighted entry)

### Contact Destinations

- Create contact (with optional prefill/origin)
- Edit contact
- Contact details

## Module Topology (from `settings.gradle.kts`)

### App Entrypoints

- `:composeApp`
- `:backendApp`

### Foundation Modules

- `:foundation:aura`
- `:foundation:app-common`
- `:foundation:platform`
- `:foundation:navigation`
- `:foundation:backend-common`
- `:foundation:database`
- `:foundation:peppol`
- `:foundation:domain`
- `:foundation:sstorage`

### Feature Modules

- `:features:ai:backend`
- `:features:auth:data`
- `:features:auth:domain`
- `:features:auth:presentation`
- `:features:cashflow:data`
- `:features:cashflow:domain`
- `:features:cashflow:presentation`
- `:features:contacts:data`
- `:features:contacts:domain`
- `:features:contacts:presentation`

## Notes

- This is a capability snapshot, not a product roadmap.
- When route contracts or destinations change, update this file in the same PR.
