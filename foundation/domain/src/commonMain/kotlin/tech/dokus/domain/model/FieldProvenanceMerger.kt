package tech.dokus.domain.model

/**
 * Result of merging two DocumentDraftData instances using field provenance.
 */
data class ProvenanceMergeResult(
    val mergedData: DocumentDraftData,
    val mergedProvenance: Map<String, FieldProvenance>,
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
    existingProvenance: Map<String, FieldProvenance>,
    incomingProvenance: Map<String, FieldProvenance>,
): ProvenanceMergeResult {
    // Type change = reclassification — incoming wins entirely
    if (existing::class != incoming::class) {
        return ProvenanceMergeResult(incoming, incomingProvenance)
    }

    return when {
        existing is InvoiceDraftData && incoming is InvoiceDraftData ->
            mergeInvoice(existing, incoming, existingProvenance, incomingProvenance)
        existing is CreditNoteDraftData && incoming is CreditNoteDraftData ->
            mergeCreditNote(existing, incoming, existingProvenance, incomingProvenance)
        existing is ReceiptDraftData && incoming is ReceiptDraftData ->
            mergeReceipt(existing, incoming, existingProvenance, incomingProvenance)
        existing is BankStatementDraftData && incoming is BankStatementDraftData ->
            mergeBankStatement(existing, incoming, existingProvenance, incomingProvenance)
        else -> ProvenanceMergeResult(incoming, incomingProvenance)
    }
}

/**
 * Returns field paths that differ between [existing] and [updated].
 * Used to detect which fields the user actually changed during editing.
 * If types differ (reclassification), all populated fields of [updated] are returned.
 */
fun diffFieldPaths(
    existing: DocumentDraftData,
    updated: DocumentDraftData,
): Set<String> {
    if (existing::class != updated::class) {
        return updated.let { (it as? InvoiceDraftData)?.let(::invoicePopulated) }
            ?: updated.let { (it as? CreditNoteDraftData)?.let(::creditNotePopulated) }
            ?: updated.let { (it as? ReceiptDraftData)?.let(::receiptPopulated) }
            ?: updated.let { (it as? BankStatementDraftData)?.let(::bankStatementPopulated) }
            ?: emptySet()
    }
    return when {
        existing is InvoiceDraftData && updated is InvoiceDraftData ->
            diffInvoice(existing, updated)
        existing is CreditNoteDraftData && updated is CreditNoteDraftData ->
            diffCreditNote(existing, updated)
        existing is ReceiptDraftData && updated is ReceiptDraftData ->
            diffReceipt(existing, updated)
        existing is BankStatementDraftData && updated is BankStatementDraftData ->
            diffBankStatement(existing, updated)
        else -> emptySet()
    }
}

// ---------------------------------------------------------------------------
// Per-variant diff
// ---------------------------------------------------------------------------

private fun diffInvoice(old: InvoiceDraftData, new: InvoiceDraftData): Set<String> = buildSet {
    if (old.direction != new.direction) add("direction")
    if (old.invoiceNumber != new.invoiceNumber) add("invoiceNumber")
    if (old.issueDate != new.issueDate) add("issueDate")
    if (old.dueDate != new.dueDate) add("dueDate")
    if (old.currency != new.currency) add("currency")
    if (old.subtotalAmount != new.subtotalAmount) add("subtotalAmount")
    if (old.vatAmount != new.vatAmount) add("vatAmount")
    if (old.totalAmount != new.totalAmount) add("totalAmount")
    if (old.lineItems != new.lineItems) add("lineItems")
    if (old.vatBreakdown != new.vatBreakdown) add("vatBreakdown")
    if (old.iban != new.iban) add("iban")
    if (old.payment != new.payment) add("payment")
    if (old.notes != new.notes) add("notes")
    addPartyDiff("seller", old.seller, new.seller)
    addPartyDiff("buyer", old.buyer, new.buyer)
}

