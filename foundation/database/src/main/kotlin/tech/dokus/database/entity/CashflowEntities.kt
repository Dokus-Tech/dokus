package tech.dokus.database.entity

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowContactRefDto

/**
 * Database entity for cashflow entries.
 * Maps directly to the cashflow_entries table.
 */
data class CashflowEntryEntity(
    val id: CashflowEntryId,
    val tenantId: TenantId,
    val sourceType: CashflowSourceType,
    val sourceId: String,
    val documentId: DocumentId?,
    val direction: CashflowDirection,
    val eventDate: LocalDate,
    val amountGross: Money,
    val amountVat: Money,
    val remainingAmount: Money,
    val currency: Currency,
    val status: CashflowEntryStatus,
    val paidAt: LocalDateTime?,
    val contact: CashflowContactRefDto? = null,
    val description: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}
