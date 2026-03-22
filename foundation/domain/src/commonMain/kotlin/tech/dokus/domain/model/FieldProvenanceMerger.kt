package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.SourceTrust
import tech.dokus.domain.ids.UserId

/**
 * Result of merging two DocumentDraftData instances using field provenance.
 */
data class ProvenanceMergeResult(
    val mergedData: DocumentDraftData,
    val mergedProvenance: DocumentFieldProvenance,
)

/**
 * Merges incoming extraction data with existing data using field provenance rules.
 *
 * 1. If types differ (reclassification), incoming wins entirely
 * 2. User-locked fields are never overwritten
 * 3. Higher source trust overwrites lower trust
 * 4. Same trust: higher confidence wins
 */
fun mergeWithProvenance(
    existing: DocumentDraftData,
    incoming: DocumentDraftData,
    existingProvenance: DocumentFieldProvenance,
    incomingProvenance: DocumentFieldProvenance,
): ProvenanceMergeResult {
    // Type change = reclassification — incoming wins entirely
    if (existing::class != incoming::class) {
        return ProvenanceMergeResult(incoming, incomingProvenance)
    }

    return when (existing) {
        is InvoiceDraftData if incoming is InvoiceDraftData && existingProvenance is InvoiceFieldProvenance && incomingProvenance is InvoiceFieldProvenance -> mergeInvoice(
            existing,
            incoming,
            existingProvenance,
            incomingProvenance
        )

        is CreditNoteDraftData if incoming is CreditNoteDraftData && existingProvenance is CreditNoteFieldProvenance && incomingProvenance is CreditNoteFieldProvenance -> mergeCreditNote(
            existing,
            incoming,
            existingProvenance,
            incomingProvenance
        )

        is ReceiptDraftData if incoming is ReceiptDraftData && existingProvenance is ReceiptFieldProvenance && incomingProvenance is ReceiptFieldProvenance -> mergeReceipt(
            existing,
            incoming,
            existingProvenance,
            incomingProvenance
        )

        is BankStatementDraftData if incoming is BankStatementDraftData && existingProvenance is BankStatementFieldProvenance && incomingProvenance is BankStatementFieldProvenance -> mergeBankStatement(
            existing,
            incoming,
            existingProvenance,
            incomingProvenance
        )

        // Classified-only types or provenance type mismatch — incoming always wins
        else -> ProvenanceMergeResult(incoming, incomingProvenance)
    }
}

// ---------------------------------------------------------------------------
// Pick helpers — no string keys, no maps, no mutable state
// ---------------------------------------------------------------------------

/**
 * Picks the winning provenance between existing and incoming.
 */
private fun pick(existing: FieldProvenance?, incoming: FieldProvenance?): FieldProvenance? = when {
    existing == null -> incoming
    existing.userLocked -> existing
    incoming == null -> existing
    existing.shouldBeOverwrittenBy(incoming) -> incoming
    else -> existing
}

/**
 * Picks the winning value and provenance for a single field.
 */
private inline fun <T> pickValue(
    existingValue: T,
    incomingValue: T,
    existingProv: FieldProvenance?,
    incomingProv: FieldProvenance?,
): T {
    return when {
        existingProv == null -> incomingValue
        existingProv.userLocked -> existingValue
        incomingProv == null -> existingValue
        existingProv.shouldBeOverwrittenBy(incomingProv) -> incomingValue
        else -> existingValue
    }
}

private fun pickParty(existing: PartyFieldProvenanceDto, incoming: PartyFieldProvenanceDto) =
    PartyFieldProvenanceDto(
        name = pick(existing.name, incoming.name),
        vat = pick(existing.vat, incoming.vat),
        email = pick(existing.email, incoming.email),
        iban = pick(existing.iban, incoming.iban),
        streetLine1 = pick(existing.streetLine1, incoming.streetLine1),
        streetLine2 = pick(existing.streetLine2, incoming.streetLine2),
        postalCode = pick(existing.postalCode, incoming.postalCode),
        city = pick(existing.city, incoming.city),
        country = pick(existing.country, incoming.country),
    )

