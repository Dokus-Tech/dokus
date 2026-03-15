package tech.dokus.features.ai.validation

/**
 * Checks that the counterparty VAT is not the same as the tenant VAT.
 * Returns a critical failure if they match, null otherwise.
 */
fun counterpartyInvariantCheck(tenantVat: String?, counterpartyVat: String?): AuditCheck? {
    if (tenantVat == null || counterpartyVat == null) return null
    if (tenantVat != counterpartyVat) return null
    return AuditCheck.criticalFailure(
        type = CheckType.COUNTERPARTY_INTEGRITY,
        field = "counterpartyVat",
        message = "Counterparty VAT equals tenant VAT ($tenantVat)",
        hint = "Verify seller/buyer extraction and direction; counterparty must be a non-tenant entity",
        expected = "counterparty VAT != tenant VAT",
        actual = "$counterpartyVat == $tenantVat"
    )
}

/**
 * Checks that the raw extracted merchant/seller VAT is not the same as the tenant VAT.
 * This catches AI hallucination where the model copies the tenant VAT into the merchant field.
 * Returns a warning if they match, null otherwise.
 */
fun rawVatInvariantCheck(tenantVat: String?, rawMerchantOrSellerVat: String?): AuditCheck? {
    if (tenantVat == null || rawMerchantOrSellerVat == null) return null
    if (tenantVat != rawMerchantOrSellerVat) return null
    return AuditCheck.warning(
        type = CheckType.COUNTERPARTY_INTEGRITY,
        field = "rawMerchantVat",
        message = "Extracted merchant/seller VAT equals tenant VAT ($tenantVat) — possible hallucination",
        hint = "The extracted merchant/seller VAT '$rawMerchantOrSellerVat' is the tenant's own VAT number — " +
            "this is almost certainly a hallucination. " +
            "Look carefully at the document: if no merchant/seller VAT is visibly printed, set merchantVat/sellerVat to null. " +
            "Do NOT copy the buyer/customer VAT into the seller field.",
        expected = "merchant VAT != tenant VAT (or confirmed on document)",
        actual = "$rawMerchantOrSellerVat == $tenantVat"
    )
}

/**
 * Merges an optional extra [AuditCheck] into an existing [AuditReport].
 */
fun mergeAudit(base: AuditReport, invariantCheck: AuditCheck?): AuditReport {
    if (invariantCheck == null) return base
    val checks = if (base.checks.isEmpty()) listOf(invariantCheck) else base.checks + invariantCheck
    return AuditReport.fromChecks(checks)
}
