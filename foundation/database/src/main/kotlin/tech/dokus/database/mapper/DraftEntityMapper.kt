@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.BankStatementDraftEntity
import tech.dokus.database.entity.CreditNoteDraftEntity
import tech.dokus.database.entity.CreditNoteDraftItemEntity
import tech.dokus.database.entity.InvoiceDraftEntity
import tech.dokus.database.entity.InvoiceDraftItemEntity
import tech.dokus.database.entity.ReceiptDraftEntity
import tech.dokus.database.tables.drafts.BankStatementDraftsTable
import tech.dokus.database.tables.drafts.CreditNoteDraftsTable
import tech.dokus.database.tables.drafts.CreditNoteDraftItemsTable
import tech.dokus.database.tables.drafts.InvoiceDraftsTable
import tech.dokus.database.tables.drafts.InvoiceDraftItemsTable
import tech.dokus.database.tables.drafts.ReceiptDraftsTable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import kotlin.uuid.toKotlinUuid

// =============================================================================
// Invoice draft
// =============================================================================

fun InvoiceDraftEntity.Companion.from(
    row: ResultRow,
    items: List<InvoiceDraftItemEntity>,
): InvoiceDraftEntity = InvoiceDraftEntity(
    id = row[InvoiceDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[InvoiceDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[InvoiceDraftsTable.documentId].toKotlinUuid()),
    contactId = row[InvoiceDraftsTable.contactId]?.toKotlinUuid(),
    invoiceNumber = row[InvoiceDraftsTable.invoiceNumber],
    issueDate = row[InvoiceDraftsTable.issueDate],
    dueDate = row[InvoiceDraftsTable.dueDate],
    direction = row[InvoiceDraftsTable.direction],
    currency = row[InvoiceDraftsTable.currency],
    subtotalAmount = row[InvoiceDraftsTable.subtotalAmount]?.let { Money.fromDbDecimal(it) },
    vatAmount = row[InvoiceDraftsTable.vatAmount]?.let { Money.fromDbDecimal(it) },
    totalAmount = row[InvoiceDraftsTable.totalAmount]?.let { Money.fromDbDecimal(it) },
    notes = row[InvoiceDraftsTable.notes],
    senderIban = row[InvoiceDraftsTable.senderIban]?.let { Iban(it) },
    structuredCommunication = row[InvoiceDraftsTable.structuredCommunication]?.let { StructuredCommunication(it) },
    items = items,
    createdAt = row[InvoiceDraftsTable.createdAt],
    updatedAt = row[InvoiceDraftsTable.updatedAt],
)

fun InvoiceDraftItemEntity.Companion.from(row: ResultRow): InvoiceDraftItemEntity = InvoiceDraftItemEntity(
    id = row[InvoiceDraftItemsTable.id].value.toKotlinUuid(),
    draftId = row[InvoiceDraftItemsTable.draftId].toKotlinUuid(),
    description = row[InvoiceDraftItemsTable.description],
    quantity = row[InvoiceDraftItemsTable.quantity]?.toDouble(),
    unitPrice = row[InvoiceDraftItemsTable.unitPrice]?.let { Money.fromDbDecimal(it) },
    vatRate = row[InvoiceDraftItemsTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
    lineTotal = row[InvoiceDraftItemsTable.lineTotal]?.let { Money.fromDbDecimal(it) },
    vatAmount = row[InvoiceDraftItemsTable.vatAmount]?.let { Money.fromDbDecimal(it) },
    sortOrder = row[InvoiceDraftItemsTable.sortOrder],
)

// =============================================================================
// CreditNote draft
// =============================================================================

fun CreditNoteDraftEntity.Companion.from(
    row: ResultRow,
    items: List<CreditNoteDraftItemEntity>,
): CreditNoteDraftEntity = CreditNoteDraftEntity(
    id = row[CreditNoteDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[CreditNoteDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[CreditNoteDraftsTable.documentId].toKotlinUuid()),
    contactId = row[CreditNoteDraftsTable.contactId]?.toKotlinUuid(),
    creditNoteNumber = row[CreditNoteDraftsTable.creditNoteNumber],
    issueDate = row[CreditNoteDraftsTable.issueDate],
    direction = row[CreditNoteDraftsTable.direction],
    currency = row[CreditNoteDraftsTable.currency],
    subtotalAmount = row[CreditNoteDraftsTable.subtotalAmount]?.let { Money.fromDbDecimal(it) },
    vatAmount = row[CreditNoteDraftsTable.vatAmount]?.let { Money.fromDbDecimal(it) },
    totalAmount = row[CreditNoteDraftsTable.totalAmount]?.let { Money.fromDbDecimal(it) },
    originalInvoiceNumber = row[CreditNoteDraftsTable.originalInvoiceNumber],
    reason = row[CreditNoteDraftsTable.reason],
    notes = row[CreditNoteDraftsTable.notes],
    items = items,
    createdAt = row[CreditNoteDraftsTable.createdAt],
    updatedAt = row[CreditNoteDraftsTable.updatedAt],
)

fun CreditNoteDraftItemEntity.Companion.from(row: ResultRow): CreditNoteDraftItemEntity = CreditNoteDraftItemEntity(
    id = row[CreditNoteDraftItemsTable.id].value.toKotlinUuid(),
    draftId = row[CreditNoteDraftItemsTable.draftId].toKotlinUuid(),
    description = row[CreditNoteDraftItemsTable.description],
    quantity = row[CreditNoteDraftItemsTable.quantity]?.toDouble(),
    unitPrice = row[CreditNoteDraftItemsTable.unitPrice]?.let { Money.fromDbDecimal(it) },
    vatRate = row[CreditNoteDraftItemsTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
    lineTotal = row[CreditNoteDraftItemsTable.lineTotal]?.let { Money.fromDbDecimal(it) },
    vatAmount = row[CreditNoteDraftItemsTable.vatAmount]?.let { Money.fromDbDecimal(it) },
    sortOrder = row[CreditNoteDraftItemsTable.sortOrder],
)

// =============================================================================
// Receipt draft
// =============================================================================

fun ReceiptDraftEntity.Companion.from(row: ResultRow): ReceiptDraftEntity = ReceiptDraftEntity(
    id = row[ReceiptDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[ReceiptDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[ReceiptDraftsTable.documentId].toKotlinUuid()),
    merchantName = row[ReceiptDraftsTable.merchantName],
    merchantVat = row[ReceiptDraftsTable.merchantVat]?.let { VatNumber(it) },
    date = row[ReceiptDraftsTable.date],
    direction = row[ReceiptDraftsTable.direction],
    currency = row[ReceiptDraftsTable.currency],
    totalAmount = row[ReceiptDraftsTable.totalAmount]?.let { Money.fromDbDecimal(it) },
    vatAmount = row[ReceiptDraftsTable.vatAmount]?.let { Money.fromDbDecimal(it) },
    receiptNumber = row[ReceiptDraftsTable.receiptNumber],
    paymentMethod = row[ReceiptDraftsTable.paymentMethod],
    notes = row[ReceiptDraftsTable.notes],
    createdAt = row[ReceiptDraftsTable.createdAt],
    updatedAt = row[ReceiptDraftsTable.updatedAt],
)

// =============================================================================
// BankStatement draft
// =============================================================================

fun BankStatementDraftEntity.Companion.from(row: ResultRow): BankStatementDraftEntity = BankStatementDraftEntity(
    id = row[BankStatementDraftsTable.id].value.toKotlinUuid(),
    tenantId = TenantId(row[BankStatementDraftsTable.tenantId].toKotlinUuid()),
    documentId = DocumentId(row[BankStatementDraftsTable.documentId].toKotlinUuid()),
    direction = row[BankStatementDraftsTable.direction],
    accountIban = row[BankStatementDraftsTable.accountIban]?.let { Iban(it) },
    openingBalance = row[BankStatementDraftsTable.openingBalance]?.let { Money.fromDbDecimal(it) },
    closingBalance = row[BankStatementDraftsTable.closingBalance]?.let { Money.fromDbDecimal(it) },
    periodStart = row[BankStatementDraftsTable.periodStart],
    periodEnd = row[BankStatementDraftsTable.periodEnd],
    notes = row[BankStatementDraftsTable.notes],
    createdAt = row[BankStatementDraftsTable.createdAt],
    updatedAt = row[BankStatementDraftsTable.updatedAt],
)

// =============================================================================
// Classified entity mappers are in ClassifiedEntityMappers.kt
// =============================================================================
