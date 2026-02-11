Below is a functional (non-technical) product description of the main Dokus modules and how users
move through them. I’m writing it the way a PM would: what it does, what the user sees, what the
system does, and the key flows + edge cases.

⸻

1) Documents

What “Documents” is

The Documents module is the single intake and truth layer of Dokus. Anything that enters the
system (uploaded PDF, email import, PEPPOL inbound invoice, etc.) becomes a Document first.

A Document is:
• A file (PDF/XML/etc.)
• With extracted structured fields (supplier, totals, VAT, dates, references, line items)
• With a processing/validation status
• With a link to downstream financial objects (inbound invoice, invoice, expense entry, cashflow entry)

Core user goals
• “I received an invoice/receipt → get it into Dokus fast.”
• “I want the system to understand it without me doing bookkeeping.”
• “If it’s unsure, show me exactly what to fix, quickly.”

User flows

A) Upload document (PDF)

1. User clicks Upload (drag & drop or file picker)
2. Document appears in list immediately with a Processing state (not an error)
3. Dokus extracts data (supplier, totals, VAT, due date, etc.)
4. Result paths:
   • High confidence → auto-draft created (e.g., Inbound Invoice draft) and ready for review
   • Medium/low confidence → user gets a “Review needed” state with a guided fix flow
   • Failed → user sees a clear failure state with options (retry, download original, report)

Key UI expectation: “quiet system” — no drama, no scary red unless truly broken.

B) Confirm a document (review + approve)

1. User opens the document
2. Sees:
   • Original preview (PDF/XML)
   • Extracted fields (editable)
   • Suggested contact match (supplier/customer)
3. User chooses:
   • Confirm & create (inbound invoice/expense/invoice depending on type)
   • Fix fields then confirm
4. Dokus records confirmation and updates downstream cashflow/tax projections.

C) Link to contact (critical subflow)
When supplier/customer is detected:
• If matched: show the matched contact
• If ambiguous/missing: user can:
• Link existing contact
• Create new contact (prefilled with extracted company info)
•    (Optionally) postpone link if you allow it—but product-wise, I’d push “link now” unless it
blocks.

Document states (functional)
• Processing: system working; user can still open but sees “in progress”
• Review needed: extraction done but needs human confirmation
• Ready/Confirmed: user approved; document is now a financial record
• Failed: extraction/import failed; offer recovery actions

⸻

2) Cashflow

What “Cashflow” is

Cashflow is the forward-looking money view of Dokus: what’s expected to come in, what’s expected to
go out, what’s overdue, what’s already paid.

It is not “bank transactions” (yet). It’s a ledger of expected cash movements derived from:
• Invoices you sent (expected incoming)
• Inbound Invoices/expenses you owe (expected outgoing)
• Payments recorded (actuals)

Core user goals
• “How much money will I have in 2–8 weeks?”
• “What’s overdue?”
• “What do I need to pay next?”
• “What invoices haven’t been paid?”

User flows

A) Cashflow overview (daily use)

1. User opens Cashflow
2. Sees a segmented status view (example):
   • Open (not yet due)
   • Overdue
   • Paid
   • Cancelled
3. Clicking a segment filters the ledger list.

Ledger rows should be one-line, scannable:
• Counterparty (company)
• Amount
• Due date
• Status
• Reference (invoice number / supplier ref)
• Source type (Invoice / Inbound Invoice / Expense)

B) From invoice/inbound invoice → cashflow entry is created automatically
• Create an Invoice → Dokus adds an incoming cashflow entry
• Confirm a Inbound Invoice → Dokus adds an outgoing cashflow entry
• Record a Payment → cashflow entry becomes Paid (and stores paid date/amount)

C) Record payment (manual)

1. User opens an entry (or invoice/inbound invoice)
2. Clicks Mark as paid
3. Enters:
   • paid date
   • paid amount (defaults to full)
   • payment method (optional)
4. System updates:
   • status to Paid
   • links payment to the underlying record
   • cashflow totals change immediately

D) Overdue handling
• If due date passes and not paid → becomes Overdue automatically
• This is a “quiet” alert: shown in status segment counts, not spammy notifications.

⸻

3) Clients (Contacts)

What “Clients” is

