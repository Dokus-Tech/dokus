package tech.dokus.features.ai.models

object ExtractionToolDescriptions {
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

    const val IssueDate = "Issue date. Null if not visible."
    const val DueDate = "Due date. Null if not visible."
    const val ValidUntil = "Validity/expiration date. Null if not visible."
    const val OrderDate = "Order date. Null if not visible."
    const val ExpectedDeliveryDate = "Expected delivery date. Null if not visible."
    const val ReceiptDate = "Transaction date. Null if not visible."

    const val CustomerName = "Customer/billed-to name. Null if unclear."
    const val CustomerVat = "Customer VAT number if shown (e.g. BE0123456789). Null if not visible."
    const val CustomerEmail = "Customer email if visible."

    const val SupplierName = "Supplier/vendor name (issuer). Null if unclear."
    const val SupplierVat = "Supplier VAT number if shown. Null if not visible."
    const val SupplierEmail = "Supplier email if visible."

    const val SellerName = "Seller/issuer legal or trading name (header/logo area). Null if unclear."
    const val SellerVat = "Seller VAT number if shown. Null if not visible."
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

    const val CounterpartyName = "Counterparty name (customer or supplier depending on direction). Null if unclear."
    const val CounterpartyVat = "Counterparty VAT number if shown. Null if not visible."

    const val MerchantName = "Merchant/store name from the receipt header. Null if not visible."

    const val Iban = "IBAN for payment if visible."
    const val PaymentReference = "Payment reference / structured communication if visible."

    const val LineItems = "Line items table. Leave empty list if not itemized."
    const val VatBreakdown = "VAT breakdown rows per rate (rate/base/amount). Leave empty list if not shown. Use rate 0 and VAT amount 0 for reverse charge."

    const val LineItemDescription = "Line item description. Required when providing a line item."
    const val LineItemQuantity = "Quantity as a whole number string (e.g., '2'). Null if not shown."
    const val LineItemUnitPrice = "Unit price (excl VAT) as plain number string (e.g., '12.50'). Null if not shown."
    const val LineItemVatRate = "VAT rate percentage for this line (e.g., '21'). Null if not shown."
    const val LineItemNetAmount = "Line total excl VAT as plain number string. Null if not shown."

    const val VatBreakdownRate = "VAT rate percentage for this row (e.g., '6', '12', '21')."
    const val VatBreakdownBase = "Taxable base (excl VAT) for this rate as plain number string."
    const val VatBreakdownAmount = "VAT amount for this rate as plain number string."

    const val CreditNoteDirection = "Direction: SALES (we issued) or PURCHASE (we received). UNKNOWN if unclear."
    const val OriginalInvoiceNumber = "Original invoice number if referenced. Null if not visible."
    const val CreditNoteReason = "Reason for credit note if explicitly stated. Null if not visible."

    const val PaymentMethod = "Payment method if visible."
}
