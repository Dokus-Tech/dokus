# Core Domain Model

Key concepts in Dokus:

- Document
    - Uploaded file (invoice, receipt, credit note, contract)
    - Has processing status

- Cashflow Item
    - Income or expense
    - Linked to a document

- Contact
    - Supplier or client
    - Can be auto-created by AI

- Item (Catalog)
    - Reusable invoice line template
    - NOT inventory

- Bank Transaction
    - Imported financial movement
    - Suggestively matched only

Documents are the source of truth.