private fun diffCreditNote(old: CreditNoteDraftData, new: CreditNoteDraftData): Set<String> = buildSet {
    if (old.direction != new.direction) add("direction")
    if (old.creditNoteNumber != new.creditNoteNumber) add("creditNoteNumber")
    if (old.issueDate != new.issueDate) add("issueDate")
    if (old.currency != new.currency) add("currency")
    if (old.subtotalAmount != new.subtotalAmount) add("subtotalAmount")
    if (old.vatAmount != new.vatAmount) add("vatAmount")
    if (old.totalAmount != new.totalAmount) add("totalAmount")
    if (old.lineItems != new.lineItems) add("lineItems")
    if (old.vatBreakdown != new.vatBreakdown) add("vatBreakdown")
    if (old.counterpartyName != new.counterpartyName) add("counterpartyName")
    if (old.counterpartyVat != new.counterpartyVat) add("counterpartyVat")
    if (old.originalInvoiceNumber != new.originalInvoiceNumber) add("originalInvoiceNumber")
    if (old.reason != new.reason) add("reason")
    if (old.notes != new.notes) add("notes")
    addPartyDiff("seller", old.seller, new.seller)
    addPartyDiff("buyer", old.buyer, new.buyer)
}

private fun diffReceipt(old: ReceiptDraftData, new: ReceiptDraftData): Set<String> = buildSet {
    if (old.direction != new.direction) add("direction")
    if (old.merchantName != new.merchantName) add("merchantName")
    if (old.merchantVat != new.merchantVat) add("merchantVat")
    if (old.date != new.date) add("date")
    if (old.currency != new.currency) add("currency")
    if (old.totalAmount != new.totalAmount) add("totalAmount")
    if (old.vatAmount != new.vatAmount) add("vatAmount")
    if (old.lineItems != new.lineItems) add("lineItems")
    if (old.vatBreakdown != new.vatBreakdown) add("vatBreakdown")
    if (old.receiptNumber != new.receiptNumber) add("receiptNumber")
    if (old.paymentMethod != new.paymentMethod) add("paymentMethod")
    if (old.notes != new.notes) add("notes")
}

private fun diffBankStatement(old: BankStatementDraftData, new: BankStatementDraftData): Set<String> = buildSet {
    if (old.direction != new.direction) add("direction")
    if (old.transactions != new.transactions) add("transactions")
    if (old.accountIban != new.accountIban) add("accountIban")
    if (old.openingBalance != new.openingBalance) add("openingBalance")
    if (old.closingBalance != new.closingBalance) add("closingBalance")
    if (old.periodStart != new.periodStart) add("periodStart")
    if (old.periodEnd != new.periodEnd) add("periodEnd")
    if (old.notes != new.notes) add("notes")
}

private fun MutableSet<String>.addPartyDiff(prefix: String, old: PartyDraft, new: PartyDraft) {
    if (old.name != new.name) add("$prefix.name")
    if (old.vat != new.vat) add("$prefix.vat")
    if (old.email != new.email) add("$prefix.email")
    if (old.iban != new.iban) add("$prefix.iban")
    if (old.streetLine1 != new.streetLine1) add("$prefix.streetLine1")
    if (old.streetLine2 != new.streetLine2) add("$prefix.streetLine2")
    if (old.postalCode != new.postalCode) add("$prefix.postalCode")
    if (old.city != new.city) add("$prefix.city")
    if (old.country != new.country) add("$prefix.country")
}

// Populated-field helpers for reclassification (all fields in updated type)
private fun invoicePopulated(d: InvoiceDraftData): Set<String> = buildSet {
    add("direction"); add("currency")
    if (d.invoiceNumber != null) add("invoiceNumber")
    if (d.issueDate != null) add("issueDate")
    if (d.dueDate != null) add("dueDate")
    if (d.subtotalAmount != null) add("subtotalAmount")
    if (d.vatAmount != null) add("vatAmount")
    if (d.totalAmount != null) add("totalAmount")
    if (d.lineItems.isNotEmpty()) add("lineItems")
    if (d.vatBreakdown.isNotEmpty()) add("vatBreakdown")
    if (d.iban != null) add("iban")
    if (d.payment != null) add("payment")
    if (d.notes != null) add("notes")
    addPopulatedParty("seller", d.seller)
    addPopulatedParty("buyer", d.buyer)
}