private fun pickPartyValue(
    existing: PartyDraftDto,
    incoming: PartyDraftDto,
    existingProv: PartyFieldProvenanceDto,
    incomingProv: PartyFieldProvenanceDto,
) = PartyDraftDto(
    name = pickValue(existing.name, incoming.name, existingProv.name, incomingProv.name),
    vat = pickValue(existing.vat, incoming.vat, existingProv.vat, incomingProv.vat),
    email = pickValue(existing.email, incoming.email, existingProv.email, incomingProv.email),
    iban = pickValue(existing.iban, incoming.iban, existingProv.iban, incomingProv.iban),
    streetLine1 = pickValue(
        existing.streetLine1,
        incoming.streetLine1,
        existingProv.streetLine1,
        incomingProv.streetLine1
    ),
    streetLine2 = pickValue(
        existing.streetLine2,
        incoming.streetLine2,
        existingProv.streetLine2,
        incomingProv.streetLine2
    ),
    postalCode = pickValue(
        existing.postalCode,
        incoming.postalCode,
        existingProv.postalCode,
        incomingProv.postalCode
    ),
    city = pickValue(existing.city, incoming.city, existingProv.city, incomingProv.city),
    country = pickValue(
        existing.country,
        incoming.country,
        existingProv.country,
        incomingProv.country
    ),
)

// ---------------------------------------------------------------------------
// Per-variant merge
// ---------------------------------------------------------------------------

private fun mergeInvoice(
    existing: InvoiceDraftData,
    incoming: InvoiceDraftData,
    ep: InvoiceFieldProvenance,
    ip: InvoiceFieldProvenance,
): ProvenanceMergeResult {
    val mergedProv = InvoiceFieldProvenance(
        direction = pick(ep.direction, ip.direction),
        invoiceNumber = pick(ep.invoiceNumber, ip.invoiceNumber),
        issueDate = pick(ep.issueDate, ip.issueDate),
        dueDate = pick(ep.dueDate, ip.dueDate),
        currency = pick(ep.currency, ip.currency),
        subtotalAmount = pick(ep.subtotalAmount, ip.subtotalAmount),
        vatAmount = pick(ep.vatAmount, ip.vatAmount),
        totalAmount = pick(ep.totalAmount, ip.totalAmount),
        lineItems = pick(ep.lineItems, ip.lineItems),
        vatBreakdown = pick(ep.vatBreakdown, ip.vatBreakdown),
        iban = pick(ep.iban, ip.iban),
        payment = pick(ep.payment, ip.payment),
        notes = pick(ep.notes, ip.notes),
        seller = pickParty(ep.seller, ip.seller),
        buyer = pickParty(ep.buyer, ip.buyer),
    )
    val mergedData = InvoiceDraftData(
        direction = pickValue(existing.direction, incoming.direction, ep.direction, ip.direction),
        invoiceNumber = pickValue(
            existing.invoiceNumber,
            incoming.invoiceNumber,
            ep.invoiceNumber,
            ip.invoiceNumber
        ),
        issueDate = pickValue(existing.issueDate, incoming.issueDate, ep.issueDate, ip.issueDate),
        dueDate = pickValue(existing.dueDate, incoming.dueDate, ep.dueDate, ip.dueDate),
        currency = pickValue(existing.currency, incoming.currency, ep.currency, ip.currency),
        subtotalAmount = pickValue(
            existing.subtotalAmount,
            incoming.subtotalAmount,
            ep.subtotalAmount,
            ip.subtotalAmount
        ),
        vatAmount = pickValue(existing.vatAmount, incoming.vatAmount, ep.vatAmount, ip.vatAmount),
        totalAmount = pickValue(
            existing.totalAmount,
            incoming.totalAmount,
            ep.totalAmount,
            ip.totalAmount
        ),
        lineItems = pickValue(existing.lineItems, incoming.lineItems, ep.lineItems, ip.lineItems),
        vatBreakdown = pickValue(
            existing.vatBreakdown,
            incoming.vatBreakdown,
            ep.vatBreakdown,
            ip.vatBreakdown
        ),
        iban = pickValue(existing.iban, incoming.iban, ep.iban, ip.iban),
        payment = pickValue(existing.payment, incoming.payment, ep.payment, ip.payment),
        notes = pickValue(existing.notes, incoming.notes, ep.notes, ip.notes),
        seller = pickPartyValue(existing.seller, incoming.seller, ep.seller, ip.seller),
        buyer = pickPartyValue(existing.buyer, incoming.buyer, ep.buyer, ip.buyer),
    )
    return ProvenanceMergeResult(mergedData, mergedProv)
}

