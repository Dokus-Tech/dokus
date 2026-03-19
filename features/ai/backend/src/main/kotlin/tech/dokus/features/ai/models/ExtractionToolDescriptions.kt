package tech.dokus.features.ai.models

object ExtractionToolDescriptions {
    private const val LocalDateOutputFormat =
        " Return as ISO 8601 date format YYYY-MM-DD (e.g. 2026-01-20)."
    private const val NullIfNotVisible = " Null if not visible."

    const val Confidence = "Confidence score 0.0-1.0 for the extraction quality."
    const val Reasoning = "Short reasoning: what you used to extract key fields."

    const val Currency = "Currency code like EUR. If symbol only, infer best guess."

    const val SubtotalAmount = "Subtotal/net amount (excl. VAT) as plain number string (e.g. 1234.56). Null if not present."
    const val VatAmount = "Total VAT amount as plain number string (e.g. 123.45). Null if not present."
    const val TotalAmount = "Total/gross amount (incl. VAT) as plain number string. Null if not present."

    const val InvoiceNumber = "Invoice number (e.g. 2025-001). Null if not visible."
    const val CreditNoteNumber = "Credit note number. Null if not visible."
    const val ProFormaNumber = "Pro forma number. Null if not visible."
    const val QuoteNumber = "Quote/offer number. Null if not visible."
    const val PurchaseOrderNumber = "Purchase order number. Null if not visible."
    const val ReceiptNumber = "Receipt/ticket number for identification. Null if not visible."

    const val IssueDate = "Issue date." + LocalDateOutputFormat + NullIfNotVisible
    const val DueDate = "Due date." + LocalDateOutputFormat + NullIfNotVisible
    const val ValidUntil = "Validity/expiration date." + LocalDateOutputFormat + NullIfNotVisible
    const val OrderDate = "Order date." + LocalDateOutputFormat + NullIfNotVisible
    const val ExpectedDeliveryDate = "Expected delivery date." + LocalDateOutputFormat + NullIfNotVisible
    const val ReceiptDate = "Transaction date." + LocalDateOutputFormat + NullIfNotVisible
    const val BankTransactionDate =
        "Transaction booking/value date for one bank statement row." + LocalDateOutputFormat + NullIfNotVisible

    const val CustomerName = "Customer/billed-to name. Null if unclear."
    const val CustomerVat = "Customer VAT number if shown (e.g. BE0123456789). Null if not visible."
    const val CustomerEmail = "Customer email if visible."

    const val SupplierName = "Supplier/vendor name (issuer). Null if unclear."
    const val SupplierVat = "Supplier VAT number if shown. Null if not visible."
    const val SupplierEmail = "Supplier email if visible."

    const val SellerName = "Seller/issuer legal or trading name (header/logo area). Null if unclear."
    const val SellerVat = "Seller VAT number if shown. Often in micro-print footer or legal block at the very bottom of the page — read carefully. Null if not visible."
    const val SellerEmail = "Seller email if visible."
    const val SellerStreet = "Seller street and number if visible."
    const val SellerPostalCode = "Seller postal code if visible."
    const val SellerCity = "Seller city if visible."
    const val SellerCountry = "Seller country code or name if visible."

    const val BuyerName = "Buyer/customer billed-to name. Null if unclear."
    const val BuyerVat = "Buyer VAT number if shown. Null if not visible."
    const val BuyerEmail = "Buyer email if visible."
    const val BuyerStreet = "Buyer street and number if visible."
    const val BuyerPostalCode = "Buyer postal code if visible."
    const val BuyerCity = "Buyer city if visible."
    const val BuyerCountry = "Buyer country code or name if visible."

    const val MerchantVat = "Merchant VAT number if visible on receipt. Null if not visible."

    const val DirectionHint = "Optional business direction hint relative to tenant: INBOUND, OUTBOUND, or UNKNOWN."
    const val DirectionHintConfidence = "Optional confidence score (0.0-1.0) for directionHint."

    const val CounterpartyName =
        "Authoritative counterparty legal/registered name for this document. Prefer footer/legal registration names over branding (e.g., use legal entity if visible). Null if unclear."
    const val CounterpartyVat =
        "Authoritative counterparty VAT number in canonical form if shown. Null if invalid or not visible."
    const val CounterpartyEmail = "Authoritative counterparty email if visible and clearly business-related."
    const val CounterpartyStreet = "Authoritative counterparty street and number if visible."
    const val CounterpartyPostalCode = "Authoritative counterparty postal code if visible."
    const val CounterpartyCity = "Authoritative counterparty city if visible."
    const val CounterpartyCountry = "Authoritative counterparty country code or name if visible."
    const val CounterpartyRole =
        "Role selected for authoritative counterparty: SELLER, BUYER, MERCHANT, or UNKNOWN."
    const val CounterpartyReasoning =
        "One short reason why this entity is the counterparty and why payment tokens are excluded."

    const val MerchantName = "Merchant/store name from the receipt header. Null if not visible."