private fun creditNotePopulated(d: CreditNoteDraftData): Set<String> = buildSet {
    add("direction"); add("currency")
    if (d.creditNoteNumber != null) add("creditNoteNumber")
    if (d.issueDate != null) add("issueDate")
    if (d.subtotalAmount != null) add("subtotalAmount")
    if (d.vatAmount != null) add("vatAmount")
    if (d.totalAmount != null) add("totalAmount")
    if (d.lineItems.isNotEmpty()) add("lineItems")
    if (d.vatBreakdown.isNotEmpty()) add("vatBreakdown")
    if (d.counterpartyName != null) add("counterpartyName")
    if (d.counterpartyVat != null) add("counterpartyVat")
    if (d.originalInvoiceNumber != null) add("originalInvoiceNumber")
    if (d.reason != null) add("reason")
    if (d.notes != null) add("notes")
    addPopulatedParty("seller", d.seller)
    addPopulatedParty("buyer", d.buyer)
}

private fun receiptPopulated(d: ReceiptDraftData): Set<String> = buildSet {
    add("direction"); add("currency")
    if (d.merchantName != null) add("merchantName")
    if (d.merchantVat != null) add("merchantVat")
    if (d.date != null) add("date")
    if (d.totalAmount != null) add("totalAmount")
    if (d.vatAmount != null) add("vatAmount")
    if (d.lineItems.isNotEmpty()) add("lineItems")
    if (d.vatBreakdown.isNotEmpty()) add("vatBreakdown")
    if (d.receiptNumber != null) add("receiptNumber")
    if (d.paymentMethod != null) add("paymentMethod")
    if (d.notes != null) add("notes")
}

private fun bankStatementPopulated(d: BankStatementDraftData): Set<String> = buildSet {
    add("direction")
    if (d.transactions.isNotEmpty()) add("transactions")
    if (d.accountIban != null) add("accountIban")
    if (d.openingBalance != null) add("openingBalance")
    if (d.closingBalance != null) add("closingBalance")
    if (d.periodStart != null) add("periodStart")
    if (d.periodEnd != null) add("periodEnd")
    if (d.notes != null) add("notes")
}

private fun MutableSet<String>.addPopulatedParty(prefix: String, party: PartyDraft) {
    if (party.name != null) add("$prefix.name")
    if (party.vat != null) add("$prefix.vat")
    if (party.email != null) add("$prefix.email")
    if (party.iban != null) add("$prefix.iban")
    if (party.streetLine1 != null) add("$prefix.streetLine1")
    if (party.streetLine2 != null) add("$prefix.streetLine2")
    if (party.postalCode != null) add("$prefix.postalCode")
    if (party.city != null) add("$prefix.city")
    if (party.country != null) add("$prefix.country")
}

// ---------------------------------------------------------------------------
// Per-field pick helper
// ---------------------------------------------------------------------------

private inline fun <T> pickField(
    field: String,
    existingValue: T,
    incomingValue: T,
    existingProvenance: Map<String, FieldProvenance>,
    incomingProvenance: Map<String, FieldProvenance>,
    mergedProvenance: MutableMap<String, FieldProvenance>,
): T {
    val ep = existingProvenance[field]
    val ip = incomingProvenance[field]

    return when {
        ep == null -> {
            if (ip != null) mergedProvenance[field] = ip
            incomingValue
        }
        ep.userLocked -> {
            mergedProvenance[field] = ep
            existingValue
        }
        ip == null -> {
            mergedProvenance[field] = ep
            existingValue
        }
        ep.shouldBeOverwrittenBy(ip) -> {
            mergedProvenance[field] = ip
            incomingValue
        }
        else -> {
            mergedProvenance[field] = ep
            existingValue
        }
    }
}

// ---------------------------------------------------------------------------
// Party merge helper
// ---------------------------------------------------------------------------