private fun mergeCreditNote(
    existing: CreditNoteDraftData,
    incoming: CreditNoteDraftData,
    ep: CreditNoteFieldProvenance,
    ip: CreditNoteFieldProvenance,
): ProvenanceMergeResult {
    val mergedProv = CreditNoteFieldProvenance(
        direction = pick(ep.direction, ip.direction),
        creditNoteNumber = pick(ep.creditNoteNumber, ip.creditNoteNumber),
        issueDate = pick(ep.issueDate, ip.issueDate),
        currency = pick(ep.currency, ip.currency),
        subtotalAmount = pick(ep.subtotalAmount, ip.subtotalAmount),
        vatAmount = pick(ep.vatAmount, ip.vatAmount),
        totalAmount = pick(ep.totalAmount, ip.totalAmount),
        lineItems = pick(ep.lineItems, ip.lineItems),
        vatBreakdown = pick(ep.vatBreakdown, ip.vatBreakdown),
        originalInvoiceNumber = pick(ep.originalInvoiceNumber, ip.originalInvoiceNumber),
        reason = pick(ep.reason, ip.reason),
        notes = pick(ep.notes, ip.notes),
        seller = pickParty(ep.seller, ip.seller),
        buyer = pickParty(ep.buyer, ip.buyer),
    )
    val mergedData = CreditNoteDraftData(
        direction = pickValue(existing.direction, incoming.direction, ep.direction, ip.direction),
        creditNoteNumber = pickValue(
            existing.creditNoteNumber,
            incoming.creditNoteNumber,
            ep.creditNoteNumber,
            ip.creditNoteNumber
        ),
        issueDate = pickValue(existing.issueDate, incoming.issueDate, ep.issueDate, ip.issueDate),
        currency = pickValue(existing.currency, incoming.currency, ep.currency, ip.currency),
        subtotalAmount = pickValue(
            existing.subtotalAmount,
            incoming.subtotalAmount,
            ep.subtotalAmount,
            ip.subtotalAmount
        ),
        vatAmount = pickValue(existing.vatAmount, incoming.vatAmount, ep.vatAmount, ip.vatAmount),
        totalAmount = pickValue(
            existing.totalAmount,
            incoming.totalAmount,
            ep.totalAmount,
            ip.totalAmount
        ),
        lineItems = pickValue(existing.lineItems, incoming.lineItems, ep.lineItems, ip.lineItems),
        vatBreakdown = pickValue(
            existing.vatBreakdown,
            incoming.vatBreakdown,
            ep.vatBreakdown,
            ip.vatBreakdown
        ),
        originalInvoiceNumber = pickValue(
            existing.originalInvoiceNumber,
            incoming.originalInvoiceNumber,
            ep.originalInvoiceNumber,
            ip.originalInvoiceNumber
        ),
        reason = pickValue(existing.reason, incoming.reason, ep.reason, ip.reason),
        notes = pickValue(existing.notes, incoming.notes, ep.notes, ip.notes),
        seller = pickPartyValue(existing.seller, incoming.seller, ep.seller, ip.seller),
        buyer = pickPartyValue(existing.buyer, incoming.buyer, ep.buyer, ip.buyer),
    )
    return ProvenanceMergeResult(mergedData, mergedProv)
}

