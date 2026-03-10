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
 * Merges an optional extra [AuditCheck] into an existing [AuditReport].
 */
fun mergeAudit(base: AuditReport, invariantCheck: AuditCheck?): AuditReport {
    if (invariantCheck == null) return base
    val checks = if (base.checks.isEmpty()) listOf(invariantCheck) else base.checks + invariantCheck
    return AuditReport.fromChecks(checks)
}