private fun pickParty(
    prefix: String,
    existing: PartyDraft,
    incoming: PartyDraft,
    existingProv: Map<String, FieldProvenance>,
    incomingProv: Map<String, FieldProvenance>,
    merged: MutableMap<String, FieldProvenance>,
): PartyDraft = PartyDraft(
    name = pickField("$prefix.name", existing.name, incoming.name, existingProv, incomingProv, merged),
    vat = pickField("$prefix.vat", existing.vat, incoming.vat, existingProv, incomingProv, merged),
    email = pickField("$prefix.email", existing.email, incoming.email, existingProv, incomingProv, merged),
    iban = pickField("$prefix.iban", existing.iban, incoming.iban, existingProv, incomingProv, merged),
    streetLine1 = pickField("$prefix.streetLine1", existing.streetLine1, incoming.streetLine1, existingProv, incomingProv, merged),
    streetLine2 = pickField("$prefix.streetLine2", existing.streetLine2, incoming.streetLine2, existingProv, incomingProv, merged),
    postalCode = pickField("$prefix.postalCode", existing.postalCode, incoming.postalCode, existingProv, incomingProv, merged),
    city = pickField("$prefix.city", existing.city, incoming.city, existingProv, incomingProv, merged),
    country = pickField("$prefix.country", existing.country, incoming.country, existingProv, incomingProv, merged),
)

// ---------------------------------------------------------------------------
// Per-variant merge
// ---------------------------------------------------------------------------

private fun mergeInvoice(
    existing: InvoiceDraftData,
    incoming: InvoiceDraftData,
    existingProv: Map<String, FieldProvenance>,
    incomingProv: Map<String, FieldProvenance>,
): ProvenanceMergeResult {
    val merged = mutableMapOf<String, FieldProvenance>()

    fun <T> pick(field: String, e: T, i: T): T =
        pickField(field, e, i, existingProv, incomingProv, merged)

    val result = InvoiceDraftData(
        direction = pick("direction", existing.direction, incoming.direction),
        invoiceNumber = pick("invoiceNumber", existing.invoiceNumber, incoming.invoiceNumber),
        issueDate = pick("issueDate", existing.issueDate, incoming.issueDate),
        dueDate = pick("dueDate", existing.dueDate, incoming.dueDate),
        currency = pick("currency", existing.currency, incoming.currency),
        subtotalAmount = pick("subtotalAmount", existing.subtotalAmount, incoming.subtotalAmount),
        vatAmount = pick("vatAmount", existing.vatAmount, incoming.vatAmount),
        totalAmount = pick("totalAmount", existing.totalAmount, incoming.totalAmount),
        lineItems = pick("lineItems", existing.lineItems, incoming.lineItems),
        vatBreakdown = pick("vatBreakdown", existing.vatBreakdown, incoming.vatBreakdown),
        iban = pick("iban", existing.iban, incoming.iban),
        payment = pick("payment", existing.payment, incoming.payment),
        notes = pick("notes", existing.notes, incoming.notes),
        seller = pickParty("seller", existing.seller, incoming.seller, existingProv, incomingProv, merged),
        buyer = pickParty("buyer", existing.buyer, incoming.buyer, existingProv, incomingProv, merged),
    )
    return ProvenanceMergeResult(result, merged)
}