private fun mergeReceipt(
    existing: ReceiptDraftData,
    incoming: ReceiptDraftData,
    ep: ReceiptFieldProvenance,
    ip: ReceiptFieldProvenance,
): ProvenanceMergeResult {
    val mergedProv = ReceiptFieldProvenance(
        direction = pick(ep.direction, ip.direction),
        merchantName = pick(ep.merchantName, ip.merchantName),
        merchantVat = pick(ep.merchantVat, ip.merchantVat),
        date = pick(ep.date, ip.date),
        currency = pick(ep.currency, ip.currency),
        totalAmount = pick(ep.totalAmount, ip.totalAmount),
        vatAmount = pick(ep.vatAmount, ip.vatAmount),
        lineItems = pick(ep.lineItems, ip.lineItems),
        vatBreakdown = pick(ep.vatBreakdown, ip.vatBreakdown),
        receiptNumber = pick(ep.receiptNumber, ip.receiptNumber),
        paymentMethod = pick(ep.paymentMethod, ip.paymentMethod),
        notes = pick(ep.notes, ip.notes),
    )
    val mergedData = ReceiptDraftData(
        direction = pickValue(existing.direction, incoming.direction, ep.direction, ip.direction),
        merchantName = pickValue(
            existing.merchantName,
            incoming.merchantName,
            ep.merchantName,
            ip.merchantName
        ),
        merchantVat = pickValue(
            existing.merchantVat,
            incoming.merchantVat,
            ep.merchantVat,
            ip.merchantVat
        ),
        date = pickValue(existing.date, incoming.date, ep.date, ip.date),
        currency = pickValue(existing.currency, incoming.currency, ep.currency, ip.currency),
        totalAmount = pickValue(
            existing.totalAmount,
            incoming.totalAmount,
            ep.totalAmount,
            ip.totalAmount
        ),
        vatAmount = pickValue(existing.vatAmount, incoming.vatAmount, ep.vatAmount, ip.vatAmount),
        lineItems = pickValue(existing.lineItems, incoming.lineItems, ep.lineItems, ip.lineItems),
        vatBreakdown = pickValue(
            existing.vatBreakdown,
            incoming.vatBreakdown,
            ep.vatBreakdown,
            ip.vatBreakdown
        ),
        receiptNumber = pickValue(
            existing.receiptNumber,
            incoming.receiptNumber,
            ep.receiptNumber,
            ip.receiptNumber
        ),
        paymentMethod = pickValue(
            existing.paymentMethod,
            incoming.paymentMethod,
            ep.paymentMethod,
            ip.paymentMethod
        ),
        notes = pickValue(existing.notes, incoming.notes, ep.notes, ip.notes),
    )
    return ProvenanceMergeResult(mergedData, mergedProv)
}

private fun mergeBankStatement(
    existing: BankStatementDraftData,
    incoming: BankStatementDraftData,
    ep: BankStatementFieldProvenance,
    ip: BankStatementFieldProvenance,
): ProvenanceMergeResult {
    val mergedProv = BankStatementFieldProvenance(
        direction = pick(ep.direction, ip.direction),
        transactions = pick(ep.transactions, ip.transactions),
        accountIban = pick(ep.accountIban, ip.accountIban),
        openingBalance = pick(ep.openingBalance, ip.openingBalance),
        closingBalance = pick(ep.closingBalance, ip.closingBalance),
        periodStart = pick(ep.periodStart, ip.periodStart),
        periodEnd = pick(ep.periodEnd, ip.periodEnd),
        notes = pick(ep.notes, ip.notes),
    )
    val mergedData = BankStatementDraftData(
        direction = pickValue(existing.direction, incoming.direction, ep.direction, ip.direction),
        transactions = pickValue(
            existing.transactions,
            incoming.transactions,
            ep.transactions,
            ip.transactions
        ),
        accountIban = pickValue(
            existing.accountIban,
            incoming.accountIban,
            ep.accountIban,
            ip.accountIban
        ),
        openingBalance = pickValue(
            existing.openingBalance,
            incoming.openingBalance,
            ep.openingBalance,
            ip.openingBalance
        ),
        closingBalance = pickValue(
            existing.closingBalance,
            incoming.closingBalance,
            ep.closingBalance,
            ip.closingBalance
        ),
        periodStart = pickValue(
            existing.periodStart,
            incoming.periodStart,
            ep.periodStart,
            ip.periodStart
        ),
        periodEnd = pickValue(existing.periodEnd, incoming.periodEnd, ep.periodEnd, ip.periodEnd),
        notes = pickValue(existing.notes, incoming.notes, ep.notes, ip.notes),
    )
    return ProvenanceMergeResult(mergedData, mergedProv)
}

// ---------------------------------------------------------------------------
// User lock helpers — typed diff + lock in one step
// ---------------------------------------------------------------------------

/**
 * Applies user locks to provenance for fields that changed between [existing] and [updated].
 * Returns a new provenance with changed fields marked as user-locked.
 */
