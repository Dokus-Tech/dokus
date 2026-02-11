Below is a PRD-style functional specification for Dokus, written so it can be handed to engineering,
design, or used to brief AI coding agents.
It is non-technical, but precise, testable, and aligned with the Dokus philosophy.

⸻

Dokus – Core Product PRD (Functional)

0. Product Principles (Non-Negotiable)

These apply to every feature:

1. Quiet by default
   No alerts, banners, or notifications unless user action is required.
2. Documents before accounting
   Reality enters as documents first. Accounting objects are downstream.
3. Correct before clever
   Users can always correct extracted data before automation proceeds.
4. Compliance is invisible
   PEPPOL, VAT, and audit trails exist without user effort.
5. One source of truth
   No duplicated concepts (e.g. suppliers vs customers, inbound invoices vs documents).

⸻

1. Documents Module – PRD

1.1 Purpose

The Documents module is the single intake, processing, and validation layer for all financial
inputs.

All downstream entities (invoices, inbound invoices, expenses, cashflow entries) originate from a Document.

⸻

1.2 Supported Inputs
• Manual file upload (PDF, image)
• PEPPOL inbound documents
• Future: email ingestion, API ingestion

⸻

1.3 Document Lifecycle

States

State Description
Processing File received, extraction in progress
Review needed Extraction completed, user input required
Ready / Confirmed User confirmed; downstream entity created
Failed Extraction or ingestion failed

⸻

1.4 Primary User Flow – Upload & Confirm

Flow: Upload document

1. User uploads file
2. Document appears instantly in list as Processing
3. System extracts structured data
4. System determines confidence level

Acceptance criteria
• User never sees a raw error during processing
• Processing state is not blocking UI usage
• Original file is always accessible

⸻

Flow: Review document

1. User opens document
2. UI shows:
   • Original document preview
   • Extracted fields (editable)
   • Suggested contact match
3. System highlights only low-confidence fields
4. User confirms

Acceptance criteria
• User is never forced to review fields with high confidence
• Edits are immediately reflected
• Confirmation is explicit (user action)

⸻

1.5 Contact Resolution (Sub-Flow)

When a document contains a counterparty:

System behavior
• Attempt match by VAT → strong match
• Attempt match by name → weak match
• If ambiguous, require user decision

User options
• Link existing contact
• Create new contact (prefilled)
• Correct extracted data

Acceptance criteria
• VAT match always overrides name match
• No duplicate contacts created silently
• User decision is remembered for future documents

⸻

1.6 Output of Document Confirmation

Upon confirmation, system creates:
• Inbound Invoice / Expense (incoming document)
• Invoice (outgoing document)
• Corresponding cashflow entry

⸻

2. Cashflow Module – PRD

2.1 Purpose

Cashflow shows expected and actual money movement over time.

It answers:

“What will happen to my money?”

⸻

2.2 Scope

Included:
• Expected incoming (invoices)
• Expected outgoing (inbound invoices, expenses)
• Paid items
• Overdue items

Excluded (for now):
• Bank transaction import
• Reconciliation

⸻

2.3 Cashflow Entry Lifecycle

Status Meaning
Open Not yet due
Overdue Due date passed, unpaid
Paid Payment recorded
Cancelled Voided, ignored

⸻

2.4 Primary User Flow – Cashflow Overview

Flow

1. User opens Cashflow screen
2. System displays segmented status bar:
   • Open
   • Overdue
   • Paid
   • Cancelled
3. User selects a segment
4. Ledger list filters accordingly

Ledger row must show (single line):
• Counterparty
• Amount
• Due date
• Status
• Reference

Acceptance criteria
• No charts required for MVP
• All entries are clickable
• List is scannable at a glance

⸻

2.5 Payment Flow

Flow: Mark as paid

1. User opens cashflow entry or source document
2. Clicks “Mark as paid”
3. Enters:
   • Paid date (default: today)
   • Paid amount (default: full)
4. Confirms

System behavior
• Status becomes Paid
• Payment metadata stored
• Cashflow recalculates immediately

Acceptance criteria
• Partial payments supported
• Overpayment prevented or warned
• Paid entries remain visible (not removed)

⸻

3. Clients (Contacts) Module – PRD

3.1 Purpose

Clients is the canonical counterparty registry.

Used by:
• Documents
• Invoices
• Inbound Invoices
• Cashflow
• PEPPOL

⸻

3.2 Contact Model (Functional)

Each contact contains:
• Legal name
• VAT number
• Address
• Optional email
• Tags:
• Customer
• Supplier
• Both

⸻

3.3 Primary User Flows

Create manually

1. User adds contact
2. Enters minimum required fields
3. Contact becomes available globally

Create from document

1. System suggests new contact
2. Fields prefilled from extraction
3. User confirms

Acceptance criteria
• VAT uniqueness enforced logically (merge suggestion)
• Contacts usable immediately after creation
• Tags do not duplicate records

⸻

3.4 Duplicate Resolution

System behavior
• Detect same VAT or strong name similarity
• Prompt user to merge

Acceptance criteria
• Merging preserves all linked documents
• No data loss
• User explicitly approves merge

⸻

4. AI Module – PRD

4.1 Purpose

AI is an assistive automation layer, not a visible product feature.

It reduces manual work without removing user control.

⸻

4.2 AI Responsibilities
• Data extraction from documents
• Confidence scoring
• Contact matching suggestions
• VAT and totals sanity checks
• One-line contextual descriptions

⸻

4.3 AI Interaction Rules
• AI never auto-confirms documents
• AI suggestions are editable
• AI corrections improve future behavior

Acceptance criteria
• No AI output is final without user confirmation
• AI failures degrade gracefully
• User never blocked by AI uncertainty

⸻

5. PEPPOL Module – PRD

5.1 Purpose

PEPPOL ensures default compliance for EU e-invoicing.

Users should not “manage” PEPPOL — it should quietly work.

⸻

5.2 Inbound Flow (Receiving)

1. Supplier sends invoice via PEPPOL
2. Dokus receives document
3. Document enters standard processing flow
4. User reviews and confirms

Acceptance criteria
• PEPPOL documents are indistinguishable from uploads (UX-wise)
• Transmission metadata stored for audit
• Failures are visible but non-blocking

⸻

5.3 Outbound Flow (Sending)

1. User creates invoice
2. Clicks “Send”
3. System checks recipient capability
4. Invoice transmitted
5. Status updated

Statuses
• Sent
• Delivered
• Failed (with reason)

⸻

5.4 Setup Flow

Cloud
• Preconfigured
• User only provides company identity

Self-hosted
• Credentials required
• Clear validation feedback

Acceptance criteria
• Setup cannot succeed with incomplete business identity
• User always sees current connection state

⸻

6. Cross-Module Acceptance Criteria
   • Every financial number can be traced back to a document
   • No silent data mutation
   • No duplicate concepts across modules
   • System remains usable during background processing
   • UI communicates state, not implementation

⸻

7. Out of Scope (Explicit)
   • Full accounting ledger
   • Bank reconciliation
   • Payroll
   • CRM features
   • Notifications as engagement tools

⸻