private fun mergeCreditNote(
    existing: CreditNoteDraftData,
    incoming: CreditNoteDraftData,
    existingProv: Map<String, FieldProvenance>,
    incomingProv: Map<String, FieldProvenance>,
): ProvenanceMergeResult {
    val merged = mutableMapOf<String, FieldProvenance>()

    fun <T> pick(field: String, e: T, i: T): T =
        pickField(field, e, i, existingProv, incomingProv, merged)

    val result = CreditNoteDraftData(
        direction = pick("direction", existing.direction, incoming.direction),
        creditNoteNumber = pick("creditNoteNumber", existing.creditNoteNumber, incoming.creditNoteNumber),
        issueDate = pick("issueDate", existing.issueDate, incoming.issueDate),
        currency = pick("currency", existing.currency, incoming.currency),
        subtotalAmount = pick("subtotalAmount", existing.subtotalAmount, incoming.subtotalAmount),
        vatAmount = pick("vatAmount", existing.vatAmount, incoming.vatAmount),
        totalAmount = pick("totalAmount", existing.totalAmount, incoming.totalAmount),
        lineItems = pick("lineItems", existing.lineItems, incoming.lineItems),
        vatBreakdown = pick("vatBreakdown", existing.vatBreakdown, incoming.vatBreakdown),
        counterpartyName = pick("counterpartyName", existing.counterpartyName, incoming.counterpartyName),
        counterpartyVat = pick("counterpartyVat", existing.counterpartyVat, incoming.counterpartyVat),
        originalInvoiceNumber = pick("originalInvoiceNumber", existing.originalInvoiceNumber, incoming.originalInvoiceNumber),
        reason = pick("reason", existing.reason, incoming.reason),
        notes = pick("notes", existing.notes, incoming.notes),
        seller = pickParty("seller", existing.seller, incoming.seller, existingProv, incomingProv, merged),
        buyer = pickParty("buyer", existing.buyer, incoming.buyer, existingProv, incomingProv, merged),
    )
    return ProvenanceMergeResult(result, merged)
}

private fun mergeReceipt(
    existing: ReceiptDraftData,
    incoming: ReceiptDraftData,
    existingProv: Map<String, FieldProvenance>,
    incomingProv: Map<String, FieldProvenance>,
): ProvenanceMergeResult {
    val merged = mutableMapOf<String, FieldProvenance>()

    fun <T> pick(field: String, e: T, i: T): T =
        pickField(field, e, i, existingProv, incomingProv, merged)

    val result = ReceiptDraftData(
        direction = pick("direction", existing.direction, incoming.direction),
        merchantName = pick("merchantName", existing.merchantName, incoming.merchantName),
        merchantVat = pick("merchantVat", existing.merchantVat, incoming.merchantVat),
        date = pick("date", existing.date, incoming.date),
        currency = pick("currency", existing.currency, incoming.currency),
        totalAmount = pick("totalAmount", existing.totalAmount, incoming.totalAmount),
        vatAmount = pick("vatAmount", existing.vatAmount, incoming.vatAmount),
        lineItems = pick("lineItems", existing.lineItems, incoming.lineItems),
        vatBreakdown = pick("vatBreakdown", existing.vatBreakdown, incoming.vatBreakdown),
        receiptNumber = pick("receiptNumber", existing.receiptNumber, incoming.receiptNumber),
        paymentMethod = pick("paymentMethod", existing.paymentMethod, incoming.paymentMethod),
        notes = pick("notes", existing.notes, incoming.notes),
    )
    return ProvenanceMergeResult(result, merged)
}

private fun mergeBankStatement(
    existing: BankStatementDraftData,
    incoming: BankStatementDraftData,
    existingProv: Map<String, FieldProvenance>,
    incomingProv: Map<String, FieldProvenance>,
): ProvenanceMergeResult {
    val merged = mutableMapOf<String, FieldProvenance>()

    fun <T> pick(field: String, e: T, i: T): T =
        pickField(field, e, i, existingProv, incomingProv, merged)

    val result = BankStatementDraftData(
        direction = pick("direction", existing.direction, incoming.direction),
        transactions = pick("transactions", existing.transactions, incoming.transactions),
        accountIban = pick("accountIban", existing.accountIban, incoming.accountIban),
        openingBalance = pick("openingBalance", existing.openingBalance, incoming.openingBalance),
        closingBalance = pick("closingBalance", existing.closingBalance, incoming.closingBalance),
        periodStart = pick("periodStart", existing.periodStart, incoming.periodStart),
        periodEnd = pick("periodEnd", existing.periodEnd, incoming.periodEnd),
        notes = pick("notes", existing.notes, incoming.notes),
    )
    return ProvenanceMergeResult(result, merged)
}