fun applyUserLocks(
    provenance: DocumentFieldProvenance,
    existing: DocumentDraftData,
    updated: DocumentDraftData,
    lockedAt: LocalDateTime,
    lockedBy: UserId,
): DocumentFieldProvenance = when (existing) {
    is InvoiceDraftData if updated is InvoiceDraftData && provenance is InvoiceFieldProvenance -> lockInvoice(
        provenance,
        existing,
        updated,
        lockedAt,
        lockedBy
    )

    is CreditNoteDraftData if updated is CreditNoteDraftData && provenance is CreditNoteFieldProvenance -> lockCreditNote(
        provenance,
        existing,
        updated,
        lockedAt,
        lockedBy
    )

    is ReceiptDraftData if updated is ReceiptDraftData && provenance is ReceiptFieldProvenance -> lockReceipt(
        provenance,
        existing,
        updated,
        lockedAt,
        lockedBy
    )

    is BankStatementDraftData if updated is BankStatementDraftData && provenance is BankStatementFieldProvenance -> lockBankStatement(
        provenance,
        existing,
        updated,
        lockedAt,
        lockedBy
    )

// Type changed or classified-only — rebuild from scratch
    else -> updated.buildProvenance(
        sourceTrust = SourceTrust.ManualEntry,
    )
}

private fun lockField(
    old: Any?,
    new: Any?,
    prov: FieldProvenance?,
    lockedAt: LocalDateTime,
    lockedBy: UserId,
): FieldProvenance? {
    if (old == new) return prov
    return (prov ?: FieldProvenance(sourceTrust = SourceTrust.ManualEntry)).copy(
        userLocked = true,
        lockedAt = lockedAt,
        lockedBy = lockedBy,
    )
}

private fun lockParty(
    prov: PartyFieldProvenanceDto,
    old: PartyDraftDto,
    new: PartyDraftDto,
    lockedAt: LocalDateTime,
    lockedBy: UserId,
) = PartyFieldProvenanceDto(
    name = lockField(old.name, new.name, prov.name, lockedAt, lockedBy),
    vat = lockField(old.vat, new.vat, prov.vat, lockedAt, lockedBy),
    email = lockField(old.email, new.email, prov.email, lockedAt, lockedBy),
    iban = lockField(old.iban, new.iban, prov.iban, lockedAt, lockedBy),
    streetLine1 = lockField(old.streetLine1, new.streetLine1, prov.streetLine1, lockedAt, lockedBy),
    streetLine2 = lockField(old.streetLine2, new.streetLine2, prov.streetLine2, lockedAt, lockedBy),
    postalCode = lockField(old.postalCode, new.postalCode, prov.postalCode, lockedAt, lockedBy),
    city = lockField(old.city, new.city, prov.city, lockedAt, lockedBy),
    country = lockField(old.country, new.country, prov.country, lockedAt, lockedBy),
)

private fun lockInvoice(
    prov: InvoiceFieldProvenance,
    old: InvoiceDraftData,
    new: InvoiceDraftData,
    at: LocalDateTime,
    by: UserId,
) = prov.copy(
    direction = lockField(old.direction, new.direction, prov.direction, at, by),
    invoiceNumber = lockField(old.invoiceNumber, new.invoiceNumber, prov.invoiceNumber, at, by),
    issueDate = lockField(old.issueDate, new.issueDate, prov.issueDate, at, by),
    dueDate = lockField(old.dueDate, new.dueDate, prov.dueDate, at, by),
    currency = lockField(old.currency, new.currency, prov.currency, at, by),
    subtotalAmount = lockField(old.subtotalAmount, new.subtotalAmount, prov.subtotalAmount, at, by),
    vatAmount = lockField(old.vatAmount, new.vatAmount, prov.vatAmount, at, by),
    totalAmount = lockField(old.totalAmount, new.totalAmount, prov.totalAmount, at, by),
    lineItems = lockField(old.lineItems, new.lineItems, prov.lineItems, at, by),
    vatBreakdown = lockField(old.vatBreakdown, new.vatBreakdown, prov.vatBreakdown, at, by),
    iban = lockField(old.iban, new.iban, prov.iban, at, by),
    payment = lockField(old.payment, new.payment, prov.payment, at, by),
    notes = lockField(old.notes, new.notes, prov.notes, at, by),
    seller = lockParty(prov.seller, old.seller, new.seller, at, by),
    buyer = lockParty(prov.buyer, old.buyer, new.buyer, at, by),
)