Clients/Contacts is the canonical counterparty directory used everywhere:
• Invoices are addressed to a client
• Inbound Invoices are linked to suppliers
• Documents match against contacts for automation
• PEPPOL needs accurate identifiers (VAT, addresses, endpoints)

This is not just “customers” — it’s customers + suppliers in one list, with lightweight tagging.

Core user goals
• “I want one place for every company I deal with.”
• “Don’t make me duplicate suppliers vs customers; just tag them.”
• “Make matching automatic from documents.”

Contact data (functional)
• Company name
• VAT number
• Address
• Email (optional)
• Tags/roles:
• Customer
• Supplier
• Both
• Notes / metadata (optional)

User flows

A) Create contact manually

1. Add contact
2. Enter company + VAT + address
3. Dokus validates VAT format
4. Saved contact becomes available everywhere

B) Create contact from a document

1. User uploads invoice/inbound invoice
2. Dokus suggests “Create new contact” with prefilled fields
3. User confirms
4. Document is linked; next documents from same supplier auto-match

C) Merge / resolve duplicates (important in real life)
When Dokus detects duplicates (same VAT, similar name):
• show suggestion to merge
• user approves merge
• documents and invoices relink cleanly

⸻

4) AI

What “AI” is in Dokus

AI is not a chatbot gimmick. It’s an automation layer that:
• Extracts structured data from documents
• Suggests correct matching (contact, category, VAT treatment)
• Generates short, useful labels/descriptions when the UI needs one line
• Flags anomalies (“this VAT doesn’t make sense”, “this looks like a credit note”, etc.)

Core user goals
• “Don’t make me do bookkeeping.”
• “If you’re uncertain, show me only what matters.”
• “Learn my patterns without being annoying.”

AI flows

A) Extraction + confidence

1. Document enters (upload or PEPPOL inbound)
2. AI extracts fields and produces a confidence assessment
3. Routing:
   • High confidence → create draft automatically
   • Lower confidence → request user review on specific fields only (don’t dump everything)

B) Smart one-line descriptions (UI quality feature)
If you want “Firstbase-style one-line rows”, AI can generate a short descriptor:
• “KBC Leasing – January lease”
• “Google Workspace – monthly subscription”
• “AWS – cloud hosting”
This makes lists readable without multiline rows.

C) Correction loop (“correct before clever”)
When user edits extracted fields:
• Dokus records the corrections
• future suggestions improve (rule-based or learned patterns depending on approach)

⸻

5) PEPPOL

What “PEPPOL” is

PEPPOL is the compliance rail. Dokus supports:
• Outbound: sending your invoices as compliant e-invoices
• Inbound: receiving supplier invoices directly into Documents
• Transmission tracking: delivery status, errors, receipts

This is the “it just works” pillar for Belgian 2026 compliance.

Core user goals
• “Make me compliant by default.”
• “I don’t want to configure scary stuff unless I’m self-hosting.”
• “Show me status only when something is wrong.”

User flows

A) Connect PEPPOL (first-time setup)

1. User opens Settings → PEPPOL
2. System checks:
   • business identity fields exist (company name, VAT, address)
3. User clicks Connect
4. Result:
   • PEPPOL is enabled
   • inbound/outbound tracking becomes available

(For cloud users: preconfigured. For self-host: credentials required.)

B) Send invoice via PEPPOL (outbound)

1. User creates invoice
2. Clicks Send
3. Dokus checks if recipient supports PEPPOL (or has endpoint)
4. Dokus transmits
5. User sees status:
   • Sent
   • Delivered
   • Failed (with actionable reason)

C) Receive invoice via PEPPOL (inbound)

1. Supplier sends invoice to your PEPPOL ID
2. Dokus receives it and creates a Document
3. Document follows the same flow:
   • Processing → (Review needed or Ready) → Confirmed inbound invoice/expense
4. The system tracks transmission metadata for audit/compliance.

⸻

How these modules fit together (the “Dokus way”)

Documents is the intake.
Everything comes in there first.

AI interprets.
It turns messy reality into structured fields + confidence.

Clients anchors identity.
Every money movement ties to a real counterparty.

Cashflow is the outcome.
It shows the future financial truth, derived from confirmed records.

PEPPOL is the compliance rail.
Outbound + inbound become default pipes instead of manual work.

⸻