    const val Iban = "IBAN for payment if visible."
    const val PaymentReference = "Payment reference / structured communication if visible."
    const val BankSignedAmount = "Signed amount for one transaction row as plain number string; positive for money received, negative for money sent."
    const val BankCounterpartyName = "Actual business or person entity name from the transaction details. NOT the transfer type header (e.g. use 'TEAM INNING BV' not 'SENDING MONEY TO'). Look in detail lines below the bold title for the real entity name."
    const val BankCounterpartyIban = "Counterparty IBAN from the transaction row if visible."
    const val BankStructuredCommunicationRaw = "Structured communication string exactly as displayed (e.g. +++123/4567/89012+++). Preserve formatting verbatim."
    const val BankFreeCommunication = "Free-form payment reference or communication that is not a Belgian structured communication (OGM). E.g. invoice number, reference code, mandate reference."
    const val BankDescriptionRaw = "Full raw text of the transaction including all detail lines, joined with newlines. Include the transfer type header AND all detail lines below it."
    const val BankRowConfidence = "Confidence score 0.0-1.0 for this specific transaction row."

    const val BankAccountIban = "IBAN of the account this statement belongs to if visible in the header/footer. Null if not visible."
    const val BankOpeningBalance = "Opening/previous balance as signed plain number string (e.g. '1234.56'). Null if not visible."
    const val BankClosingBalance = "Closing/new balance as signed plain number string (e.g. '1234.56'). Null if not visible."
    const val BankPeriodStart = "Start date of the statement period." + LocalDateOutputFormat + NullIfNotVisible
    const val BankPeriodEnd = "End date of the statement period." + LocalDateOutputFormat + NullIfNotVisible
    const val BankInstitutionName = "Legal or trading name of the bank/financial institution that issued this statement (e.g. 'Wise Europe SA', 'KBC Bank NV', 'ING Belgium'). Look in the header, logo, or footer. Null if not visible."
    const val BankInstitutionBic = "BIC/SWIFT code of the issuing bank if visible (e.g. 'TRWIBEB1XXX'). Null if not visible."

    const val LineItems = "Line items table. Leave empty list if not itemized."
    const val VatBreakdown = "VAT breakdown rows per rate (rate/base/amount). Leave empty list if not shown. Use rate 0 and VAT amount 0 for reverse charge."

    const val LineItemDescription = "Line item description."
    const val LineItemTitle = "Line item title. Required when providing a line item."
    const val LineItemQuantity = "Quantity as a whole number string (e.g., '2'). Null if not shown."
    const val LineItemUnitPrice = "Unit price (excl VAT) as plain number string (e.g., '12.50'). Null if not shown."
    const val LineItemVatRate = "VAT rate percentage for this line (e.g., '21'). Null if not shown."
    const val LineItemNetAmount = "Line total excl VAT as plain number string. Null if not shown."

    const val VatBreakdownRate = "VAT rate percentage for this row (e.g., '6', '12', '21')."
    const val VatBreakdownBase = "Taxable base (excl VAT) for this rate as plain number string."
    const val VatBreakdownAmount = "VAT amount for this rate as plain number string."

    const val OriginalInvoiceNumber = "Original invoice number if referenced. Null if not visible."
    const val CreditNoteReason = "Reason for credit note if explicitly stated. Null if not visible."

    const val PaymentMethod = "Payment method if visible."

    const val LocalDateToolOutputGuidance =
        "When submitting any date field to the tool, always normalize it to ISO 8601 YYYY-MM-DD (e.g. 2026-01-20), even if the document prints it as 20/01/2026 or 01/20/2026."

    const val DocumentDateFormatClarification =
        "If dd/mm/yyyy or mm/dd/yyyy is mentioned below, that describes how the date may appear on the document. The tool output must still use ISO 8601 YYYY-MM-DD."

    const val VatNumberFormatGuidance = """
    ## VAT NUMBER FORMAT
    - VAT numbers are COUNTRY CODE + DIGITS (and sometimes letters). Extract ONLY the VAT number, never enterprise numbers, RPR references, or other identifiers.
    - Belgian VAT: "BE" + exactly 10 digits (e.g., BE0123456789). When multiple BE-prefixed numbers appear in a footer, pick the one with exactly 10 digits — longer numbers are bank account identifiers, not VAT numbers.
    - Dutch VAT: "NL" + 9 digits + "B" + 2 digits (e.g., NL123456789B02).
    - German VAT: "DE" + exactly 9 digits (e.g., DE123456789).
    - French VAT: "FR" + 2 alphanumeric chars + 9 digits (e.g., FRXX123456789).
    - Austrian VAT (UID-Nr.): "ATU" + exactly 8 digits (e.g., ATU12345678).
    - Luxembourg VAT: "LU" + exactly 8 digits (e.g., LU12345678).
    - Italian VAT: "IT" + exactly 11 digits (e.g., IT12345678901).
    - Spanish VAT: "ES" + 1 letter + 7 digits + 1 alphanumeric (e.g., ESX1234567X).
    - EU OSS VAT: "EU" + 9 to 12 digits (e.g., EU372041333). Used for cross-border B2C services under the One-Stop Shop scheme. This is a valid format — do NOT flag it as malformed.
    - "UID", "UID-Nr.", "USt-IdNr.", "TVA", "BTW", "MwSt" are all labels for VAT numbers.
    - Remove dots/spaces from the VAT (e.g., "BE 0123.456.789" → "BE0123456789").
    - If you cannot isolate the exact VAT number, return null."""
}