private fun lockCreditNote(
    prov: CreditNoteFieldProvenance,
    old: CreditNoteDraftData,
    new: CreditNoteDraftData,
    at: LocalDateTime,
    by: UserId,
) = prov.copy(
    direction = lockField(old.direction, new.direction, prov.direction, at, by),
    creditNoteNumber = lockField(
        old.creditNoteNumber,
        new.creditNoteNumber,
        prov.creditNoteNumber,
        at,
        by
    ),
    issueDate = lockField(old.issueDate, new.issueDate, prov.issueDate, at, by),
    currency = lockField(old.currency, new.currency, prov.currency, at, by),
    subtotalAmount = lockField(old.subtotalAmount, new.subtotalAmount, prov.subtotalAmount, at, by),
    vatAmount = lockField(old.vatAmount, new.vatAmount, prov.vatAmount, at, by),
    totalAmount = lockField(old.totalAmount, new.totalAmount, prov.totalAmount, at, by),
    lineItems = lockField(old.lineItems, new.lineItems, prov.lineItems, at, by),
    vatBreakdown = lockField(old.vatBreakdown, new.vatBreakdown, prov.vatBreakdown, at, by),
    originalInvoiceNumber = lockField(
        old.originalInvoiceNumber,
        new.originalInvoiceNumber,
        prov.originalInvoiceNumber,
        at,
        by
    ),
    reason = lockField(old.reason, new.reason, prov.reason, at, by),
    notes = lockField(old.notes, new.notes, prov.notes, at, by),
    seller = lockParty(prov.seller, old.seller, new.seller, at, by),
    buyer = lockParty(prov.buyer, old.buyer, new.buyer, at, by),
)

private fun lockReceipt(
    prov: ReceiptFieldProvenance,
    old: ReceiptDraftData,
    new: ReceiptDraftData,
    at: LocalDateTime,
    by: UserId,
) = prov.copy(
    direction = lockField(old.direction, new.direction, prov.direction, at, by),
    merchantName = lockField(old.merchantName, new.merchantName, prov.merchantName, at, by),
    merchantVat = lockField(old.merchantVat, new.merchantVat, prov.merchantVat, at, by),
    date = lockField(old.date, new.date, prov.date, at, by),
    currency = lockField(old.currency, new.currency, prov.currency, at, by),
    totalAmount = lockField(old.totalAmount, new.totalAmount, prov.totalAmount, at, by),
    vatAmount = lockField(old.vatAmount, new.vatAmount, prov.vatAmount, at, by),
    lineItems = lockField(old.lineItems, new.lineItems, prov.lineItems, at, by),
    vatBreakdown = lockField(old.vatBreakdown, new.vatBreakdown, prov.vatBreakdown, at, by),
    receiptNumber = lockField(old.receiptNumber, new.receiptNumber, prov.receiptNumber, at, by),
    paymentMethod = lockField(old.paymentMethod, new.paymentMethod, prov.paymentMethod, at, by),
    notes = lockField(old.notes, new.notes, prov.notes, at, by),
)

private fun lockBankStatement(
    prov: BankStatementFieldProvenance,
    old: BankStatementDraftData,
    new: BankStatementDraftData,
    at: LocalDateTime,
    by: UserId,
) = prov.copy(
    direction = lockField(old.direction, new.direction, prov.direction, at, by),
    transactions = lockField(old.transactions, new.transactions, prov.transactions, at, by),
    accountIban = lockField(old.accountIban, new.accountIban, prov.accountIban, at, by),
    openingBalance = lockField(old.openingBalance, new.openingBalance, prov.openingBalance, at, by),
    closingBalance = lockField(old.closingBalance, new.closingBalance, prov.closingBalance, at, by),
    periodStart = lockField(old.periodStart, new.periodStart, prov.periodStart, at, by),
    periodEnd = lockField(old.periodEnd, new.periodEnd, prov.periodEnd, at, by),
    notes = lockField(old.notes, new.notes, prov.notes, at, by),
)
